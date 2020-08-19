package fr.cnes.regards.modules.processing.controller;

import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import feign.*;
import feign.codec.Decoder;
import feign.gson.GsonEncoder;
import fr.cnes.regards.framework.feign.TokenClientProvider;
import fr.cnes.regards.framework.feign.annotation.RestClient;
import fr.cnes.regards.framework.feign.security.FeignSecurityManager;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.domain.PluginMetaData;
import fr.cnes.regards.framework.modules.plugins.domain.parameter.IPluginParam;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.test.integration.AbstractRegardsWebIT;
import fr.cnes.regards.modules.processing.dto.PProcessDTO;
import fr.cnes.regards.modules.storage.client.IStorageRestClient;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.r2dbc.R2dbcAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static feign.Util.ensureClosed;
import static fr.cnes.regards.modules.processing.ProcessingConstants.Path.PROCESS_CONFIG_INSTANCES_PATH;
import static fr.cnes.regards.modules.processing.ProcessingConstants.Path.PROCESS_CONFIG_METADATA_PATH;
import static fr.cnes.regards.modules.processing.testutils.RandomUtils.randomList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.context.annotation.FilterType.ASSIGNABLE_TYPE;

@ActiveProfiles(value = { "default", "test" }, inheritProfiles = false)
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=processing_plugins_config_tests" })
@ContextConfiguration(classes = { ProcessPluginConfigControllerTest.Config.class })
@TestPropertySource(
        properties = {
                "regards.jpa.multitenant.tenants[0].url=jdbc:tc:postgresql:///ProcessPluginConfigControllerTest",
                "regards.jpa.multitenant.tenants[0].tenant=default"
        }
)
public class ProcessPluginConfigControllerTest extends AbstractRegardsWebIT {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessPluginConfigControllerTest.class);

    private Client client;

    @Test
    public void test_list_metadata() {
        runtimeTenantResolver.forceTenant(getDefaultTenant());

        // LIST AVAILABLE PLUGINS
        List<PluginMetaData> pluginMetaData = client.listAllDetectedPlugins();

        pluginMetaData
                .forEach(md -> LOGGER.info("Found md {}: {}", md.getPluginId(), md));

        assertThat(pluginMetaData).hasSize(2);
        assertThat(pluginMetaData).anyMatch(md -> md.getPluginClassName().equals(UselessProcessPlugin.class.getName()));

        // LIST AVAILABLE CONFIGURATIONS: NOTHING YET...
        List<PluginConfiguration> pluginConfigs = client.listAllPluginConfigurations();
        pluginConfigs.forEach(pc -> LOGGER.info("Found pc {}: {}", pc.getPluginId(), pc));
        assertThat(pluginConfigs).hasSize(0);

        // CREATE A CONFIG
        PluginConfiguration useless1Config = new PluginConfiguration("useless1 label", UselessProcessPlugin.class.getSimpleName());
        useless1Config.setVersion("1.0");
        useless1Config.setPriorityOrder(1);
        useless1Config.setParameters(IPluginParam.set(
                IPluginParam.build("processName", "useless-processName-1")
        ));
        client.create(useless1Config);

        // LIST AGAIN: THERE IS ONE CONFIG!
        List<PluginConfiguration> pluginConfigsWithUseless1 = client.listAllPluginConfigurations();
        pluginConfigsWithUseless1.forEach(pc -> LOGGER.info("Found pc {}: {}", pc.getPluginId(), pc));
        assertThat(pluginConfigsWithUseless1).hasSize(1);
        assertThat(pluginConfigsWithUseless1).anyMatch(pc -> pc.getParameter("processName").getValue().equals("useless-processName-1"));

        // UPDATE THE CONFIG
        PluginConfiguration useless1ConfigPersisted = pluginConfigsWithUseless1.get(0);
        useless1ConfigPersisted.setParameters(IPluginParam.set(
                IPluginParam.build("processName", "useless-processName-2")
        ));
        client.update(useless1ConfigPersisted);

        // LIST AGAIN: THERE IS ONE CONFIG!
        List<PluginConfiguration> pluginConfigsWithUseless2 = client.listAllPluginConfigurations();
        pluginConfigsWithUseless2.forEach(pc -> LOGGER.info("Found pc {}: {}", pc.getPluginId(), pc));
        assertThat(pluginConfigsWithUseless2).hasSize(1);
        assertThat(pluginConfigsWithUseless2).anyMatch(pc -> pc.getParameter("processName").getValue().equals("useless-processName-2"));

        // NOW DELETE IT
        client.delete(pluginConfigsWithUseless2.get(0).getId());

        // LIST AVAILABLE CONFIGURATIONS: NOTHING ANYMORE...
        List<PluginConfiguration> pluginConfigsFinal = client.listAllPluginConfigurations();
        pluginConfigsFinal.forEach(pc -> LOGGER.info("Found pc {}: {}", pc.getPluginId(), pc));
        assertThat(pluginConfigsFinal).hasSize(0);

    }

    //==================================================================================================================
    //==================================================================================================================
    //==================================================================================================================
    //==================================================================================================================

    private static final String DBNAME = "ProcessPluginConfigControllerTest";

    @Autowired
    private FeignSecurityManager feignSecurityManager;
    @Value("${server.address}")
    private String serverAddress;
    @Autowired
    private Gson gson;
    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    @Before
    public void init() throws IOException, ModuleException {
        client = Feign.builder()
            .decoder(new GsonDecoder(gson))
            .encoder(new GsonEncoder(gson))
            .target(new TokenClientProvider<>(Client.class, "http://" + serverAddress + ":" + getPort(), feignSecurityManager));
        runtimeTenantResolver.forceTenant(getDefaultTenant());
        FeignSecurityManager.asSystem();
    }

    interface Values {
        io.vavr.collection.List<PProcessDTO> processes = randomList(PProcessDTO.class, 20);
    }

    @RestClient(name = "rs-processing-config", contextId = "rs-processing.rest.plugin-conf.client")
    @Headers({ "Accept: application/json", "Content-Type: application/json" })
    public interface Client {
        @RequestLine("GET " + PROCESS_CONFIG_METADATA_PATH)
        List<PluginMetaData> listAllDetectedPlugins();

        @RequestLine("GET " + PROCESS_CONFIG_INSTANCES_PATH)
        List<PluginConfiguration> listAllPluginConfigurations();

        @RequestLine("POST " + PROCESS_CONFIG_INSTANCES_PATH)
        PluginConfiguration create(PluginConfiguration config);

        @RequestLine("PUT " + PROCESS_CONFIG_INSTANCES_PATH)
        PluginConfiguration update(PluginConfiguration config);

        @RequestLine("DELETE " + PROCESS_CONFIG_INSTANCES_PATH + "/{id}")
        void delete(@Param("id") Long id);
    }

    @Configuration
    @EnableTransactionManagement
    @EnableAutoConfiguration(exclude = {
            R2dbcAutoConfiguration.class
    })
    @EnableJpaRepositories(excludeFilters = {
            @ComponentScan.Filter(type = ASSIGNABLE_TYPE, classes = { ReactiveCrudRepository.class })
    })
    @EnableConfigurationProperties
    @ConfigurationPropertiesScan
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
        public IStorageRestClient storageRestClient() {
            return Mockito.mock(IStorageRestClient.class);
        }

    }

    class GsonDecoder implements Decoder {

        private final Gson gson;

        public GsonDecoder(Gson gson) {
            this.gson = gson;
        }

        @Override
        public Object decode(Response response, Type type) throws IOException {
            if (response.body() == null)
                return null;
            Reader reader = response.body().asReader();
            String content = IOUtils.toString(reader);
            Request request = response.request();
            LOGGER.error("{} {}\n>>>\n{}\n<<<", request.httpMethod().name(), request.url(), content);
            try {
                return gson.fromJson(content, type);
            } catch (JsonIOException e) {
                if (e.getCause() != null && e.getCause() instanceof IOException) {
                    throw IOException.class.cast(e.getCause());
                }
                throw e;
            } finally {
                ensureClosed(reader);
            }
        }
    }

}