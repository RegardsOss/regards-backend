package fr.cnes.regards.modules.processing.repository;

import fr.cnes.regards.framework.jpa.multitenant.test.AbstractDaoTest;
import fr.cnes.regards.framework.modules.plugins.dao.IPluginConfigurationRepository;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.modules.processing.client.IReactiveRolesClient;
import fr.cnes.regards.modules.processing.client.IReactiveStorageClient;
import fr.cnes.regards.modules.processing.entity.RightsPluginConfiguration;
import io.vavr.collection.List;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.r2dbc.R2dbcAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import reactivefeign.spring.config.EnableReactiveFeignClients;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

@ActiveProfiles(value = { "default", "test" }, inheritProfiles = false)
@TestPropertySource(
        properties = {
                "spring.jpa.properties.hibernate.default_schema=" + IRightsPluginConfigurationRepositoryTest.TENANT,
                "regards.jpa.multitenant.tenants[0].url=jdbc:tc:postgresql:///test",
                "regards.jpa.multitenant.tenants[0].tenant=" + IRightsPluginConfigurationRepositoryTest.TENANT
        }
)
@ContextConfiguration(classes = IRightsPluginConfigurationRepositoryTest.Config.class)
public class IRightsPluginConfigurationRepositoryTest extends AbstractDaoTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(IRightsPluginConfigurationRepositoryTest.class);

    public static final String TENANT = "test";

    @Autowired IRightsPluginConfigurationRepository rightsRepo;
    @Autowired IPluginConfigurationRepository pluginConfRepo;
    @Autowired IRuntimeTenantResolver runtimeTenantResolver;

    @Test
    public void test_crud() {
        runtimeTenantResolver.forceTenant(TENANT);

        PluginConfiguration conf = new PluginConfiguration("some_label", "some_plugin_ID");
        conf.setVersion("1.0.0");
        conf.setPriorityOrder(0);
        conf.setBusinessId(UUID.randomUUID().toString());

        PluginConfiguration persistedConf = pluginConfRepo.save(conf);
        RightsPluginConfiguration rights = new RightsPluginConfiguration(
                null,
                persistedConf,
                TENANT,
                "EXPLOIT",
                List.of(1L, 2L).toJavaList()
        );
        RightsPluginConfiguration persistedRights = rightsRepo.save(rights);

        LOGGER.info("persisted rights: {}", persistedRights);

    }


    @EnableReactiveFeignClients(basePackageClasses = {
            IReactiveStorageClient.class,
            IReactiveRolesClient.class
    })
    @EnableAutoConfiguration(exclude = { R2dbcAutoConfiguration.class })
    static class Config {

        @Bean
        public Path executionWorkdirParentPath() {
            try {
                return Files.createTempDirectory("execWorkdir");
            } catch (IOException e) {
                throw new RuntimeException("Can not create execution workdir base directory.");
            }
        }

        @Bean
        public Path sharedStorageBasePath() {
            try {
                return Files.createTempDirectory("sharedStorage");
            } catch (IOException e) {
                throw new RuntimeException("Can not create shared storage base directory.");
            }
        }
    }

}