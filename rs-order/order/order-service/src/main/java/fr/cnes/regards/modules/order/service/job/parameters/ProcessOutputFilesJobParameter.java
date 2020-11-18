package fr.cnes.regards.modules.order.service.job.parameters;

import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.modules.order.domain.OrderDataFile;

public class ProcessOutputFilesJobParameter extends JobParameter {

    public static final String NAME = "processOutputFiles";

    public ProcessOutputFilesJobParameter(OrderDataFile[] value) {
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

    public static boolean isCompatible(JobParameter param) {
        return param.getName().equals(NAME);
    }
}
