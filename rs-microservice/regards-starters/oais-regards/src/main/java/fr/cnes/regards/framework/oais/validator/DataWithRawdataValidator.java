package fr.cnes.regards.framework.oais.validator;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.cnes.regards.framework.oais.AbstractInformationPackage;
import fr.cnes.regards.framework.oais.ContentInformation;
import fr.cnes.regards.framework.oais.InformationPackageProperties;
import fr.cnes.regards.framework.oais.urn.DataType;
import fr.cnes.regards.framework.oais.urn.EntityType;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
@SuppressWarnings("rawtypes")
public class DataWithRawdataValidator implements ConstraintValidator<DataWithRawdata, AbstractInformationPackage> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DataWithRawdataValidator.class);

    @Override
    public void initialize(DataWithRawdata constraintAnnotation) {
        // Nothing to do
    }

    @Override
    public boolean isValid(AbstractInformationPackage value, ConstraintValidatorContext context) {
        // Only DATA has special validation
        if (value.getIpType() == EntityType.DATA) {
            InformationPackageProperties properties = (InformationPackageProperties) value.getProperties();
            if (properties == null) {
                // because of SIP which are references
                return true;
            }

            for (ContentInformation ci : properties.getContentInformations()) {
                if (ci == null) {
                    LOGGER.error("Null content information detected, JSON file may contain unnecessary commas!");
                    return false;
                }
                if (ci.getDataObject().getRegardsDataType() == DataType.RAWDATA) {
                    // At least one raw data is detected
                    return true;
                }
            }
            return false;
        }

        return true;
    }

}
