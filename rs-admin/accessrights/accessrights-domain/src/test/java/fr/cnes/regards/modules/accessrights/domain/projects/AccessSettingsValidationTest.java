/*
 * Copyright 2017-2021 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.accessrights.domain.projects;

import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;

/**
 * Validate hibernate constraints on {@link AccessSettings}.
 *
 * @author CS SI
 */
public class AccessSettingsValidationTest {

    /**
     * Javax validator
     */
    private static Validator validator;

    /**
     * Set up the validator
     */
    @BeforeClass
    public static void setUpValidator() {
        final ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    /**
     * Check that the system fails when a wrong <code>mode</code> is set on an {@link AccessSettings}.
     */
    @Test
    @Requirement("?")
    @Purpose("Check that the system fails when a wrong mode is set on an AccountSettings.")
    public void modeIsDifferentFromAllowedValues() {
        // Init the malformed object
        final AccessSettings settings = new AccessSettings();
        settings.setId(0L);
        settings.setMode("wrong string");

        // Run the validator
        final Set<ConstraintViolation<AccessSettings>> constraintViolations = validator.validate(settings);

        // Check constraint violations
        Assert.assertEquals(1, constraintViolations.size());
    }

    /**
     * Check that the system allows <code>mode</code> from a list of allowed values.
     */
    @Test
    @Requirement("?")
    @Purpose("Check that the system allows mode from a list of allowed values.")
    public void modeIsAllowedValue() {
        // Init the malformed object
        final AccessSettings settings = new AccessSettings();
        settings.setId(0L);
        settings.setMode("manual");

        // Run the validator
        Set<ConstraintViolation<AccessSettings>> constraintViolations = validator.validate(settings);

        // Check constraint violations
        Assert.assertEquals(0, constraintViolations.size());

        settings.setMode("auto-accept");

        // Run the validator
        constraintViolations = validator.validate(settings);

        // Check constraint violations
        Assert.assertEquals(0, constraintViolations.size());
    }

}
