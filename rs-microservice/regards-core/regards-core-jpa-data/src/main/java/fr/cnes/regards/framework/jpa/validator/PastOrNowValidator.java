/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.jpa.validator;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.time.OffsetDateTime;

/**
 * @author svissier
 *
 */
public class PastOrNowValidator implements ConstraintValidator<PastOrNow, OffsetDateTime> {

    @Override
    public void initialize(PastOrNow pArg0) {
        // Nothing to initialize for now
    }

    @Override
    public boolean isValid(OffsetDateTime date, ConstraintValidatorContext context) {
        OffsetDateTime now = OffsetDateTime.now();
        return (date == null) || !date.isAfter(now);
    }

}
