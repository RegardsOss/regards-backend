package fr.cnes.regards.framework.oais.validator;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.Iterator;
import java.util.stream.Collectors;

import fr.cnes.regards.framework.oais.AbstractInformationPackage;
import fr.cnes.regards.framework.oais.InformationPackageProperties;
import fr.cnes.regards.framework.oais.OAISDataObject;
import fr.cnes.regards.framework.oais.urn.DataType;
import fr.cnes.regards.framework.oais.urn.EntityType;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
public class DataWithRawdataValidator implements ConstraintValidator<DataWithRawdata, AbstractInformationPackage> {

    @Override
    public void initialize(DataWithRawdata constraintAnnotation) {
        // Nothing to do
    }

    @Override
    public boolean isValid(AbstractInformationPackage value, ConstraintValidatorContext context) {
        // Only DATA has special validation
        if (value.getIpType() == EntityType.DATA) {
            // lets see if there is at least one file representing a RAWDATA
            boolean hasRawData = false;
            Iterator<OAISDataObject> oaisDataObjectIterator = ((InformationPackageProperties) value.getProperties())
                    .getContentInformations().stream().map(ci -> ci.getDataObject()).collect(Collectors.toList())
                    .iterator();
            while (!hasRawData && oaisDataObjectIterator.hasNext()) {
                OAISDataObject oaisDataObject = oaisDataObjectIterator.next();
                if (oaisDataObject.getRegardsDataType() == DataType.RAWDATA) {
                    hasRawData = true;
                }
            }
            return hasRawData;
        }

        return true;
    }

}
