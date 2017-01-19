/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.storage.urn.validator;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import fr.cnes.regards.modules.storage.urn.OAISIdentifier;
import fr.cnes.regards.modules.storage.urn.UniformResourceName;

/**
 * @author Sylvain Vissiere-Guerinet
 *
 */
public class UrnValidator implements ConstraintValidator<URN, String> {

    @Override
    public void initialize(URN pConstraintAnnotation) {
        // nothing to initialize for now
    }

    @Override
    public boolean isValid(String pValue, ConstraintValidatorContext pContext) {
        // parsing will ensure that this supposed URN has the right format
        UniformResourceName urnObject = UniformResourceName.fromString(pValue);
        // verify that the parsed URN is valid
        return (urnObject == null) || !(urnObject.getOaisIdentifier().equals(OAISIdentifier.SIP)
                && ((urnObject.getOrder() != null) || (urnObject.getRevision() != null)));
    }
}
