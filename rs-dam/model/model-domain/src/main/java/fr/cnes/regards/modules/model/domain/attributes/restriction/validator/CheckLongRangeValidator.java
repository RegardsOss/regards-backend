package fr.cnes.regards.modules.model.domain.attributes.restriction.validator;

import fr.cnes.regards.modules.model.domain.attributes.restriction.LongRangeRestriction;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Validate range
 *
 * @author oroussel
 */
public class CheckLongRangeValidator implements ConstraintValidator<CheckLongRange, LongRangeRestriction> {

    @Override
    public void initialize(CheckLongRange pConstraintAnnotation) {
        // Nothing to do
    }

    @Override
    public boolean isValid(LongRangeRestriction pValue, ConstraintValidatorContext pContext) {
        return pValue.getMin() < pValue.getMax();
    }
}
