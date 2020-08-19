package fr.cnes.regards.modules.processing.rest;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonIOException;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import feign.Feign;
import feign.Request;
import feign.Response;
import feign.codec.Decoder;
import feign.gson.GsonEncoder;
import fr.cnes.regards.framework.feign.TokenClientProvider;
import fr.cnes.regards.framework.feign.security.FeignSecurityManager;
import fr.cnes.regards.framework.jpa.json.GsonUtil;
import fr.cnes.regards.framework.jpa.utils.FlywayDatasourceSchemaHelper;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.test.integration.AbstractRegardsWebIT;
import fr.cnes.regards.modules.processing.domain.PExecution;
import fr.cnes.regards.modules.processing.domain.engine.IWorkloadEngine;
import fr.cnes.regards.modules.processing.domain.execution.ExecutionStatus;
import fr.cnes.regards.modules.processing.dto.PProcessDTO;
import fr.cnes.regards.modules.processing.entities.mapping.DaoCustomConverters;
import fr.cnes.regards.modules.processing.repository.IWorkloadEngineRepository;
import fr.cnes.regards.modules.processing.utils.GsonProcessingUtils;
import fr.cnes.regards.modules.storage.client.IStorageRestClient;
import io.r2dbc.spi.ConnectionFactories;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.ConnectionFactoryOptions;
import io.vavr.Tuple;
import io.vavr.collection.List;
import io.vavr.collection.Stream;
import io.vavr.gson.VavrGson;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.hibernate.cfg.AvailableSettings;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateProperties;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.r2dbc.config.AbstractR2dbcConfiguration;
import org.springframework.data.r2dbc.connectionfactory.R2dbcTransactionManager;
import org.springframework.data.r2dbc.core.DatabaseClient;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.ReactiveTransactionManager;
import org.springframework.transaction.TransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.testcontainers.containers.PostgreSQLContainer;
import reactor.core.publisher.Mono;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Properties;

import static feign.Util.ensureClosed;
import static fr.cnes.regards.modules.processing.ProcessingConstants.ContentType.APPLICATION_JSON;
import static fr.cnes.regards.modules.processing.ProcessingConstants.Path.MONITORING_EXECUTIONS_PATH;
import static fr.cnes.regards.modules.processing.domain.execution.ExecutionStatus.RUNNING;
import static fr.cnes.regards.modules.processing.testutils.RandomUtils.randomList;
import static io.r2dbc.postgresql.PostgresqlConnectionFactoryProvider.SCHEMA;
import static io.r2dbc.spi.ConnectionFactoryOptions.*;
import static java.util.Arrays.asList;

@ActiveProfiles(value = { "default", "test" }, inheritProfiles = false)
@ContextConfiguration(classes = { PMonitoringControllerTest.Config.class })
@TestPropertySource(
        properties = {
                "spring.jpa.properties.hibernate.default_schema=processing_plugins_config_tests",
                "regards.jpa.multitenant.tenants[0].url=jdbc:tc:postgresql:///ProcessPluginConfigControllerTest",
                "regards.jpa.multitenant.tenants[0].tenant=default",
                "logging.level.org.springframework.data.r2dbc=DEBUG"
        }
)
public class PMonitoringControllerTest extends AbstractRegardsWebIT {

    @Test public void executions() {

        ResponseEntity<List<PExecution>> response = client
                .executions("default", asList(RUNNING), PageRequest.of(0, 10));

        LOGGER.info("Resp: {}", response);
        LOGGER.info("Resp: {}", response.getBody());

    }


    //==================================================================================================================
    //==================================================================================================================
    //==================================================================================================================
    //==================================================================================================================

    private static final Logger LOGGER = LoggerFactory.getLogger(PMonitoringControllerTest.class);
    private static final String DBNAME = "PMonitoringControllerTest";
    @ClassRule
    public static PostgreSQLContainer<?> postgreSQLContainer = new PostgreSQLContainer<>("postgres:11.5")
            .withDatabaseName(DBNAME)
            .withUsername("user")
            .withPassword("secret");

    @Autowired DataSource dataSource;
    @Autowired FlywayDatasourceSchemaHelper migrationHelper;

    @Autowired
    private FeignSecurityManager feignSecurityManager;
    @Value("${server.address}")
    private String serverAddress;
    @Autowired
    private Gson gson;
    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;
    @Autowired
    private Client client;

    @PostConstruct
    public void setup() {
        GsonUtil.setGson(GsonProcessingUtils.gson());
        migrationHelper.migrate(dataSource, DBNAME);
    }

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

    @FeignClient(name = "rs-processing-config", contextId = "rs-processing.rest.plugin-conf.client")
    public interface Client {
        @GetMapping(
                path = MONITORING_EXECUTIONS_PATH,
                consumes = APPLICATION_JSON,
                produces = APPLICATION_JSON
        )
        ResponseEntity<io.vavr.collection.List<PExecution>> executions(
                @RequestParam String tenant,
                @RequestParam java.util.List<ExecutionStatus> status,
                Pageable page
        );
    }


