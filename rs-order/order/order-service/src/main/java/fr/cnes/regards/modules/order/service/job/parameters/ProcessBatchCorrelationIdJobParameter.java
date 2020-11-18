package fr.cnes.regards.modules.order.service.job.parameters;

import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;

public class ProcessBatchCorrelationIdJobParameter extends JobParameter {

    public static final String NAME = "batchCorrelationId";

    public ProcessBatchCorrelationIdJobParameter(String value) {
        super(NAME, value);
    }

    @SuppressWarnings("unchecked")
    @Override
    public String getValue() {
        return super.getValue();
    }

    public void setValue(String value) {
        super.setValue(value);
    }

    public static boolean isCompatible(JobParameter param) {
        return param.getName().equals(NAME);
    }

}
