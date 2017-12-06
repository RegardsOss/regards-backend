package fr.cnes.regards.modules.acquisition.domain.job;

import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.modules.acquisition.domain.AcquisitionProcessingChain;

/**
 * {@link JobParameter} specific class to be used with AcquisitionProductsJob
 * 
 * @author Christophe Mertz
 */
public class AcquisitionProcessingChainJobParameter extends JobParameter {

    public static final String NAME = "chain";

    public AcquisitionProcessingChainJobParameter(AcquisitionProcessingChain value) {
        super(NAME, value);
    }

    @SuppressWarnings("unchecked")
    @Override
    public AcquisitionProcessingChain getValue() {
        return super.getValue();
    }

    /**
     * Check if given {@link JobParameter} is compatible with {@link AcquisitionProcessingChainJobParameter} ie same name
     */
    public static boolean isCompatible(JobParameter param) {
        return param.getName().equals(NAME);
    }

}
