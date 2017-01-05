/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.entities.service.validator.restriction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.Validator;

import fr.cnes.regards.modules.models.domain.attributes.restriction.AbstractRestriction;
import fr.cnes.regards.modules.models.domain.attributes.restriction.EnumerationRestriction;
import fr.cnes.regards.modules.models.domain.attributes.restriction.DoubleRangeRestriction;
import fr.cnes.regards.modules.models.domain.attributes.restriction.IntegerRangeRestriction;
import fr.cnes.regards.modules.models.domain.attributes.restriction.PatternRestriction;

/**
 * Restriction validator factory
 *
 * @author Marc Sordi
 *
 */
public final class RestrictionValidatorFactory {

    /**
     * Class logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(RestrictionValidatorFactory.class);

    private RestrictionValidatorFactory() {
    }

    public static Validator getValidator(AbstractRestriction pRestriction, String pAttributeKey) {
        String errorMessage = String.format("No validator found for restriction type %s and attribute %s.",
                                            pRestriction.getType(), pAttributeKey);
        LOGGER.debug(errorMessage);
        throw new UnsupportedOperationException(errorMessage);
    }

    public static Validator getValidator(EnumerationRestriction pRestriction, String pAttributeKey) {
        return new EnumerationValidator(pRestriction, pAttributeKey);
    }

    public static Validator getValidator(DoubleRangeRestriction pRestriction, String pAttributeKey) {
        return new DoubleRangeValidator(pRestriction, pAttributeKey);
    }

    public static Validator getValidator(IntegerRangeRestriction pRestriction, String pAttributeKey) {
        return new IntegerRangeValidator(pRestriction, pAttributeKey);
    }

    public static Validator getValidator(PatternRestriction pRestriction, String pAttributeKey) {
        return new PatternValidator(pRestriction, pAttributeKey);
    }
}