    @Configuration
    @EnableTransactionManagement
    @EntityScan
    @EnableJpaRepositories
    @EnableR2dbcRepositories
    @ComponentScan
    @EnableConfigurationProperties
    @EnableFeignClients
    @ConfigurationPropertiesScan
    static class Config extends AbstractR2dbcConfiguration {

        public Gson gsonProcessing() {
            return GsonProcessingUtils.gson();
        }

        @Bean public ConnectionFactory connectionFactory() {
            return ConnectionFactories.get(ConnectionFactoryOptions.builder()
                                                   .option(DRIVER, "pool") // This is important to allow large number of parallel calls to db (pooled connections)
                                                   .option(PROTOCOL, "postgresql")
                                                   .option(HOST, postgreSQLContainer.getContainerIpAddress())
                                                   .option(PORT, postgreSQLContainer.getMappedPort(PostgreSQLContainer.POSTGRESQL_PORT))
                                                   .option(USER, "user")
                                                   .option(PASSWORD, "secret")
                                                   .option(DATABASE, DBNAME)
                                                   .option(SCHEMA, DBNAME)
                                                   .build());
        }

        @Bean public DatabaseClient databaseClient(ConnectionFactory connectionFactory) {
            return DatabaseClient.create(connectionFactory);
        }

        protected java.util.List<Object> getCustomConverters() {
            return DaoCustomConverters.getCustomConverters(gsonProcessing());
        }

        @Bean FlywayDatasourceSchemaHelper migrationHelper(Properties hibernateProperties) {
            Map<String,Object> props = Stream.ofAll(hibernateProperties.entrySet())
                    .toJavaMap(entry -> Tuple.of(entry.getKey().toString(), entry.getValue()));
            return new FlywayDatasourceSchemaHelper(props);
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

        @Bean
        public ReactiveTransactionManager reactiveTransactionManager(ConnectionFactory connectionFactory) {
            return new R2dbcTransactionManager(connectionFactory);
        }

        /*
        @Bean({"multitenantsJpaTransactionManager", "transactionManager"})
        public TransactionManager transactionManager(EntityManagerFactory entityManagerFactory) {
            JpaTransactionManager transactionManager = new JpaTransactionManager();
            transactionManager.setEntityManagerFactory(entityManagerFactory);
            return transactionManager;
        }
        */


        @Bean(name = "entityManagerFactory")
        public EntityManagerFactory entityManagerFactory(DataSource dataSource, Properties hibernateProperties) {
            LocalContainerEntityManagerFactoryBean lcemfb
                    = new LocalContainerEntityManagerFactoryBean();
            lcemfb.setDataSource(dataSource);
            lcemfb.setPackagesToScan("fr.cnes.regards.modules.processing");
            HibernateJpaVendorAdapter va = new HibernateJpaVendorAdapter();
            lcemfb.setJpaVendorAdapter(va);
            lcemfb.setJpaProperties(hibernateProperties);
            lcemfb.afterPropertiesSet();
            return lcemfb.getObject();
        }

        @Bean(name = "hibernateProperties")
        public Properties hibernateProperties() {
            Properties ps = new Properties();
            ps.put("hibernate.temp.use_jdbc_metadata_defaults", "false");
            ps.put("hibernate.dialect", "org.hibernate.dialect.PostgreSQL95Dialect");
            ps.put("hibernate.connection.characterEncoding", "UTF-8");
            ps.put("hibernate.enable_lazy_load_no_trans", "true"); // helpful to bypass session setup
            ps.put("hibernate.connection.charSet", "UTF-8");
            ps.put("hibernate.default_schema", DBNAME);
            ps.put(AvailableSettings.FORMAT_SQL, "true");
            ps.put(AvailableSettings.SHOW_SQL, "true");
            return ps;
        }

        @Bean
        public IWorkloadEngineRepository engineRepo() {
            return new IWorkloadEngineRepository() {
                @Override public Mono<IWorkloadEngine> findByName(String name) {
                    return Mono.error(new RuntimeException("Engine not found"));
                }
                @Override public Mono<IWorkloadEngine> register(IWorkloadEngine engine) {
                    return Mono.error(new RuntimeException("Cannot register engine"));
                }
            };
        }

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

        private Path tempFile;
        private final Gson gson;

        public GsonDecoder(Gson gson) {
            this.gson = gson;

            try {
                tempFile = Files.createFile(Paths.get("target/requests.log"));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public Object decode(Response response, Type type) throws IOException {
            if (response.body() == null)
                return null;
            Reader reader = response.body().asReader();
            String content = IOUtils.toString(reader);
            Request request = response.request();

            FileUtils.write(tempFile.toFile(),
                            String.format("%s %s\n---\n%s\n\n", request.httpMethod().name(), request.url(), content),
                            "UTF-8", true);
            System.err.println("################## SEE LOGS IN " + tempFile.toFile().getAbsolutePath());
            LOGGER.info("################## SEE LOGS IN {}", tempFile);
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