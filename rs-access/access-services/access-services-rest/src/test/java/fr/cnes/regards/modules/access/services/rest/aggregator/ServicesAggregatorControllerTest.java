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
package fr.cnes.regards.modules.access.services.rest.aggregator;

import fr.cnes.regards.framework.hateoas.HateoasUtils;
import fr.cnes.regards.framework.utils.plugins.PluginUtils;
import fr.cnes.regards.modules.access.services.domain.aggregator.PluginServiceDto;
import fr.cnes.regards.modules.access.services.domain.ui.UIPluginConfiguration;
import fr.cnes.regards.modules.access.services.rest.AccessServicesITConfiguration;
import fr.cnes.regards.modules.access.services.rest.assembler.PluginServiceDtoResourcesAssembler;
import fr.cnes.regards.modules.access.services.service.ui.IUIPluginConfigurationService;
import fr.cnes.regards.modules.catalog.services.client.ICatalogServicesClient;
import fr.cnes.regards.modules.catalog.services.domain.ServiceScope;
import fr.cnes.regards.modules.catalog.services.domain.dto.PluginConfigurationDto;
import org.assertj.core.util.Lists;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

/**
 * Unit Test for {@link ServicesAggregatorController}
 *
 * @author Xavier-Alexandre Brochard
 */
public class ServicesAggregatorControllerTest {

    /**
     * Controller under test
     */
    private ServicesAggregatorController controller;

    /**
     * The mocked client providing catalog services
     */
    private ICatalogServicesClient catalogServicesClient;

    /**
     * The mocked service provinding ui services
     */
    private IUIPluginConfigurationService uiPluginConfigurationService;

    /**
     *
     */
    @Before
    public void setUp() throws Exception {
        PluginUtils.setup();
        // Mock Catalog Services
        catalogServicesClient = Mockito.mock(ICatalogServicesClient.class);
        PluginConfigurationDto dto = new AccessServicesITConfiguration().dummyPluginConfigurationDto();
        Mockito.when(catalogServicesClient.retrieveServices(Mockito.anyList(), Mockito.any()))
               .thenReturn(new ResponseEntity<List<EntityModel<PluginConfigurationDto>>>(HateoasUtils.wrapList(Lists.newArrayList(
                   dto)), HttpStatus.OK));

        // Mock Ui Services
        uiPluginConfigurationService = Mockito.mock(IUIPluginConfigurationService.class);
        UIPluginConfiguration uiPluginConfiguration = new AccessServicesITConfiguration().dummyUiPluginConfiguration();
        Mockito.when(uiPluginConfigurationService.retrieveActivePluginServices(Mockito.anyList(), Mockito.any()))
               .thenReturn(Lists.newArrayList(uiPluginConfiguration));

        // Mock the resource assembler
        PluginServiceDtoResourcesAssembler assembler = Mockito.mock(PluginServiceDtoResourcesAssembler.class);
        Mockito.when(assembler.toResources(Mockito.anyCollection()))
               .thenAnswer(pInvocation -> HateoasUtils.wrapCollection(pInvocation.getArgument(0)));

        // Construct controller with mocked deps
        controller = new ServicesAggregatorController(catalogServicesClient, uiPluginConfigurationService, assembler);
    }

    /**
     * Test method for {@link fr.cnes.regards.modules.access.services.rest.aggregator.ServicesAggregatorController#retrieveServices}.
     */
    @Test
    public final void testRetrieveServices() {
        ResponseEntity<List<EntityModel<PluginServiceDto>>> result = controller.retrieveServices(Lists.newArrayList(
            "coucou"), Lists.newArrayList(ServiceScope.MANY));
        Assert.assertNotNull(result);
        Assert.assertThat(result.getBody(), Matchers.hasSize(2));
    }

}
