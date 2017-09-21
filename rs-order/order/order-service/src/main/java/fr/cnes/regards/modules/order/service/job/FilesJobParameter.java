package fr.cnes.regards.modules.order.service.job;

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

    @Override
    public OrderDataFile[] getValue() {
        return super.getValue();
    }

    public void setValue(OrderDataFile[] value) {
        super.setValue(value);
    }
}
