package fr.cnes.regards.modules.processing.repository;

import fr.cnes.regards.framework.modules.plugins.dao.IPluginConfigurationRepository;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.modules.processing.entity.RightsPluginConfiguration;
import fr.cnes.regards.modules.processing.testutils.AbstractProcessingTest;
import io.vavr.collection.List;
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
        conf.setBusinessId(UUID.randomUUID().toString());

        RightsPluginConfiguration rights = new RightsPluginConfiguration(
                null,
                conf,
                TENANT_PROJECTA,
                "EXPLOIT",
                List.of(1L, 2L).toJavaList()
        );
        RightsPluginConfiguration persistedRights = rightsRepo.save(rights);

        LOGGER.info("persisted rights: {}", persistedRights);

    }

}