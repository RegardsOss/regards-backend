/*
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
 *
 * This file is part of REGARDS.
 *
 * REGARDS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * REGARDS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with REGARDS. If not, see <http://www.gnu.org/licenses/>.
 */
package fr.cnes.regards.modules.order.service.job;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import fr.cnes.regards.framework.modules.jobs.domain.AbstractJob;
import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterInvalidException;
import fr.cnes.regards.framework.modules.jobs.service.IJobInfoService;
import fr.cnes.regards.modules.order.dao.IOrderDataFileRepository;
import fr.cnes.regards.modules.order.domain.FileState;
import fr.cnes.regards.modules.order.domain.OrderDataFile;
import fr.cnes.regards.modules.order.service.IOrderDataFileService;
import fr.cnes.regards.modules.order.service.IOrderJobService;
import fr.cnes.regards.modules.order.service.job.parameters.*;
import fr.cnes.regards.modules.order.service.processing.IOrderProcessingService;
import fr.cnes.regards.modules.storage.client.IStorageClient;
import io.vavr.control.Option;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.CancellationException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Job to ensure with storage microservice that order files are availables to download.
 * <p>
 * When using processing, this job launches the {@link ProcessExecutionJob} referenced in
 * the {@link #processJobInfoId} field.
 *
 * @author Sébastien Binda
 */
public class StorageFilesJob extends AbstractJob<Void> {

    @Autowired
    protected IOrderDataFileService dataFileService;

    @Autowired
    protected IJobInfoService jobInfoService;

    @Autowired
    protected IStorageFileListenerService subscriber;

    @Autowired
    protected IStorageClient storageClient;

    @Autowired
    protected IOrderJobService orderJobService;

    @Autowired
    private IOrderProcessingService processingService;

    protected Integer subOrderAvailabilityDurationHours;

    protected Semaphore semaphore;

    protected Option<UUID> processJobInfoId = Option.none();

    /**
     * Map { checksum -> ( dataFiles) } of data files.
     * Same file can be part of 2 or more data objects so with same checksum but different OrderDataFile (thanks to
     * uri)
     * The case where checksum is the same for 2 different files is abandoned (never mind)
     */
    protected final Multimap<String, OrderDataFile> dataFilesMultimap = HashMultimap.create();

    /**
     * Set of file checksums already handled by a DataStorageEvent.
     * Used in order to avoid listening on two same available events from storage.
     */
    protected final Set<String> availableHandledFiles = Sets.newHashSet();

    protected final Set<String> unavailableHandledFiles = Sets.newHashSet();

    /**
     * The user.
     */
    private String user;

    private Long orderId;

    @Autowired
    private IOrderDataFileRepository orderDataFileRepository;

    @Override
    public void setParameters(Map<String, JobParameter> parameters) throws JobParameterInvalidException {
        if ((parameters.size() < 5) || (parameters.size() > 6)) {
            throw new JobParameterInvalidException(
                "Five or six parameters are expected : 'files', 'subOrderAvailabilityDurationHours', 'user' "
                + "'userRole' and 'orderId', and optionally 'processJobInfo'.");
        }
        for (JobParameter param : parameters.values()) {
            boolean paramIsIncompatible = Stream.<Function<JobParameter, Boolean>>of(FilesJobParameter::isCompatible,
                                                                                     SubOrderAvailabilityPeriodJobParameter::isCompatible,
                                                                                     UserJobParameter::isCompatible,
                                                                                     UserRoleJobParameter::isCompatible,
                                                                                     ProcessJobInfoJobParameter::isCompatible,
                                                                                     OrderIdJobParameter::isCompatible)
                                                .noneMatch(f -> f.apply(param));
            if (paramIsIncompatible) {
                throw new JobParameterInvalidException(
                    "Please use ProcessJobInfoJobParameter, FilesJobParameter, SubOrderAvailabilityPeriodJobParameter, UserJobParameter and "
                    + "UserRoleJobParameter in place of JobParameter (these "
                    + "classes are here to facilitate your life so please use them.");
            }
            if (FilesJobParameter.isCompatible(param)) {
                Long[] fileIds = param.getValue();
                List<OrderDataFile> files = orderDataFileRepository.findAllById(Arrays.asList(fileIds));
                for (OrderDataFile dataFile : files) {
                    dataFilesMultimap.put(dataFile.getChecksum(), dataFile);
                }
            } else if (SubOrderAvailabilityPeriodJobParameter.isCompatible(param)) {
                subOrderAvailabilityDurationHours = param.getValue();
            } else if (ProcessJobInfoJobParameter.isCompatible(param)) {
                processJobInfoId = Option.some(param.getValue());
            } else if (UserJobParameter.isCompatible(param)) {
                user = param.getValue();
            } else if (OrderIdJobParameter.isCompatible(param)) {
                orderId = param.getValue();
            }
        }
    }

    @Override
    public int getCompletionCount() {
        return dataFilesMultimap.keySet().size();
    }

    @Override
    public void run() {
        // Ensure the order is not paused before starting computation
        if (orderJobService.isOrderPaused(orderId)) {
            logger.info("The job {} will not be run now as the order {} is paused", this.jobInfoId, this.orderId);
            Thread.currentThread().interrupt();
            throw new CancellationException();
        }

        logger.debug("Start of StorageFilesJob {} run", this.jobInfoId);
        this.semaphore = new Semaphore(-dataFilesMultimap.keySet().size() + 1);
        subscriber.subscribe(this);

        try {
            storageClient.makeAvailable(dataFilesMultimap.keySet(),
                                        OffsetDateTime.now().plusHours(subOrderAvailabilityDurationHours));
            dataFilesMultimap.forEach((cs, f) -> logger.debug("Order job is waiting for {} file {} - {} availability.",
                                                              dataFilesMultimap.size(),
                                                              f.getFilename(),
                                                              cs));
            // Wait for remaining files availability from storage
            // Wait maximum 2 hours for storage restitution.
            int nbHoursToWait = 2;
            if (this.semaphore.tryAcquire(nbHoursToWait, TimeUnit.HOURS)) {
                logger.debug("All files ({}) are available.", dataFilesMultimap.keySet().size());
            } else {
                logger.error("All files are not available after waiting for {} hours", nbHoursToWait);
            }
        } catch (RuntimeException e) { // Feign or network or ... exception
            // Put All data files in ERROR and propagate exception to make job fail
            dataFilesMultimap.values().forEach(df -> df.setState(FileState.ERROR));
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.info("Order job {} has been interrupted ! ", this.jobInfoId);
        } finally {
            // All files have been treated by storage, no more event subscriber needed...
            subscriber.unsubscribe(this);
            if (processJobInfoId.isEmpty()) {
                // NO PROCESSING
                dataFilesMultimap.entries()
                                 .stream()
                                 .filter(e -> availableHandledFiles.contains(e.getKey()))
                                 .forEach(e -> e.getValue().setState(FileState.AVAILABLE));
                dataFilesMultimap.entries()
                                 .stream()
                                 .filter(e -> unavailableHandledFiles.contains(e.getKey()))
                                 .forEach(e -> e.getValue().setState(FileState.ERROR));
                dataFileService.save(dataFilesMultimap.values());
            } else {
                // With PROCESSING
                processingService.enqueuedProcessingJob(processJobInfoId.get(), dataFilesMultimap.values(), user);
            }
        }
        logger.debug("End of StorageFilesJob {} run", this.jobInfoId);
    }

    public void notifyFilesAvailable(Collection<String> availableFilesChecksum) {
        notifyFiles(availableFilesChecksum, availableHandledFiles);
    }

    public void notifyFilesUnavailable(Collection<String> unavailableFilesChecksum) {
        notifyFiles(unavailableFilesChecksum, unavailableHandledFiles);
    }

    private void notifyFiles(Collection<String> notifiedFiles, Collection<String> alreadyNotifiedFiles) {
        Set<String> unavailableFilesOrderedByThisJob = new HashSet<>(notifiedFiles);
        unavailableFilesOrderedByThisJob.retainAll(dataFilesMultimap.keySet());
        unavailableFilesOrderedByThisJob.removeAll(alreadyNotifiedFiles);
        for (String unavailable : unavailableFilesOrderedByThisJob) {
            alreadyNotifiedFiles.add(unavailable);
            this.advanceCompletion();
        }
        // Release as much semaphore permits as there is available files
        this.semaphore.release(unavailableFilesOrderedByThisJob.size());
    }
}
