/*
 * Copyright 2017-2018 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
 *
 * This file is part of REGARDS.
 *
 * REGARDS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * REGARDS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with REGARDS. If not, see <http://www.gnu.org/licenses/>.
 */
package fr.cnes.regards.modules.jpa.validator;

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
