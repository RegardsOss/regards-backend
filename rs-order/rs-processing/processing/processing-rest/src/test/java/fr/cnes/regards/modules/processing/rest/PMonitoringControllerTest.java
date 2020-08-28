package fr.cnes.regards.modules.processing.rest;

import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import com.zaxxer.hikari.HikariDataSource;
import feign.*;
import feign.codec.Decoder;
import feign.gson.GsonEncoder;
import fr.cnes.regards.framework.feign.TokenClientProvider;
import fr.cnes.regards.framework.feign.annotation.RestClient;
import fr.cnes.regards.framework.feign.security.FeignSecurityManager;
import fr.cnes.regards.framework.jpa.json.GsonUtil;
import fr.cnes.regards.framework.jpa.utils.FlywayDatasourceSchemaHelper;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.test.integration.AbstractRegardsWebIT;
import fr.cnes.regards.modules.processing.client.IReactiveRolesClient;
import fr.cnes.regards.modules.processing.client.IReactiveStorageClient;
import fr.cnes.regards.modules.processing.config.PgSqlConfig;
import fr.cnes.regards.modules.processing.config.ProcessingDaoR2dbcConfiguration;
import fr.cnes.regards.modules.processing.domain.PExecution;
import fr.cnes.regards.modules.processing.domain.execution.ExecutionStatus;
import fr.cnes.regards.modules.processing.dto.PProcessDTO;
import fr.cnes.regards.modules.processing.repository.IPProcessRepository;
import fr.cnes.regards.modules.processing.repository.IWorkloadEngineRepository;
import fr.cnes.regards.modules.processing.utils.GsonProcessingUtils;
import io.vavr.collection.HashMap;
import io.vavr.collection.List;
import name.nkonev.r2dbc.migrate.autoconfigure.R2dbcMigrateAutoConfiguration;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.flywaydb.core.internal.jdbc.JdbcConnectionFactory;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import reactivefeign.spring.config.EnableReactiveFeignClients;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;
import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import static feign.Util.ensureClosed;
import static fr.cnes.regards.modules.processing.ProcessingConstants.Path.MONITORING_EXECUTIONS_PATH;
import static fr.cnes.regards.modules.processing.domain.execution.ExecutionStatus.PREPARE;
import static fr.cnes.regards.modules.processing.domain.execution.ExecutionStatus.RUNNING;
import static fr.cnes.regards.modules.processing.testutils.RandomUtils.randomList;
import static java.util.Arrays.asList;

@ActiveProfiles(value = { "default", "test" }, inheritProfiles = false)
@ContextConfiguration(classes = { PMonitoringControllerTest.Config.class })
@TestPropertySource(
        properties = {
                "spring.application.name=PMonitoringControllerTest",
                "spring.jpa.properties.hibernate.default_schema=test",
                "regards.jpa.multitenant.tenants[0].url=jdbc:tc:postgresql:///test",
                "regards.jpa.multitenant.tenants[0].tenant=test",
                "logging.level.org.springframework.data.r2dbc=DEBUG"
        }
)
public class PMonitoringControllerTest extends AbstractRegardsWebIT {

    @Test public void executions() {

        List<PExecution> response = client
                .executions("default", asList(RUNNING, PREPARE), toMap(PageRequest.of(0, 10)));

        // TODO: the actual test, now that doing nothin runs...

        LOGGER.info("Resp: {}", response);

    }

    private Map<String, String> toMap(Pageable page) {
        return HashMap.of(
            "page", "" + page.getPageNumber(),
            "limit", "" + page.getPageSize()
        ).toJavaMap();
    }

    //==================================================================================================================
    //==================================================================================================================
    //==================================================================================================================
    //==================================================================================================================

    private static final Logger LOGGER = LoggerFactory.getLogger(PMonitoringControllerTest.class);
    private static final String DBNAME = "test";

    @Autowired private DataSource dataSource;
    @Autowired private FlywayDatasourceSchemaHelper migrationHelper;
    @Autowired private FeignSecurityManager feignSecurityManager;
    @Autowired private IRuntimeTenantResolver runtimeTenantResolver;
    @Value("${server.address}") private String serverAddress;

    private Gson gson = GsonProcessingUtils.gson();

    private Client client;

    @PostConstruct
    public void setup() {
        GsonUtil.setGson(gson);
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

    @RestClient(name = "rs-processing-config", contextId = "rs-processing.rest.plugin-conf.client")
    @Headers({ "Accept: application/json", "Content-Type: application/json" })
    public interface Client {
        @RequestLine("GET " + MONITORING_EXECUTIONS_PATH + "?tenant={tenant}&status={status}")
        List<PExecution> executions(
                @Param("tenant") String tenant,
                @Param("status") java.util.List<ExecutionStatus> status,
                @QueryMap Map<String,String> params
        );
    }

    @EnableReactiveFeignClients(basePackageClasses = {
            IReactiveStorageClient.class,
            IReactiveRolesClient.class
    })
    @EnableAutoConfiguration(exclude = {
            R2dbcMigrateAutoConfiguration.class
    })
    @Configuration
    @Import(ProcessingDaoR2dbcConfiguration.class)
    static class Config {

        private static final String DEFAULT_USER = "test";
        private static final String DEFAULT_PASSWORD = "test";

        @Bean
        public PgSqlConfig pgSqlConfig(HikariDataSource datasource) throws URISyntaxException {
            JdbcConnectionFactory connFactory = new JdbcConnectionFactory(datasource, 1);
            String jdbcUrl = connFactory.getJdbcUrl();
            URI uri = new URI(jdbcUrl.replace("jdbc:postgresql", "http"));

            PgSqlConfig result = new PgSqlConfig(
                uri.getHost(),
                uri.getPort(),
                DBNAME,
                DBNAME,
                DEFAULT_USER,
                DEFAULT_PASSWORD
            );
            return result;
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
        public IWorkloadEngineRepository workloadEngineRepository() {
            return Mockito.mock(IWorkloadEngineRepository.class);
        }

    }


    class GsonDecoder implements Decoder {

        private Path tempFile;
        private final Gson gson;

        public GsonDecoder(Gson gson) {
            this.gson = gson;

            try {
                Path tmp = Paths.get("target/requests.log");
                Files.deleteIfExists(tmp);
                tempFile = Files.createFile(tmp);
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