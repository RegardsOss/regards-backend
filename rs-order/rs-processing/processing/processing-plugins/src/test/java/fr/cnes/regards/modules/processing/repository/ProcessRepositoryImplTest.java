package fr.cnes.regards.modules.processing.repository;

import fr.cnes.regards.framework.jpa.multitenant.test.AbstractDaoTest;
import fr.cnes.regards.framework.modules.plugins.dao.IPluginConfigurationRepository;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.domain.PluginMetaData;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.security.endpoint.MethodAuthorizationService;
import fr.cnes.regards.framework.utils.plugins.PluginUtils;
import fr.cnes.regards.modules.processing.client.IReactiveRolesClient;
import fr.cnes.regards.modules.processing.client.IReactiveStorageClient;
import fr.cnes.regards.modules.processing.domain.PProcess;
import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.r2dbc.R2dbcAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import reactivefeign.spring.config.EnableReactiveFeignClients;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;

//@Ignore // FIXME
@ActiveProfiles(value = { "default", "test" }, inheritProfiles = false)
@TestPropertySource(
        properties = {
                "spring.application.name=ProcessRepositoryImplTest",
                "spring.jpa.properties.hibernate.default_schema=" + ProcessRepositoryImplTest.TENANT,
                "regards.jpa.multitenant.tenants[0].url=jdbc:tc:postgresql:///test",
                "regards.jpa.multitenant.tenants[0].tenant=" + ProcessRepositoryImplTest.TENANT
        }
)
@ContextConfiguration(classes = ProcessRepositoryImplTest.Config.class)
public class ProcessRepositoryImplTest extends AbstractDaoTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessRepositoryImplTest.class);

    public static final String TENANT = "test";

    public static final int ATTEMPTS = 20;

    @Autowired IPluginConfigurationRepository pluginConfRepo;
    @Autowired IPProcessRepository processRepo;

    @Test public void batch_save_then_getOne() {
        runtimeTenantResolver.forceTenant(TENANT);

        PluginConfiguration pc = new PluginConfiguration("theLabel", "thePluginId");
        pc.setVersion("1.0.0");
        pc.setPriorityOrder(0);
        pc.setBusinessId(UUID.randomUUID().toString());

        Map<String, PluginMetaData> plugins = PluginUtils.getPlugins();
        LOGGER.info("plugins: {}", plugins);

        pc.setMetaData(plugins.get("UselessProcessPlugin"));
        pluginConfRepo.save(pc);

        // TODO
        PProcess process = processRepo.findByTenantAndProcessName(TENANT, "theLabel")
                .doOnError(t -> LOGGER.error(t.getMessage(), t))
                .block();

        LOGGER.info("FOund process: {}", process);
    }

    //==================================================================================================================
    //==================================================================================================================
    // BORING STUFF ====================================================================================================
    //==================================================================================================================
    //==================================================================================================================

    @Autowired IRuntimeTenantResolver runtimeTenantResolver;

    @EnableReactiveFeignClients(basePackageClasses = {
            IReactiveStorageClient.class,
            IReactiveRolesClient.class
    })
    @EnableAutoConfiguration(exclude = { R2dbcAutoConfiguration.class })
    @ComponentScan(basePackages = "fr.cnes.regards")
    @Configuration
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

        @Bean
        public MethodAuthorizationService methodAuthorizationService() {
            return Mockito.mock(MethodAuthorizationService.class);
        }

    }

}