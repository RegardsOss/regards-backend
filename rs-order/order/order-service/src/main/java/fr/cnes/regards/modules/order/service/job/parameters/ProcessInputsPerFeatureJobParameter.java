package fr.cnes.regards.modules.order.service.job.parameters;

import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;


public class ProcessInputsPerFeatureJobParameter extends JobParameter {

    public static final String NAME = "orderDataFileInputsPerFeature";

    public ProcessInputsPerFeatureJobParameter(ProcessInputsPerFeature value) {
        super(NAME, value);
    }

    @SuppressWarnings("unchecked")
    @Override
    public ProcessInputsPerFeature getValue() {
        return super.getValue();
    }

    public void setValue(ProcessInputsPerFeature value) {
        super.setValue(value);
    }

    public static boolean isCompatible(JobParameter param) {
        return param.getName().equals(NAME);
    }

}
