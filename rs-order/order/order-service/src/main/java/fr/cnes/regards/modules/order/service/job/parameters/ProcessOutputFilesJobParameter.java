package fr.cnes.regards.modules.order.service.job.parameters;

import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.modules.order.domain.OrderDataFile;

public class ProcessOutputFilesJobParameter extends JobParameter {

    public static final String NAME = "processOutputFileIds";

    public ProcessOutputFilesJobParameter(Long[] value) {
        super(NAME, value);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Long[] getValue() {
        return super.getValue();
    }

    public void setValue(Long[] value) {
        super.setValue(value);
    }

    public static boolean isCompatible(JobParameter param) {
        return param.getName().equals(NAME);
    }
}
