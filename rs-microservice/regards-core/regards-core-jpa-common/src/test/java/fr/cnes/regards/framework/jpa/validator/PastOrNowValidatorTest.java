/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.jpa.validator;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Set;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author svissier
 *
 */
public class PastOrNowValidatorTest {

    /**
     * Validator
     */
    private static Validator validator;

    @BeforeClass
    public static void init() {
        final ValidatorFactory validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @Test
    public void testPast() {
        final PastOrNowDate min = new PastOrNowDate(OffsetDateTime.MIN);
        final PastOrNowDate lastNano = new PastOrNowDate(
                OffsetDateTime.from(OffsetDateTime.now().minus(Duration.ofNanos(1))));

        final Set<ConstraintViolation<PastOrNowDate>> minConstraintViolations = validator.validate(min);
        final Set<ConstraintViolation<PastOrNowDate>> lastNanoConstraintViolations = validator.validate(lastNano);

        Assert.assertEquals(0, minConstraintViolations.size());
        Assert.assertEquals(0, lastNanoConstraintViolations.size());
    }

    @Test
    public void testFuture() {
        final PastOrNowDate future = new PastOrNowDate(OffsetDateTime.now().plus(Duration.ofMinutes(1)));

        final Set<ConstraintViolation<PastOrNowDate>> futureConstraintViolations = validator.validate(future);
        Assert.assertEquals(1, futureConstraintViolations.size());
        Assert.assertEquals("{" + PastOrNow.CLASS_NAME + "message}",
                            futureConstraintViolations.iterator().next().getMessage());
    }

    @Test
    public void testNow() {
        final PastOrNowDate now = new PastOrNowDate(OffsetDateTime.now());

        final Set<ConstraintViolation<PastOrNowDate>> nowConstraintViolations = validator.validate(now);

        Assert.assertEquals(0, nowConstraintViolations.size());
    }

}
