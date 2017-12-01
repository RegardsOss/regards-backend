package fr.cnes.regards.framework.oais.urn.validator;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.cnes.regards.framework.oais.urn.UniformResourceName;

/**
 *
 * @author Sylvain VISSIERE-GUERINET
 */
public class RegardsOaisUrnAsStringValidator implements ConstraintValidator<RegardsOaisUrnAsString, String> {

    /**
     * Logger instance
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(RegardsOaisUrnAsStringValidator.class);

    @Override
    public void initialize(RegardsOaisUrnAsString constraintAnnotation) {
        //nothing to initialize
    }

    @Override
    public boolean isValid(String possibleUrn, ConstraintValidatorContext context) {
        boolean validity = false;
        try {
            UniformResourceName urn = UniformResourceName.fromString(possibleUrn);
            validity = new RegardsOaisUrnValidator().isValid(urn, context);
        } catch (IllegalArgumentException e) {
            // UniformResourceName.fromString(String) uses assertion to check if the argument matches the urn pattern.
            // If it fails it throws an IllegalArguementException which means that it is not a urn.
            LOGGER.trace(possibleUrn + " is expected to be an urn but is not", e);
        }
        return validity;
    }
}
