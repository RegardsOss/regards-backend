package fr.cnes.regards.modules.acquisition.domain.job;

import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.modules.acquisition.domain.Product;

/**
 * {@link JobParameter} specific class to be used with AcquisitionGenerateSIPJob
 * 
 * @author Christophe Mertz
 */
public class ProductJobParameter extends JobParameter {

    public static final String NAME = "product";

    public ProductJobParameter(Product value) {
        super(NAME, value);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Product getValue() {
        return super.getValue();
    }

    public void setValue(Product value) {
        super.setValue(value);
    }

    /**
     * Check if given {@link JobParameter} is compatible with ProductJobParameter ie same name
     */
    public static boolean isCompatible(JobParameter param) {
        return param.getName().equals(NAME);
    }

}
