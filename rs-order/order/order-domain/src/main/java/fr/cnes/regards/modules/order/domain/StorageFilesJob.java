package fr.cnes.regards.modules.order.domain;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import fr.cnes.regards.framework.modules.jobs.domain.AbstractJob;
import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterInvalidException;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterMissingException;

/**
 * @author oroussel
 */
public class StorageFilesJob extends AbstractJob<Void> {

    private int filesCount;

    /**
     * Map { checksum -> dataFile } of not yet available data files.
     * Each time an event is received (with checksum), corresponding entry is removed from map
     */
    private Map<String, OrderDataFile> notAvailableMap = new HashMap<>();

//    @Autowired
//    private ISubscriber<StorageFileEvent> subscriber

    @Override
    public void setParameters(Set<JobParameter> parameters)
            throws JobParameterMissingException, JobParameterInvalidException {
        if (parameters.isEmpty()) {
            throw new JobParameterMissingException("No parameter provided");
        }
        if (parameters.size() != 1) {
            throw new JobParameterInvalidException("Only one parameter is expected.");
        }
        JobParameter param = parameters.iterator().next();
        if (!(param instanceof StorageFilesJobParameter)) {
            throw new JobParameterInvalidException("Please use StorageFilesJobParameter in place of JobParameter (this "
                                                           + "class is here to facilitate your life so please use it.");
        }
        OrderDataFile[] files = param.getValue();
        filesCount = files.length;
        for (OrderDataFile dataFile : files) {
            notAvailableMap.put(dataFile.getChecksum(), dataFile);
        }
        super.parameters = parameters;
    }

    @Override
    public void run() {

    }

    @Override
    public int getCompletionCount() {
        return filesCount;
    }
}
