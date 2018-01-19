/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.access.services.service.ui;

import java.util.stream.Stream;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.google.common.collect.Lists;
import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.module.rest.exception.EntityException;
import fr.cnes.regards.modules.access.services.dao.ui.ILinkUIPluginsDatasetsRepository;
import fr.cnes.regards.modules.access.services.dao.ui.IUIPluginConfigurationRepository;
import fr.cnes.regards.modules.access.services.dao.ui.IUIPluginDefinitionRepository;
import fr.cnes.regards.modules.access.services.domain.ui.LinkUIPluginsDatasets;
import fr.cnes.regards.modules.access.services.domain.ui.UIPluginConfiguration;
import fr.cnes.regards.modules.entities.domain.Dataset;
import fr.cnes.regards.modules.models.domain.Model;

/**
 * Unit Test for {@link UIPluginConfigurationService}
 *
 * @author Xavier-Alexandre Brochard
 */
public class UIPluginConfigurationServiceTest {

    /**
     * Service under test
     */
    private IUIPluginConfigurationService pluginConfigurationService;

    private IUIPluginDefinitionRepository pluginRepository;

    private ILinkUIPluginsDatasetsRepository linkedUiPluginRespository;

    private IUIPluginConfigurationRepository repository;

    private IPublisher publisher;

    /**
     * @param publisher
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        pluginRepository = Mockito.mock(IUIPluginDefinitionRepository.class);
        linkedUiPluginRespository = Mockito.mock(ILinkUIPluginsDatasetsRepository.class);
        repository = Mockito.mock(IUIPluginConfigurationRepository.class);
        publisher = Mockito.mock(IPublisher.class);

        pluginConfigurationService = new UIPluginConfigurationService(pluginRepository, linkedUiPluginRespository,
                repository, publisher);
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
    }

    /**
     * Test method for {@link fr.cnes.regards.modules.access.services.service.ui.UIPluginConfigurationService#deletePluginconfiguration(fr.cnes.regards.modules.access.services.domain.ui.UIPluginConfiguration)}.
     * @throws EntityException
     */
    @Test
    public final void testDeletePluginconfiguration_shouldRemoveAndSaveLink() throws EntityException {
        UIPluginConfiguration pluginConfiguration0 = new UIPluginConfiguration();
        UIPluginConfiguration pluginConfiguration1 = new UIPluginConfiguration();
        pluginConfiguration0.setId(4334L);
        pluginConfiguration1.setId(8484L);
        Dataset dataset = new Dataset(new Model(), "tenant", "label");
        LinkUIPluginsDatasets link = new LinkUIPluginsDatasets(dataset.getIpId().toString(),
                Lists.newArrayList(pluginConfiguration0, pluginConfiguration1));

        // Mock
        Mockito.when(repository.exists(Mockito.anyLong())).thenReturn(true);
        Mockito.when(linkedUiPluginRespository.findAllByServicesContaining(pluginConfiguration0))
                .thenReturn(Stream.of(link));

        // Perform test
        pluginConfigurationService.deletePluginconfiguration(pluginConfiguration0);

        // Verify
        Mockito.verify(linkedUiPluginRespository).save(link);
        Mockito.verify(repository).delete(pluginConfiguration0);

    }

    /**
     * Test method for {@link fr.cnes.regards.modules.access.services.service.ui.UIPluginConfigurationService#deletePluginconfiguration(fr.cnes.regards.modules.access.services.domain.ui.UIPluginConfiguration)}.
     * @throws EntityException
     */
    @Test
    public final void testDeletePluginconfiguration_shouldRemoveAndDeleteLink() throws EntityException {
        UIPluginConfiguration pluginConfiguration0 = new UIPluginConfiguration();
        pluginConfiguration0.setId(4334L);
        Dataset dataset = new Dataset(new Model(), "tenant", "label");
        LinkUIPluginsDatasets link = new LinkUIPluginsDatasets(dataset.getIpId().toString(),
                Lists.newArrayList(pluginConfiguration0));

        // Mock
        Mockito.when(repository.exists(Mockito.anyLong())).thenReturn(true);
        Mockito.when(linkedUiPluginRespository.findAllByServicesContaining(pluginConfiguration0))
                .thenReturn(Stream.of(link));

        // Perform test
        pluginConfigurationService.deletePluginconfiguration(pluginConfiguration0);

        // Verify
        Mockito.verify(linkedUiPluginRespository).delete(link);
        Mockito.verify(repository).delete(pluginConfiguration0);

    }

}
