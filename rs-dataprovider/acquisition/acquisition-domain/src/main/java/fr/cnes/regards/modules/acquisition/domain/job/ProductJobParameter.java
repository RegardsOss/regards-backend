package fr.cnes.regards.modules.acquisition.domain.job;

import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.modules.acquisition.domain.Product;

/**
 * JobParameter specific class to be used with AcquisitionProductJob
 * 
 * @author Christophe Mertz
 */
public class ProductJobParameter extends JobParameter {

    public static final String NAME = "product";

    public ProductJobParameter(Product value) {
        super(NAME, value);
    }

    @Override
    public Product getValue() {
        return super.getValue();
    }

    public void setValue(Product value) {
        super.setValue(value);
    }
    
    /**
     * Check if given JobParameter is compatible with FilesJobParameter ie same name
     */
    public static boolean isCompatible(JobParameter param) {
        return param.getName().equals(NAME);
    }
    
}
