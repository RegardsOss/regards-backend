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

import com.google.common.collect.Iterables;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.modules.model.domain.attributes.AttributeModel;
import fr.cnes.regards.modules.model.domain.attributes.AttributeModelBuilder;
import fr.cnes.regards.modules.model.domain.attributes.Fragment;
import fr.cnes.regards.modules.model.domain.attributes.restriction.AbstractRestriction;
import fr.cnes.regards.modules.model.dto.properties.PropertyType;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Some cleaning tests
 *
 * @author Marc Sordi
 */
@MultitenantTransactional
public class CleanDatabaseIT extends AbstractModelIT {

    /**
     * Logger
     */
    @SuppressWarnings("unused")
    private static final Logger LOG = LoggerFactory.getLogger(CleanDatabaseIT.class);

    @Test
    public void deleteAttributeWithRestriction() {
        final AttributeModel attModel = new AttributeModelBuilder("withRestrictionAttribute",
                                                                  PropertyType.STRING,
                                                                  "ForTests").setPatternRestriction("MOCKPATTERN")
                                                                             .build();
        final AttributeModel saved = saveAttribute(attModel);
        attModelRepository.delete(saved);

        final Iterable<AbstractRestriction> it = restrictionRepository.findAll();
        Assert.assertEquals(0, Iterables.size(it));
    }

    /**
     * Test attribute removal
     */
    @Test
    public void deleteAttributeWithNoRestriction() {

        final AttributeModel attModel = new AttributeModelBuilder("TO_DELETE",
                                                                  PropertyType.STRING,
                                                                  "ForTests").setNoRestriction().build();
        final AttributeModel saved = saveAttribute(attModel);

        final AttributeModel attModel2 = new AttributeModelBuilder("TO_DELETE_TWO",
                                                                   PropertyType.STRING,
                                                                   "ForTests").setNoRestriction().build();
        saveAttribute(attModel2);

        attModelRepository.delete(saved);

        // Only one attribute model is deleted
        // The default fragment is not removed
        final Fragment defaultFragment = fragmentRepository.findByName(Fragment.getDefaultName());
        Assert.assertNotNull(defaultFragment);
    }
}
