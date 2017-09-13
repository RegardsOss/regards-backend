package fr.cnes.regards.modules.order.domain;

import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;

/**
 * JobParameter specific class to be used with StorageFilesJob
 * @author oroussel
 */
public class StorageFilesJobParameter extends JobParameter {
    public static final String NAME = "files";

    public StorageFilesJobParameter(OrderDataFile[] value) {
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
