/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.core.validation.test;

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

import fr.cnes.regards.modules.core.validation.test.domain.PastOrNowDate;

/**
 * @author svissier
 *
 */
public class PastOrNowValidatorTest {

    private static Validator validator;

    @BeforeClass
    public static void init() {
        ValidatorFactory validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @Test
    public void TestPast() {
        PastOrNowDate min = new PastOrNowDate(LocalDateTime.MIN);
        PastOrNowDate lastNano = new PastOrNowDate(LocalDateTime.from(LocalDateTime.now().minus(Duration.ofNanos(1))));

        Set<ConstraintViolation<PastOrNowDate>> minConstraintViolations = validator.validate(min);
        Set<ConstraintViolation<PastOrNowDate>> lastNanoConstraintViolations = validator.validate(lastNano);

        Assert.assertEquals(0, minConstraintViolations.size());
        Assert.assertEquals(0, lastNanoConstraintViolations.size());
    }

    @Test
    public void TestFuture() {
        PastOrNowDate future = new PastOrNowDate(LocalDateTime.now().plus(Duration.ofMinutes(1)));

        Set<ConstraintViolation<PastOrNowDate>> futureConstraintViolations = validator.validate(future);
        Assert.assertEquals(1, futureConstraintViolations.size());
        Assert.assertEquals("{fr.cnes.modules.core.validation.PastOrNow.message}",
                            futureConstraintViolations.iterator().next().getMessage());
    }

    @Test
    public void TestNow() {
        PastOrNowDate now = new PastOrNowDate(LocalDateTime.now());

        Set<ConstraintViolation<PastOrNowDate>> nowConstraintViolations = validator.validate(now);

        Assert.assertEquals(0, nowConstraintViolations.size());
    }

}
