/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.access.services.rest.aggregator;

import java.util.Collection;
import java.util.List;

import org.assertj.core.util.Lists;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import fr.cnes.regards.framework.hateoas.HateoasUtils;
import fr.cnes.regards.modules.access.services.domain.aggregator.PluginServiceDto;
import fr.cnes.regards.modules.access.services.domain.ui.UIPluginConfiguration;
import fr.cnes.regards.modules.access.services.rest.AccessServicesITConfiguration;
import fr.cnes.regards.modules.access.services.rest.assembler.PluginServiceDtoResourcesAssembler;
import fr.cnes.regards.modules.access.services.service.ui.IUIPluginConfigurationService;
import fr.cnes.regards.modules.catalog.services.client.ICatalogServicesClient;
import fr.cnes.regards.modules.catalog.services.domain.ServiceScope;
import fr.cnes.regards.modules.catalog.services.domain.dto.PluginConfigurationDto;

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
     * @throws java.lang.Exception
     */
    @Before
    @SuppressWarnings("unchecked")
    public void setUp() throws Exception {
        // Mock Catalog Services
        catalogServicesClient = Mockito.mock(ICatalogServicesClient.class);
        PluginConfigurationDto dto = new AccessServicesITConfiguration().dummyPluginConfigurationDto();
        Mockito.when(catalogServicesClient.retrieveServices(Mockito.anyListOf(String.class), Mockito.any()))
                .thenReturn(new ResponseEntity<List<Resource<PluginConfigurationDto>>>(
                        HateoasUtils.wrapList(Lists.newArrayList(dto)), HttpStatus.OK));

        // Mock Ui Services
        uiPluginConfigurationService = Mockito.mock(IUIPluginConfigurationService.class);
        UIPluginConfiguration uiPluginConfiguration = new AccessServicesITConfiguration().dummyUiPluginConfiguration();
        Mockito.when(uiPluginConfigurationService.retrieveActivePluginServices(Mockito.anyListOf(String.class),
                                                                               Mockito.any()))
                .thenReturn(Lists.newArrayList(uiPluginConfiguration));

        // Mock the resource assembler
        PluginServiceDtoResourcesAssembler assembler = Mockito.mock(PluginServiceDtoResourcesAssembler.class);
        Mockito.when(assembler.toResources(Mockito.anyCollection()))
                .thenAnswer(pInvocation -> HateoasUtils.wrapCollection(pInvocation.getArgumentAt(0, Collection.class)));

        // Construct controller with mocked deps
        controller = new ServicesAggregatorController(catalogServicesClient, uiPluginConfigurationService, assembler);
    }

    /**
     * Test method for {@link fr.cnes.regards.modules.access.services.rest.aggregator.ServicesAggregatorController#retrieveServices(java.lang.String, fr.cnes.regards.modules.catalog.services.domain.ServiceScope)}.
     */
    @Test
    public final void testRetrieveServices() {
        ResponseEntity<List<Resource<PluginServiceDto>>> result = controller
                .retrieveServices(Lists.newArrayList("coucou"), Lists.newArrayList(ServiceScope.MANY));
        Assert.assertNotNull(result);
        Assert.assertThat(result.getBody(), Matchers.hasSize(2));
    }

}
