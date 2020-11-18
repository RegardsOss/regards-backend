package fr.cnes.regards.modules.order.service.job.parameters;

import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.modules.order.domain.OrderDataFile;

/**
 * JobParameter specific class to be used with StorageFilesJob, it contains the pair "files" : [ DataFiles ]
 * @author oroussel
 */
public class FilesJobParameter extends JobParameter {

    public static final String NAME = "files";

    public FilesJobParameter(OrderDataFile[] value) {
        super(NAME, value);
    }

    @SuppressWarnings("unchecked")
    @Override
    public OrderDataFile[] getValue() {
        return super.getValue();
    }

    public void setValue(OrderDataFile[] value) {
        super.setValue(value);
    }

    /**
     * Check if given JobParameter is compatible with FilesJobParameter ie same name
     * @param param
     * @return {@link Boolean}
     */
    public static boolean isCompatible(JobParameter param) {
        return param.getName().equals(NAME);
    }
}
