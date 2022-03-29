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
import fr.cnes.regards.modules.storage.client.IStorageClient;
import io.vavr.control.Option;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.Semaphore;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Job  to ensure with storage microservice that order files are availables to download.
 *
 * When using processing, this job launches the {@link ProcessExecutionJob} referenced in
 * the {@link #processJobInfoId} field.
 *
 * @author Sébastien Binda
 *
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
    protected final Set<String> alreadyHandledFiles = Sets.newHashSet();

    /**
     * The user.
     */
    private String user;

    @Autowired
    private IOrderDataFileRepository orderDataFileRepository;

    @Autowired
    private IOrderDataFileService orderDataFileService;

    @Override
    public void setParameters(Map<String, JobParameter> parameters)
            throws JobParameterInvalidException {
        if ((parameters.size() < 4) || (parameters.size() > 5)) {
            throw new JobParameterInvalidException(
                    "Four or five parameters are expected : 'files', 'subOrderAvailabilityDurationHours', 'user' and 'userRole', and optionally 'processJobInfo'."
            );
        }
        for (JobParameter param : parameters.values()) {
            boolean paramIsIncompatible =
                Stream.<Function<JobParameter, Boolean>>of(
                    FilesJobParameter::isCompatible,
                    SubOrderAvailabilityPeriodJobParameter::isCompatible,
                    UserJobParameter::isCompatible,
                    UserRoleJobParameter::isCompatible,
                    ProcessJobInfoJobParameter::isCompatible
                ).noneMatch(f -> f.apply(param));
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
            }
        }
    }

    @Override
    public int getCompletionCount() {
        return dataFilesMultimap.keySet().size();
    }

    @Override
    public void run() {
        this.semaphore = new Semaphore(-dataFilesMultimap.keySet().size() + 1);
        subscriber.subscribe(this);

        try {
            storageClient.makeAvailable(dataFilesMultimap.keySet(), OffsetDateTime.now().plusHours(subOrderAvailabilityDurationHours));
            dataFilesMultimap.forEach((cs, f) -> logger.debug("Order job is waiting for {} file {} - {} availability.", dataFilesMultimap.size(),
                                                          f.getFilename(), cs));
            // Wait for remaining files availability from storage
            this.semaphore.acquire();
            logger.debug("All files ({}) are available.", dataFilesMultimap.keySet().size());
        } catch (RuntimeException e) { // Feign or network or ... exception
            // Put All data files in ERROR and propagate exception to make job fail
            dataFilesMultimap.values().forEach(df -> df.setState(FileState.ERROR));
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.info("Order job has been interrupted !");
        } finally {
            // All files have bean treated by storage, no more event subscriber needed...
            subscriber.unsubscribe(this);

            processJobInfoId
                    // NO PROCESSING
                    .onEmpty(() ->
                    // All order data files statuses are updated into database (if there is no process to launch)
                    dataFileService.save(dataFilesMultimap.values()))
                    // PROCESSING TO BE LAUNCHED
                    .peek(id -> {
                        // Delete the OrderDataFiles which were only temporary input files
                        orderDataFileRepository.deleteAll(dataFilesMultimap.values());
                        // Enqueue the processing job because all of its dependencies are ready (if there is a process to launch)
                        jobInfoService.enqueueJobForId(id);
                        // Nudge the order job service to enqueue next storage files jobs.
                        orderJobService.manageUserOrderStorageFilesJobInfos(user);
                    });
        }
    }

    public void changeFilesState(Set<String> checksumsAvailable, FileState fileState) {
        Set<String> availableFilesOrderedByThisJob = new HashSet<>(checksumsAvailable);
        availableFilesOrderedByThisJob.retainAll(dataFilesMultimap.keySet());
        availableFilesOrderedByThisJob.removeAll(alreadyHandledFiles);
        List<OrderDataFile> handledOrderDataFiles = new ArrayList<>(availableFilesOrderedByThisJob.size());
        for (String available: availableFilesOrderedByThisJob) {
            Collection<OrderDataFile> dataFiles = dataFilesMultimap.get(available);
            for (OrderDataFile df : dataFiles) {
                logger.debug("File {} - {} is now in state: {}.", df.getFilename(), df.getChecksum(), fileState);
                df.setState(fileState);
                handledOrderDataFiles.add(df);
            }
            alreadyHandledFiles.add(available);
            this.advanceCompletion();
            this.semaphore.release();
        }
        orderDataFileRepository.saveAll(handledOrderDataFiles);
    }
}
