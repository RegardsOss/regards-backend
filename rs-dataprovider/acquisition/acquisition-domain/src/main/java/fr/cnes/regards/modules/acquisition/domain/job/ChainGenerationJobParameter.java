package fr.cnes.regards.modules.acquisition.domain.job;

import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.modules.acquisition.domain.ChainGeneration;

/**
 * {@link JobParameter} specific class to be used with AcquisitionProductsJob
 * 
 * @author Christophe Mertz
 */
public class ChainGenerationJobParameter extends JobParameter {

    public static final String NAME = "chain";

    public ChainGenerationJobParameter(ChainGeneration value) {
        super(NAME, value);
    }

    @SuppressWarnings("unchecked")
    @Override
    public ChainGeneration getValue() {
        return super.getValue();
    }

    public void setValue(ChainGeneration value) {
        super.setValue(value);
    }

    /**
     * Check if given {@link JobParameter} is compatible with ChainGenerationJobParameter ie same name
     */
    public static boolean isCompatible(JobParameter param) {
        return param.getName().equals(NAME);
    }

}
