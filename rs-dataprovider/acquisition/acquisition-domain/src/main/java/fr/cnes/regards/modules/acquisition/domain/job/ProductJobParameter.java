package fr.cnes.regards.modules.acquisition.domain.job;

import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;

/**
 * {@link JobParameter} specific class to be used with AcquisitionGenerateSIPJob
 * 
 * @author Christophe Mertz
 */
public class ProductJobParameter extends JobParameter {

    public static final String NAME = "product";

    public ProductJobParameter(String value) {
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

    /**
     * Check if given {@link JobParameter} is compatible with ProductJobParameter ie same name
     */
    public static boolean isCompatible(JobParameter param) {
        return param.getName().equals(NAME);
    }

}
