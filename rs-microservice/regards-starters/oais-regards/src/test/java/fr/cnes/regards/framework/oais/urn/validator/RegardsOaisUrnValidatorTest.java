/*
 * Copyright 2017-2020 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.framework.oais.urn.validator;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import java.util.Set;
import java.util.UUID;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import fr.cnes.regards.framework.oais.urn.OAISIdentifier;
import fr.cnes.regards.framework.oais.urn.OaisUniformResourceName;
import fr.cnes.regards.framework.oais.validator.RegardsOaisUrn;
import fr.cnes.regards.framework.urn.EntityType;

/**
 * @author Sylvain Vissiere-Guerinet
 */
public class RegardsOaisUrnValidatorTest {

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
    public void testFullAip() {
        final OaisUniformResourceName urn = new OaisUniformResourceName(OAISIdentifier.AIP,
                                                                        EntityType.DATA,
                                                                        "Tenant123",
                                                                        UUID.randomUUID(),
                                                                        1,
                                                                        3L,
                                                                        "4");
        final Set<ConstraintViolation<OaisUniformResourceName>> urnConstraintViolation = validator.validate(urn);
        Assert.assertEquals(0, urnConstraintViolation.size());
    }

    @Test
    public void testMinimalAip() {
        final OaisUniformResourceName urn = new OaisUniformResourceName(OAISIdentifier.AIP,
                                                                        EntityType.DATA,
                                                                        "Tenant123",
                                                                        UUID.randomUUID(),
                                                                        1,
                                                                        null,
                                                                        null);
        final Set<ConstraintViolation<OaisUniformResourceName>> urnConstraintViolation = validator.validate(urn);
        Assert.assertEquals(0, urnConstraintViolation.size());
    }

    @Test
    public void testOrderAip() {
        final OaisUniformResourceName urn = new OaisUniformResourceName(OAISIdentifier.AIP,
                                                                        EntityType.DATA,
                                                                        "Tenant123",
                                                                        UUID.randomUUID(),
                                                                        1,
                                                                        3L,
                                                                        null);
        final Set<ConstraintViolation<OaisUniformResourceName>> urnConstraintViolation = validator.validate(urn);
        Assert.assertEquals(0, urnConstraintViolation.size());
    }

    @Test
    public void testRevisionAip() {
        final OaisUniformResourceName urn = new OaisUniformResourceName(OAISIdentifier.AIP,
                                                                        EntityType.DATA,
                                                                        "Tenant123",
                                                                        UUID.randomUUID(),
                                                                        1,
                                                                        null,
                                                                        "3");
        final Set<ConstraintViolation<OaisUniformResourceName>> urnConstraintViolation = validator.validate(urn);
        Assert.assertEquals(0, urnConstraintViolation.size());
    }

    @Test
    public void testValidSip() {
        final OaisUniformResourceName urn = new OaisUniformResourceName(OAISIdentifier.SIP,
                                                                        EntityType.DATA,
                                                                        "Tenant123",
                                                                        UUID.randomUUID(),
                                                                        1,
                                                                        null,
                                                                        null);
        final Set<ConstraintViolation<OaisUniformResourceName>> urnConstraintViolation = validator.validate(urn);
        Assert.assertEquals(0, urnConstraintViolation.size());
    }

    @Test
    public void testInvalidSipOrderAndRev() {
        final OaisUniformResourceName urn = new OaisUniformResourceName(OAISIdentifier.SIP,
                                                                        EntityType.DATA,
                                                                        "Tenant123",
                                                                        UUID.randomUUID(),
                                                                        1,
                                                                        3L,
                                                                        "1");
        final Set<ConstraintViolation<OaisUniformResourceName>> urnConstraintViolation = validator.validate(urn);
        Assert.assertEquals(1, urnConstraintViolation.size());
        Assert.assertEquals("{" + RegardsOaisUrn.CLASS_NAME + "message}",
                            urnConstraintViolation.iterator().next().getMessage());
    }

    @Test
    public void testInvalidOrderSip() {
        final OaisUniformResourceName urn = new OaisUniformResourceName(OAISIdentifier.SIP,
                                                                        EntityType.DATA,
                                                                        "Tenant123",
                                                                        UUID.randomUUID(),
                                                                        1,
                                                                        3L,
                                                                        null);
        final Set<ConstraintViolation<OaisUniformResourceName>> urnConstraintViolation = validator.validate(urn);
        Assert.assertEquals(1, urnConstraintViolation.size());
        Assert.assertEquals("{" + RegardsOaisUrn.CLASS_NAME + "message}",
                            urnConstraintViolation.iterator().next().getMessage());
    }

    @Test
    public void testInvalidRevisionSip() {
        final OaisUniformResourceName urn = new OaisUniformResourceName(OAISIdentifier.SIP,
                                                                        EntityType.DATA,
                                                                        "Tenant123",
                                                                        UUID.randomUUID(),
                                                                        1,
                                                                        null,
                                                                        "2");
        final Set<ConstraintViolation<OaisUniformResourceName>> urnConstraintViolation = validator.validate(urn);
        Assert.assertEquals(1, urnConstraintViolation.size());
        Assert.assertEquals("{" + RegardsOaisUrn.CLASS_NAME + "message}",
                            urnConstraintViolation.iterator().next().getMessage());
    }

}
