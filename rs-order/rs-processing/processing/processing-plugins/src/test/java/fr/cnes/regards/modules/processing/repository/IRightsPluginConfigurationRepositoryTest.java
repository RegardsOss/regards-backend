package fr.cnes.regards.modules.processing.repository;

import fr.cnes.regards.framework.modules.plugins.dao.IPluginConfigurationRepository;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.modules.processing.entity.RightsPluginConfiguration;
import fr.cnes.regards.modules.processing.testutils.AbstractProcessingTest;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

public class IRightsPluginConfigurationRepositoryTest extends AbstractProcessingTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(IRightsPluginConfigurationRepositoryTest.class);

    @Autowired IRightsPluginConfigurationRepository rightsRepo;
    @Autowired IPluginConfigurationRepository pluginConfRepo;

    @Test
    public void test_crud() {
        runtimeTenantResolver.forceTenant(TENANT_PROJECTA);

        PluginConfiguration conf = new PluginConfiguration("some_label", "some_plugin_ID");
        conf.setVersion("1.0.0");
        conf.setPriorityOrder(0);
        UUID bid = UUID.randomUUID();
        conf.setBusinessId(bid.toString());
        RightsPluginConfiguration rights = new RightsPluginConfiguration(
                null,
                conf,
                bid,
                TENANT_PROJECTA,
                "EXPLOIT",
                new String[]{ randomDataset(), randomDataset() }
        );
        RightsPluginConfiguration persistedRights = rightsRepo.save(rights);

        LOGGER.info("persisted rights: {}", persistedRights);

    }

    public String randomDataset() {
        return "URN:DATASET:tenant:" + UUID.randomUUID();
    }
}