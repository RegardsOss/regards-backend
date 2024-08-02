/*
 * Copyright 2017-2024 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.model.dao;

import fr.cnes.regards.modules.model.domain.attributes.AttributeModel;
import fr.cnes.regards.modules.model.domain.attributes.AttributeModelBuilder;
import fr.cnes.regards.modules.model.domain.attributes.restriction.*;
import fr.cnes.regards.modules.model.dto.properties.PropertyType;
import org.junit.Assert;
import org.junit.Test;

/**
 * Restriction test
 *
 * @author Marc Sordi
 */
public class RestrictionIT extends AbstractModelIT {

    @Test
    public void enumRestriction() {
        final AttributeModel attModel = new AttributeModelBuilder("ENUM",
                                                                  PropertyType.STRING,
                                                                  "ForTests").setEnumerationRestriction("FIRST",
                                                                                                        "SECOND")
                                                                             .build();
        saveAttribute(attModel);

        final AttributeModel att = findSingle();
        Assert.assertEquals(PropertyType.STRING, att.getType());
        final EnumerationRestriction er = checkRestrictionType(att.getRestriction(), EnumerationRestriction.class);
        Assert.assertEquals(2, er.getAcceptableValues().size());
    }

    /**
     * Test float restriction
     */
    @Test
    public void floatRestriction() {
        final AttributeModel attModel = new AttributeModelBuilder("VFLOAT",
                                                                  PropertyType.DOUBLE_INTERVAL,
                                                                  "ForTests").setFloatRangeRestriction(Double.MIN_VALUE,
                                                                                                       Double.MAX_VALUE,
                                                                                                       false,
                                                                                                       false).build();
        saveAttribute(attModel);

        final AttributeModel att = findSingle();
        Assert.assertEquals(PropertyType.DOUBLE_INTERVAL, att.getType());
        final DoubleRangeRestriction frr = checkRestrictionType(att.getRestriction(), DoubleRangeRestriction.class);
        Assert.assertTrue(Double.MIN_VALUE == frr.getMin());
        Assert.assertTrue(Double.MAX_VALUE == frr.getMax());
        Assert.assertFalse(frr.isMinExcluded());
        Assert.assertFalse(frr.isMaxExcluded());
    }

    /**
     * Test integer restriction
     */
    @Test
    public void integerRestriction() {
        final AttributeModel attModel = new AttributeModelBuilder("VINTEGER",
                                                                  PropertyType.INTEGER_ARRAY,
                                                                  "ForTests").setIntegerRangeRestriction(Integer.MIN_VALUE,
                                                                                                         Integer.MAX_VALUE,
                                                                                                         false,
                                                                                                         true).build();
        saveAttribute(attModel);

        final AttributeModel att = findSingle();
        Assert.assertEquals(PropertyType.INTEGER_ARRAY, att.getType());
        final IntegerRangeRestriction irr = checkRestrictionType(att.getRestriction(), IntegerRangeRestriction.class);
        Assert.assertTrue(Integer.MIN_VALUE == irr.getMin());
        Assert.assertTrue(Integer.MAX_VALUE == irr.getMax());
        Assert.assertFalse(irr.isMinExcluded());
        Assert.assertTrue(irr.isMaxExcluded());
    }

    @Test
    public void patternRestriction() {
        final String pattern = "pattern";
        final AttributeModel attModel = new AttributeModelBuilder("STRING",
                                                                  PropertyType.STRING_ARRAY,
                                                                  "ForTests").setPatternRestriction(pattern).build();
        saveAttribute(attModel);

        final AttributeModel att = findSingle();
        Assert.assertEquals(PropertyType.STRING_ARRAY, att.getType());
        final PatternRestriction pr = checkRestrictionType(att.getRestriction(), PatternRestriction.class);
        Assert.assertEquals(pattern, pr.getPattern());
    }

    @SuppressWarnings("unchecked")
    private <T extends AbstractRestriction> T checkRestrictionType(AbstractRestriction pRestriction, Class<T> pClass) {
        if (pRestriction != null) {
            Assert.assertTrue(pClass.isInstance(pRestriction));
            return (T) pRestriction;
        } else {
            throw new AssertionError("Missing restriction!");
        }
    }
}
