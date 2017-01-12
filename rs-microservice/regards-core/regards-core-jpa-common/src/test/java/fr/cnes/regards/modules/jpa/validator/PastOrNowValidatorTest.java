/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.jpa.validator;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import fr.cnes.regards.framework.jpa.validator.PastOrNow;

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
        final PastOrNowDate min = new PastOrNowDate(LocalDateTime.MIN);
        final PastOrNowDate lastNano = new PastOrNowDate(
                LocalDateTime.from(LocalDateTime.now().minus(Duration.ofNanos(1))));

        final Set<ConstraintViolation<PastOrNowDate>> minConstraintViolations = validator.validate(min);
        final Set<ConstraintViolation<PastOrNowDate>> lastNanoConstraintViolations = validator.validate(lastNano);

        Assert.assertEquals(0, minConstraintViolations.size());
        Assert.assertEquals(0, lastNanoConstraintViolations.size());
    }

    @Test
    public void testFuture() {
        final PastOrNowDate future = new PastOrNowDate(LocalDateTime.now().plus(Duration.ofMinutes(1)));

        final Set<ConstraintViolation<PastOrNowDate>> futureConstraintViolations = validator.validate(future);
        Assert.assertEquals(1, futureConstraintViolations.size());
        Assert.assertEquals("{" + PastOrNow.CLASS_NAME + "message}",
                            futureConstraintViolations.iterator().next().getMessage());
    }

    @Test
    public void testNow() {
        final PastOrNowDate now = new PastOrNowDate(LocalDateTime.now());

        final Set<ConstraintViolation<PastOrNowDate>> nowConstraintViolations = validator.validate(now);

        Assert.assertEquals(0, nowConstraintViolations.size());
    }

}
