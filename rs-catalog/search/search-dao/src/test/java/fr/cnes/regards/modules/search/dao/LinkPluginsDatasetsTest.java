/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.search.dao;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

import com.google.common.collect.Sets;

import fr.cnes.regards.framework.jpa.multitenant.test.AbstractDaoTransactionalTest;
import fr.cnes.regards.framework.modules.plugins.dao.IPluginConfigurationRepository;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.modules.search.domain.IService;
import fr.cnes.regards.modules.search.domain.LinkPluginsDatasets;

/**
 * @author Sylvain Vissiere-Guerinet
 *
 */
@TestPropertySource(locations = "classpath:tests.properties")
public class LinkPluginsDatasetsTest extends AbstractDaoTransactionalTest {

    @Autowired
    private ILinkPluginsDatasetsRepository linkRepo;

    @Autowired
    private IPluginConfigurationRepository pluginRepo;

    @Test
    public void testMapping() {
        LinkPluginsDatasets link1 = new LinkPluginsDatasets(1L, Sets.newHashSet());
        linkRepo.save(link1);
        linkRepo.findOne(link1.getDatasetId());

        PluginConfiguration confService = new PluginConfiguration();
        confService.setPluginId("pPluginId");
        confService.setVersion("pVersion");
        confService.setPriorityOrder(0);
        confService.setInterfaceName(IService.class.getName());
        confService = pluginRepo.save(confService);

        LinkPluginsDatasets link2 = new LinkPluginsDatasets(2L, Sets.newHashSet(confService));
        linkRepo.save(link2);
    }

}
