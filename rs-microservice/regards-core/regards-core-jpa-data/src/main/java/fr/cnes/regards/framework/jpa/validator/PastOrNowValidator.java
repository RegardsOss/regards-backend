/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.jpa.validator;

import java.time.LocalDateTime;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

/**
 * @author svissier
 *
 */
public class PastOrNowValidator implements ConstraintValidator<PastOrNow, LocalDateTime> {

    /*
     * (non-Javadoc)
     *
     * @see javax.validation.ConstraintValidator#initialize(java.lang.annotation.Annotation)
     */
    @Override
    public void initialize(PastOrNow pArg0) {
        // Nothing to initialize for now
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.validation.ConstraintValidator#isValid(java.lang.Object, javax.validation.ConstraintValidatorContext)
     */
    @Override
    public boolean isValid(LocalDateTime pArg0, ConstraintValidatorContext pArg1) {
        LocalDateTime now = LocalDateTime.now();
        return (pArg0 == null) || pArg0.isBefore(now) || pArg0.isEqual(now);
    }

}
