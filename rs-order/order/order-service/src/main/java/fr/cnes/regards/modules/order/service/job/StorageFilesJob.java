package fr.cnes.regards.modules.order.service.job;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Semaphore;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.amqp.domain.TenantWrapper;
import fr.cnes.regards.framework.modules.jobs.domain.AbstractJob;
import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterInvalidException;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterMissingException;
import fr.cnes.regards.modules.order.domain.FileState;
import fr.cnes.regards.modules.order.domain.OrderDataFile;
import fr.cnes.regards.modules.order.service.IOrderDataFileService;
import fr.cnes.regards.modules.storage.client.IAipClient;
import fr.cnes.regards.modules.storage.domain.database.AvailabilityRequest;
import fr.cnes.regards.modules.storage.domain.event.DataFileEvent;

/**
 * @author oroussel
 */
public class StorageFilesJob extends AbstractJob<Void> {

    private static final Logger LOGGER = LoggerFactory.getLogger(StorageFilesJob.class);

    private int filesCount;

    private OffsetDateTime expirationDate;

    @Autowired
    private ISubscriber subscriber;

    @Autowired
    private IAipClient aipClient;

    @Autowired
    private IOrderDataFileService dataFileService;

    private Semaphore semaphore;

    /**
     * Map { checksum -> dataFile } of not yet available data files.
     * Each time an event is received (with checksum), corresponding entry is removed from map
     */
    private Map<String, OrderDataFile> notAvailableMap = new HashMap<>();

    @Override
    public void setParameters(Set<JobParameter> parameters)
            throws JobParameterMissingException, JobParameterInvalidException {
        if (parameters.isEmpty()) {
            throw new JobParameterMissingException("No parameter provided");
        }
        if (parameters.size() != 2) {
            throw new JobParameterInvalidException("Two parameters are expected : 'files' and 'expirationDate'.");
        }
        for (JobParameter param : parameters) {
            if (!(param instanceof FilesJobParameter) && !(param instanceof ExpirationDateJobParameter)) {
                throw new JobParameterInvalidException(
                        "Please use FilesJobParameter and ExpirarionDateJobParameter in place of JobParameter (these "
                                + "classes are here to facilitate your life so please use them.");
            }
            if (param instanceof FilesJobParameter) {
                OrderDataFile[] files = param.getValue();
                filesCount = files.length;
                for (OrderDataFile dataFile : files) {
                    notAvailableMap.put(dataFile.getChecksum(), dataFile);
                }
            } else if (param instanceof ExpirationDateJobParameter) {
                expirationDate = param.getValue();
            }
        }
        super.parameters = parameters;
    }

    @Override
    public void run() {
        this.semaphore = new Semaphore(-notAvailableMap.size());
        subscriber.subscribeTo(DataFileEvent.class, this::handle);
        AvailabilityRequest request = new AvailabilityRequest();
        request.setChecksums(notAvailableMap.keySet());
        request.setExpirationDate(expirationDate);
        aipClient.makeFilesAvailable(request);
        try {
            this.semaphore.acquire();
        } catch (InterruptedException e) {
            return;
        }
        dataFileService.save(notAvailableMap.values());
    }

    private void handle(TenantWrapper<DataFileEvent> wrapper) {
        DataFileEvent event = wrapper.getContent();
        switch (event.getState()) {
            case AVAILABLE:
                notAvailableMap.get(event.getChecksum()).setState(FileState.AVAILABLE);
                break;
            case ERROR:
                notAvailableMap.get(event.getChecksum()).setState(FileState.ERROR);
                break;
        }
        this.semaphore.release();
    }

    @Override
    public int getCompletionCount() {
        return filesCount;
    }
}
