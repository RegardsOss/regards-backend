package fr.cnes.regards.modules.order.service.job.parameters;

import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.modules.processing.order.OrderProcessInfo;

import java.util.UUID;

public class ProcessJobInfoJobParameter extends JobParameter {

    public static final String NAME = "processJobInfo";

    public ProcessJobInfoJobParameter(UUID value) {
        super(NAME, value);
    }

    @SuppressWarnings("unchecked")
    @Override
    public UUID getValue() {
        return super.getValue();
    }

    public void setValue(UUID value) {
        super.setValue(value);
    }

    public static boolean isCompatible(JobParameter param) {
        return param.getName().equals(NAME);
    }
}
