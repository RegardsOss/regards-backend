package fr.cnes.regards.modules.order.service.job;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Semaphore;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.Sets;

import fr.cnes.regards.framework.amqp.domain.IHandler;
import fr.cnes.regards.framework.amqp.domain.TenantWrapper;
import fr.cnes.regards.framework.feign.security.FeignSecurityManager;
import fr.cnes.regards.framework.modules.jobs.domain.AbstractJob;
import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterInvalidException;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterMissingException;
import fr.cnes.regards.modules.order.domain.FileState;
import fr.cnes.regards.modules.order.domain.OrderDataFile;
import fr.cnes.regards.modules.order.service.IOrderDataFileService;
import fr.cnes.regards.modules.storage.client.IAipClient;
import fr.cnes.regards.modules.storage.domain.AvailabilityRequest;
import fr.cnes.regards.modules.storage.domain.AvailabilityResponse;
import fr.cnes.regards.modules.storage.domain.event.DataFileEvent;

/**
 * Job that ask files availability to storage microservice and wait for all files availability or error
 * @author oroussel
 */
public class StorageFilesJob extends AbstractJob<Void> implements IHandler<DataFileEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(StorageFilesJob.class);

    private OffsetDateTime expirationDate;

    private String user;

    private String role;

    @Autowired
    private IForwardingDataFileEventHandlerService subscriber;

    @Autowired
    private IAipClient aipClient;

    @Autowired
    private IOrderDataFileService dataFileService;

    private Semaphore semaphore;

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
            if (!FilesJobParameter.isCompatible(param) && !(ExpirationDateJobParameter.isCompatible(param))
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
            } else if (ExpirationDateJobParameter.isCompatible(param)) {
                expirationDate = param.getValue();
            } else if (UserJobParameter.isCompatible(param)) {
                user = param.getValue();
            } else if (UserRoleJobParameter.isCompatible(param)) {
                role = param.getValue();
            }
        }
    }

    @Override
    public void run() {
        this.semaphore = new Semaphore(-dataFilesMultimap.keySet().size() + 1);
        subscriber.subscribe(this);
        AvailabilityRequest request = new AvailabilityRequest();
        request.setChecksums(dataFilesMultimap.keySet());
        request.setExpirationDate(expirationDate);
        try {
            FeignSecurityManager.asUser(user, role);
            AvailabilityResponse response = aipClient.makeFilesAvailable(request).getBody();

            // Update all already available files
            boolean atLeastOneDataFileIntoResponse = false;
            for (String checksum : response.getAlreadyAvailable()) {
                for (OrderDataFile dataFile : dataFilesMultimap.get(checksum)) {
                    LOGGER.debug("File {} - {} is already available.", dataFile.getFilename(), checksum);
                    dataFile.setState(FileState.AVAILABLE);
                    atLeastOneDataFileIntoResponse = true;
                }
                this.semaphore.release();
            }
            // Update all files in error
            for (String checksum : response.getErrors()) {
                LOGGER.error("File {} cannot be retrieved.", checksum);
                dataFilesMultimap.get(checksum).forEach(f -> f.setState(FileState.ERROR));
                atLeastOneDataFileIntoResponse = true;
                this.semaphore.release();
            }
            // Update all dataFiles state if at least one is already available or in error
            if (atLeastOneDataFileIntoResponse) {
                dataFileService.save(dataFilesMultimap.values());
            }
            dataFilesMultimap.forEach((cs, f) -> LOGGER.debug("Order job is waiting for {} file {} - {} availability.",
                                                         dataFilesMultimap.size(), f.getFilename(), cs));
            // Wait for remaining files availability from storage
            try {
                this.semaphore.acquire();
            } catch (InterruptedException e) {
                return;
            }

            LOGGER.debug("All files ({}) are available.", dataFilesMultimap.keySet().size());
            // All files have bean treated by storage, no more event subscriber needed...
            subscriber.unsubscribe(this);
            // ...and all order data files statuses are updated into database
            dataFileService.save(dataFilesMultimap.values());
        } catch (RuntimeException e) { // Feign or network or ... exception
            // Put All data files in ERROR and propagate exception to make job fail
            dataFilesMultimap.values().forEach(df -> df.setState(FileState.ERROR));
            dataFileService.save(dataFilesMultimap.values());
            throw e;
        } finally {
            FeignSecurityManager.reset();
        }
    }

    /**
     * Handle Events from storage about all files availability asking
     * Each time an event come back from storage, a token is released through semaphore
     */
    @Override
    public void handle(TenantWrapper<DataFileEvent> wrapper) {
        DataFileEvent event = wrapper.getContent();
        if (!dataFilesMultimap.containsKey(event.getChecksum())) {
            return;
        }
        if (alreadyHandledFiles.contains(event.getChecksum())) {
            return;
        }
        Collection<OrderDataFile> dataFiles = dataFilesMultimap.get(event.getChecksum());
        switch (event.getState()) {
            case AVAILABLE:
                for (OrderDataFile df : dataFiles) {
                    LOGGER.debug("File {} - {} is now available.", df.getFilename(), df.getChecksum());
                    df.setState(FileState.AVAILABLE);
                }
                alreadyHandledFiles.add(event.getChecksum());
                break;
            case ERROR:
                for (OrderDataFile df : dataFiles) {
                    LOGGER.debug("File {} - {} is now in error.", df.getFilename(), df.getChecksum());
                    df.setState(FileState.ERROR);
                }
                alreadyHandledFiles.add(event.getChecksum());
                break;
        }
        this.semaphore.release();
    }

    @Override
    public int getCompletionCount() {
        return dataFilesMultimap.keySet().size();
    }
}
