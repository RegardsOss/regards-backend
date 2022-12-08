/*
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.model.service;

import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.framework.urn.EntityType;
import fr.cnes.regards.modules.model.dao.IModelAttrAssocRepository;
import fr.cnes.regards.modules.model.dao.IModelRepository;
import fr.cnes.regards.modules.model.domain.Model;
import fr.cnes.regards.modules.model.service.xml.IComputationPluginService;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.context.ApplicationEventPublisher;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Stephane Cortine
 */
@RunWith(MockitoJUnitRunner.class)
public class ModelServiceTest {

    @Mock
    private IModelRepository mockModelRepository;

    @Mock
    private IModelAttrAssocRepository mockModelAttrAssocRepository;

    @Mock
    private IAttributeModelService mockAttributeModelSrv;

    @Mock
    private IPluginService mockPluginSrv;

    @Mock
    private ApplicationEventPublisher mockApplicationEventPublisher;

    @Mock
    private IPublisher mockPublisher;

    @Mock
    private IComputationPluginService mockComputationPluginSrv;

    private IModelService modelSrv;

    @Before
    public void beforeTest() {
        modelSrv = new ModelService(mockModelRepository,
                                    mockModelAttrAssocRepository,
                                    mockAttributeModelSrv,
                                    mockPluginSrv,
                                    mockApplicationEventPublisher,
                                    mockPublisher,
                                    mockComputationPluginSrv);
    }

    @Test
    public void test_getModels_with_type() {
        // Given
        List<Model> models = new ArrayList<>();
        models.add(Model.build("Bmodel", "description", EntityType.DATA));
        models.add(Model.build("Amodel", "description", EntityType.DATA));
        models.add(Model.build("Cmodel", "description", EntityType.DATA));
        EntityType type = EntityType.DATA;

        Mockito.when(mockModelRepository.findByType(type)).thenReturn(models);

        // When
        List<Model> results = modelSrv.getModels(type);

        // Then
        Assert.assertNotNull(results);
        Assert.assertEquals(3, results.size());
        Assert.assertEquals("Amodel", results.get(0).getName());
        Assert.assertEquals("Bmodel", results.get(1).getName());
        Assert.assertEquals("Cmodel", results.get(2).getName());
    }

    @Test
    public void test_getModels_without_type() {
        // Given
        List<Model> models = new ArrayList<>();
        models.add(Model.build("Bmodel", "description", EntityType.DATA));
        models.add(Model.build("Amodel", "description", EntityType.COLLECTION));
        models.add(Model.build("Cmodel", "description", EntityType.DATASET));

        Mockito.when(mockModelRepository.findAll()).thenReturn(models);

        // When
        List<Model> results = modelSrv.getModels(null);

        // Then
        Assert.assertNotNull(results);
        Assert.assertEquals(3, results.size());
        Assert.assertEquals("Amodel", results.get(0).getName());
        Assert.assertEquals("Bmodel", results.get(1).getName());
        Assert.assertEquals("Cmodel", results.get(2).getName());
    }

}
