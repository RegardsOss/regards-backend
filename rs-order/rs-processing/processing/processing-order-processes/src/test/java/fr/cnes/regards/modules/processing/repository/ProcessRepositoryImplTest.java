package fr.cnes.regards.modules.processing.repository;

import fr.cnes.regards.framework.modules.plugins.dao.IPluginConfigurationRepository;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.domain.PluginMetaData;
import fr.cnes.regards.framework.utils.plugins.PluginUtils;
import fr.cnes.regards.modules.accessrights.client.IRolesClient;
import fr.cnes.regards.modules.processing.domain.PProcess;
import fr.cnes.regards.modules.processing.domain.repository.IPProcessRepository;
import fr.cnes.regards.modules.processing.testutils.AbstractProcessingTest;
import fr.cnes.regards.modules.storage.client.IStorageRestClient;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.openfeign.EnableFeignClients;

import java.util.Map;
import java.util.UUID;

@EnableFeignClients(basePackageClasses = { IRolesClient.class, IStorageRestClient.class })
public class ProcessRepositoryImplTest extends AbstractProcessingTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessRepositoryImplTest.class);

    public static final int ATTEMPTS = 20;

    @Autowired IPluginConfigurationRepository pluginConfRepo;
    @Autowired IPProcessRepository processRepo;

    @Test public void batch_save_then_getOne() {
        runtimeTenantResolver.forceTenant(TENANT_PROJECTA);

        PluginConfiguration pc = new PluginConfiguration("theLabel", "thePluginId");
        pc.setVersion("1.0.0");
        pc.setPriorityOrder(0);
        pc.setBusinessId(UUID.randomUUID().toString());

        Map<String, PluginMetaData> plugins = PluginUtils.getPlugins();
        LOGGER.info("plugins: {}", plugins);

        pc.setMetaData(plugins.get("UselessProcessPlugin"));
        pluginConfRepo.save(pc);

        // TODO
        PProcess process = processRepo.findByTenantAndProcessName(TENANT_PROJECTA, "theLabel")
                .doOnError(t -> LOGGER.error(t.getMessage(), t))
                .block();

        LOGGER.info("FOund process: {}", process);
    }

}