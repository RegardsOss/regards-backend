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
package fr.cnes.regards.modules.model.rest;

import fr.cnes.regards.framework.hateoas.IResourceService;
import fr.cnes.regards.modules.model.domain.attributes.AttributeModel;
import fr.cnes.regards.modules.model.domain.attributes.AttributeModelBuilder;
import fr.cnes.regards.modules.model.dto.properties.PropertyType;
import fr.cnes.regards.modules.model.service.IAttributeModelService;
import fr.cnes.regards.modules.model.service.IModelAttrAssocService;
import fr.cnes.regards.modules.model.service.RestrictionService;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;

/**
 * Attribute controller test
 *
 * @author msordi
 */
public class AttributeModelControllerTest {

    /**
     * Attribute service
     */
    private IAttributeModelService attributeServiceMocked;

    /**
     * Model attribute association service
     */
    private IModelAttrAssocService modelAttrAssocService;

    /**
     * Resource service
     */
    private IResourceService resourceServiceMocked;

    /**
     * {@link AttributeModelController}
     */
    private AttributeModelController attributeModelController;

    @Before
    public void init() {
        // Service
        attributeServiceMocked = Mockito.mock(IAttributeModelService.class);
        // Hateoas authorization
        resourceServiceMocked = Mockito.mock(IResourceService.class);
        modelAttrAssocService = Mockito.mock(IModelAttrAssocService.class);
        final RestrictionService restrictionService = Mockito.mock(RestrictionService.class);
        // Init controller
        attributeModelController = new AttributeModelController(attributeServiceMocked,
                                                                resourceServiceMocked,
                                                                modelAttrAssocService,
                                                                restrictionService);
    }

    @Test
    public void getAttributeTest() {
        // Given
        final List<AttributeModel> attributes = new ArrayList<>();
        attributes.add(new AttributeModelBuilder("NAME", PropertyType.STRING, "ForTests").setId(1L)
                                                                                         .setFragmentUsingDefault()
                                                                                         .build());
        attributes.add(new AttributeModelBuilder("START_DATE", PropertyType.DATE_ISO8601, "ForTests").setId(2L)
                                                                                                     .setFragmentUsingDefault()
                                                                                                     .build());
        attributes.add(new AttributeModelBuilder("STOP_DATE", PropertyType.DATE_ISO8601, "ForTests").setId(3L)
                                                                                                    .setFragmentUsingDefault()
                                                                                                    .build());

        Mockito.when(attributeServiceMocked.getAttributes(any(), any(), any(), any())).thenReturn(attributes);

        // When
        final ResponseEntity<List<EntityModel<AttributeModel>>> response = attributeModelController.getAttributes(null,
                                                                                                                  null,
                                                                                                                  null,
                                                                                                                  null);
        // Then
        Assert.assertEquals(attributes.size(), response.getBody().size());
    }

}
