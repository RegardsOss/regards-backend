package fr.cnes.regards.framework.utils.file.validation;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.security.Security;
import java.util.Set;

/**
 * Validate {@link HandledMessageDigestAlgorithm}
 * @author Sylvain VISSIERE-GUERINET
 */
public class HandledMessageDigestAlgorithmValidator
        implements ConstraintValidator<HandledMessageDigestAlgorithm, String> {

    @Override
    public void initialize(HandledMessageDigestAlgorithm constraintAnnotation) {
        //nothing to initialize
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        Set<String> digestAlgorithms = Security.getAlgorithms("MessageDigest");
        if (value == null) {
            return true;
        }
        if (digestAlgorithms != null && digestAlgorithms.contains(value)) {
            return true;
        } else {
            String messageTemplate = context.getDefaultConstraintMessageTemplate();
            String msg = String.format(messageTemplate, value);
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(msg).addConstraintViolation();
            return false;
        }
    }
}
