package fr.cnes.regards.modules.order.service.job.parameters;

import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.modules.processing.domain.dto.PProcessDTO;
import fr.cnes.regards.modules.processing.order.OrderProcessInfo;

public class ProcessDTOJobParameter extends JobParameter {

    public static final String NAME = "processDesc";

    public ProcessDTOJobParameter(OrderProcessInfo value) {
        super(NAME, value);
    }

    @SuppressWarnings("unchecked")
    @Override
    public PProcessDTO getValue() {
        return super.getValue();
    }

    public void setValue(PProcessDTO value) {
        super.setValue(value);
    }

    public static boolean isCompatible(JobParameter param) {
        return param.getName().equals(NAME);
    }

}
