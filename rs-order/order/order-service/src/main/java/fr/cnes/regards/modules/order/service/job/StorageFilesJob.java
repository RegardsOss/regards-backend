package fr.cnes.regards.modules.order.service.job;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Semaphore;

import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

import fr.cnes.regards.framework.modules.jobs.domain.AbstractJob;
import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterInvalidException;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterMissingException;
import fr.cnes.regards.modules.order.domain.FileState;
import fr.cnes.regards.modules.order.domain.OrderDataFile;
import fr.cnes.regards.modules.order.service.IOrderDataFileService;
import fr.cnes.regards.modules.storage.client.IStorageClient;

public class StorageFilesJob extends AbstractJob<Void> {

    @Autowired
    private IStorageClient storageClient;

    private Integer subOrderValidationPeriodDays;

    private Semaphore semaphore;

    @Autowired
    private IStorageFileListenerService subscriber;

    /**
     * Map { checksum -> ( dataFiles) } of data files.
     * Same file can be part of 2 or more data objects so with same checksum but different OrderDataFile (thanks to
     * uri)
     * The case where checksum is the same for 2 different files is abandoned (never mind)
     */
    private final Multimap<String, OrderDataFile> dataFilesMultimap = HashMultimap.create();

    /**
     * Set of file checksums already handled by a DataStorageEvent.
     * Used in order to avoid listening on two same available events from storage.
     */
    private final Set<String> alreadyHandledFiles = Sets.newHashSet();

    @Autowired
    private IOrderDataFileService dataFileService;

    @Override
    public void setParameters(Map<String, JobParameter> parameters)
            throws JobParameterMissingException, JobParameterInvalidException {
        if (parameters.isEmpty()) {
            throw new JobParameterMissingException("No parameter provided");
        }
        if (parameters.size() != 4) {
            throw new JobParameterInvalidException(
                    "Four parameters are expected : 'files', 'expirationDate', 'user' and 'userRole'.");
        }
        for (JobParameter param : parameters.values()) {
            if (!FilesJobParameter.isCompatible(param) && !(SubOrderAvailabilityPeriodJobParameter.isCompatible(param))
                    && !UserJobParameter.isCompatible(param) && !UserRoleJobParameter.isCompatible(param)) {
                throw new JobParameterInvalidException(
                        "Please use FilesJobParameter, ExpirationDateJobParameter, UserJobParameter and "
                                + "UserRoleJobParameter in place of JobParameter (these "
                                + "classes are here to facilitate your life so please use them.");
            }
            if (FilesJobParameter.isCompatible(param)) {
                OrderDataFile[] files = param.getValue();
                for (OrderDataFile dataFile : files) {
                    dataFilesMultimap.put(dataFile.getChecksum(), dataFile);
                }
            } else if (SubOrderAvailabilityPeriodJobParameter.isCompatible(param)) {
                subOrderValidationPeriodDays = param.getValue();
            }
        }
    }

    @Override
    public void run() {
        this.semaphore = new Semaphore(-dataFilesMultimap.keySet().size() + 1);
        subscriber.subscribe(this);

        try {
            storageClient.makeAvailable(dataFilesMultimap.keySet(), OffsetDateTime.now().plusDays(subOrderValidationPeriodDays));
            dataFilesMultimap.forEach((cs, f) -> {
                LOGGER.debug("Order job is waiting for {} file {} - {} availability.", dataFilesMultimap.size(),
                             f.getFilename(), cs);
            });
            // Wait for remaining files availability from storage
            try {
                this.semaphore.acquire();
            } catch (InterruptedException e) {
                return;
            }

            logger.debug("All files ({}) are available.", dataFilesMultimap.keySet().size());
            // All files have bean treated by storage, no more event subscriber needed...
            subscriber.unsubscribe(this);
            // ...and all order data files statuses are updated into database
            dataFileService.save(dataFilesMultimap.values());
        } catch (RuntimeException e) { // Feign or network or ... exception
            // Put All data files in ERROR and propagate exception to make job fail
            dataFilesMultimap.values().forEach(df -> df.setState(FileState.ERROR));
            dataFileService.save(dataFilesMultimap.values());
            throw e;
        }
    }

    /**
     * Handle Events from storage about all files availability asking
     * Each time an event come back from storage, a token is released through semaphore
     */
    public void handle(String checksum, boolean available) {
        if (!dataFilesMultimap.containsKey(checksum)) {
            return;
        }
        if (alreadyHandledFiles.contains(checksum)) {
            return;
        }
        Collection<OrderDataFile> dataFiles = dataFilesMultimap.get(checksum);
        if (available) {
            for (OrderDataFile df : dataFiles) {
                logger.debug("File {} - {} is now available.", df.getFilename(), df.getChecksum());
                df.setState(FileState.AVAILABLE);
            }
            alreadyHandledFiles.add(checksum);
        } else {
            for (OrderDataFile df : dataFiles) {
                logger.debug("File {} - {} is now in error.", df.getFilename(), df.getChecksum());
                df.setState(FileState.ERROR);
            }
            alreadyHandledFiles.add(checksum);
        }
        this.semaphore.release();
    }

}
