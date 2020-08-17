package fr.cnes.regards.modules.processing.controller;

import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import feign.Feign;
import feign.Headers;
import feign.RequestLine;
import feign.Response;
import feign.codec.Decoder;
import fr.cnes.regards.framework.feign.TokenClientProvider;
import fr.cnes.regards.framework.feign.annotation.RestClient;
import fr.cnes.regards.framework.feign.security.FeignSecurityManager;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.domain.PluginMetaData;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.test.integration.AbstractRegardsWebIT;
import fr.cnes.regards.modules.processing.dto.PProcessDTO;
import io.vavr.collection.List;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.r2dbc.R2dbcAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.testcontainers.containers.PostgreSQLContainer;
import reactor.core.publisher.Mono;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;

import static feign.Util.ensureClosed;
import static fr.cnes.regards.modules.processing.ProcessingConstants.Path.PROCESS_CONFIG_INSTANCES_PATH;
import static fr.cnes.regards.modules.processing.ProcessingConstants.Path.PROCESS_CONFIG_METADATA_PATH;
import static fr.cnes.regards.modules.processing.testutils.RandomUtils.randomList;
import static org.springframework.context.annotation.FilterType.ASSIGNABLE_TYPE;

@ActiveProfiles(value = { "default", "test" }, inheritProfiles = false)
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=processing_plugins_config_tests" })
@ContextConfiguration(classes = ProcessPluginConfigControllerTest.Config.class)
public class ProcessPluginConfigControllerTest extends AbstractRegardsWebIT {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessPluginConfigControllerTest.class);


    private Client client;

    @Test
    public void test_list_metadata() {
        client.listAllDetectedPlugins()
            .forEach(md -> LOGGER.info("Found md {}: {}", md.getPluginId(), md));
    }

    //==================================================================================================================
    //==================================================================================================================
    //==================================================================================================================
    //==================================================================================================================

    private static final String DBNAME = ProcessPluginConfigControllerTest.class.getSimpleName().toLowerCase();

    @ClassRule
    public static PostgreSQLContainer<?> postgreSQLContainer = new PostgreSQLContainer<>("postgres:11.5")
            .withDatabaseName(DBNAME)
            .withUsername("user")
            .withPassword("secret");

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
        runtimeTenantResolver.forceTenant(getDefaultTenant());
        client = Feign.builder()
            .decoder(new GsonDecoder(gson))
            .target(new TokenClientProvider<>(Client.class, "http://" + serverAddress + ":" + getPort(), feignSecurityManager));
        runtimeTenantResolver.forceTenant(getDefaultTenant());
        FeignSecurityManager.asSystem();
    }

    interface Values {
        List<PProcessDTO> processes = randomList(PProcessDTO.class, 20);
    }

    @RestClient(name = "rs-processing-config", contextId = "rs-processing.rest.plugin-conf.client")
    @Headers({ "Accept: application/json", "Content-Type: application/json" })
    public interface Client {

        @RequestLine("GET " + PROCESS_CONFIG_METADATA_PATH)
        java.util.List<PluginMetaData> listAllDetectedPlugins();

        @RequestLine("GET " + PROCESS_CONFIG_INSTANCES_PATH)
        java.util.List<PluginConfiguration> listAllPluginConfigurations();

        @RequestLine("POST " + PROCESS_CONFIG_INSTANCES_PATH)
        PluginConfiguration create(PluginConfiguration config);

        @RequestLine("PUT " + PROCESS_CONFIG_INSTANCES_PATH)
        PluginConfiguration update(Mono<PluginConfiguration> config);

    }

    @Configuration
    @EnableTransactionManagement
    @EnableAutoConfiguration(exclude = { R2dbcAutoConfiguration.class })
    @EnableJpaRepositories(excludeFilters = {
            @ComponentScan.Filter(type = ASSIGNABLE_TYPE, classes = { ReactiveCrudRepository.class })
    })
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
        public DataSource dataSource() {
            HikariConfig hk = new HikariConfig();
            hk.setJdbcUrl("jdbc:postgresql://" + postgreSQLContainer.getContainerIpAddress() +
                                  ":" + postgreSQLContainer.getMappedPort(PostgreSQLContainer.POSTGRESQL_PORT) + "/" +
                                  postgreSQLContainer.getDatabaseName());

            hk.setDriverClassName(org.postgresql.Driver.class.getCanonicalName());
            hk.setUsername(postgreSQLContainer.getUsername());
            hk.setPassword(postgreSQLContainer.getPassword());

            return new DockerizedDataSource(postgreSQLContainer, hk);
        }

        public class DockerizedDataSource extends HikariDataSource implements DisposableBean {
            private PostgreSQLContainer<?> container;
            public DockerizedDataSource(PostgreSQLContainer<?> container, HikariConfig config) {
                super(config);
                this.container = container;
            }
            @Override
            public void destroy() throws Exception {
                if (container != null && container.isRunning()) {
                    container.stop();
                }
            }
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
            LOGGER.info("Body content: \n>>>\n{}\n<<<", content);
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