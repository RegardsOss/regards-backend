package fr.cnes.regards.modules.processing.testutils;

import com.google.gson.Gson;
import fr.cnes.regards.framework.feign.security.FeignSecurityManager;
import fr.cnes.regards.framework.jpa.utils.FlywayDatasourceSchemaHelper;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import io.vavr.control.Try;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;

@RunWith(SpringRunner.class)

@SpringBootTest(
    classes = TestReactiveApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
        "spring.main.web-application-type=reactive",
        "spring.http.converters.preferred-json-mapper=gson"
    }
)

@ActiveProfiles(value = { "default", "test" }, inheritProfiles = false)

@ContextConfiguration(
        initializers = { AbstractProcessingTest.Initializer.class },
        classes = { TestSpringConfiguration.class })

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)

public class AbstractProcessingTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractProcessingTest.class);

    protected static final String TENANT_PROJECTA = "PROJECTA";

    protected static final String TENANT_PROJECTB = "PROJECTB";

    protected static final String DBNAME = "testdb";

    protected static final String PGSQL_USER = "user";

    protected static final String PGSQL_SECRET = "secret";

    protected static PostgreSQLContainer<?> postgreSQLContainer = new PostgreSQLContainer<>("postgres:9.6.2")
            .withDatabaseName(DBNAME).withUsername(PGSQL_USER).withPassword(PGSQL_SECRET);

    protected static RabbitMQContainer rabbitMQContainer = new RabbitMQContainer("rabbitmq:3.6.5-management")
            .withUser("guest", "guest");

    protected static boolean onCi = onCi();
    protected static boolean onLocal = !onCi;


    @Value("${server.address}") protected String serverAddress;

    @LocalServerPort protected int port;

    @Autowired protected DataSource dataSource;

    @Autowired protected FlywayDatasourceSchemaHelper migrationHelper;

    @Autowired protected Gson gson;

    @Autowired protected FeignSecurityManager feignSecurityManager;

    @Autowired protected IRuntimeTenantResolver runtimeTenantResolver;


    @BeforeClass public static void launchContainers() {
        if (onLocal) {
            postgreSQLContainer.start();
            rabbitMQContainer.start();
        }
    }

    @PostConstruct public void setup() {
        migrationHelper.migrate(dataSource, DBNAME);
    }

    @AfterClass public static void stopContainers() {
        if (postgreSQLContainer.isRunning()) {
            postgreSQLContainer.stop();
        }
        if (rabbitMQContainer.isRunning()) {
            rabbitMQContainer.stop();
        }
    }

    public static class Initializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

        @Override public void initialize(ConfigurableApplicationContext applicationContext) {
            if (onLocal) {
                Try.run(() -> {
                    LOGGER.info("################## Creating DB for tenant {}", TENANT_PROJECTA);
                    Container.ExecResult resultA = postgreSQLContainer
                            .execInContainer("createdb", "-U", "user", "db_" + TENANT_PROJECTA);
                    LOGGER.info("################## Created DB for tenant {}: {}\n{}\n{}", TENANT_PROJECTA,
                                resultA.getExitCode(), resultA.getStdout(), resultA.getStderr());

                    LOGGER.info("################## Creating DB for tenant " + TENANT_PROJECTB);
                    Container.ExecResult resultB = postgreSQLContainer
                            .execInContainer("createdb", "-U", "user", "db_" + TENANT_PROJECTB);
                    LOGGER.info("################## Created DB for tenant {}: {}\n{}\n{}", TENANT_PROJECTB,
                                resultB.getExitCode(), resultB.getStdout(), resultB.getStderr());

                    LOGGER.info("################## Creating DB for r2dbc");
                    Container.ExecResult r2dbc = postgreSQLContainer
                            .execInContainer("createdb", "-U", "user", "r2dbcdb");
                    LOGGER.info("################## Created DB for r2dbc: {}\n{}\n{}", r2dbc.getExitCode(),
                                r2dbc.getStdout(), r2dbc.getStderr());
                }).onFailure(t -> LOGGER.error(t.getMessage(), t));
            }

            Path keyPath = Try.of(() -> Files.createTempFile("testKey_", ".tmp")).get();
            Try.run(() -> Files.write(keyPath, "746f746f746f746f".getBytes()));

            Path sharedStorage = Try.of(() -> Files.createTempDirectory("sharedStorage")).get();
            Path execWorkdir = Try.of(() -> Files.createTempDirectory("execWorkdir")).get();

            String pgHost = onCi ? "rs-postgres" : postgreSQLContainer.getContainerIpAddress();
            int pgPort = onCi ? 5432 : postgreSQLContainer.getMappedPort(PostgreSQLContainer.POSTGRESQL_PORT);
            String rabbitHost = onCi ? "rs-rabbitmq" : rabbitMQContainer.getContainerIpAddress();
            int rabbitPort = onCi ? 5672 : rabbitMQContainer.getMappedPort(5672);
            int rabbitManagementPort = onCi ? 15672 : rabbitMQContainer.getMappedPort(15672);

            TestPropertyValues.of(
                "regards.jpa.multitenant.enabled=true",
                "regards.jpa.multitenant.embedded=false",
                "regards.amqp.enabled=true",

                "debug=true",
                  //"spring.main.allow-bean-definition-overriding=true",

                "regards.test.role=USER_ROLE", "regards.test.user=user@regards.fr",
                "regards.test.tenant=" + TENANT_PROJECTA,

                "regards.processing.sharedStorage.basePath=" + sharedStorage.toFile()
                          .getAbsolutePath(),
                "regards.processing.executionWorkdir.basePath=" + execWorkdir.toFile()
                          .getAbsolutePath(),

                "regards.cipher.keyLocation=" + keyPath.toFile().getAbsolutePath(),
                "regards.cipher.iv=1234567812345678",

                "regards.test.tenant=" + TENANT_PROJECTA,
                "spring.jpa.properties.hibernate.default_schema=" + DBNAME,

                "regards.tenants=" + TENANT_PROJECTA + "," + TENANT_PROJECTB,

                "regards.jpa.multitenant.tenants[0].url=jdbc:postgresql://" + pgHost + ":" + pgPort
                          + "/db_" + TENANT_PROJECTA,
                "regards.jpa.multitenant.tenants[0].tenant=" + TENANT_PROJECTA,
                "regards.jpa.multitenant.tenants[0].driverClassName=org.postgresql.Driver",
                "regards.jpa.multitenant.tenants[0].userName=" + PGSQL_USER,
                "regards.jpa.multitenant.tenants[0].password=" + PGSQL_SECRET,

                "regards.jpa.multitenant.tenants[1].url=jdbc:postgresql://" + pgHost + ":" + pgPort
                          + "/db_" + TENANT_PROJECTB,
                "regards.jpa.multitenant.tenants[1].tenant=" + TENANT_PROJECTB,
                "regards.jpa.multitenant.tenants[1].driverClassName=org.postgresql.Driver",
                "regards.jpa.multitenant.tenants[1].userName=" + PGSQL_USER,
                "regards.jpa.multitenant.tenants[1].password=" + PGSQL_SECRET,

                "regards.processing.r2dbc.host=" + pgHost, "regards.processing.r2dbc.port=" + pgPort,
                "regards.processing.r2dbc.username=" + PGSQL_USER,
                "regards.processing.r2dbc.password=" + PGSQL_SECRET,
                "regards.processing.r2dbc.dbname=r2dbcdb", "regards.processing.r2dbc.schema=public",

                "spring.rabbitmq.host=" + rabbitHost,
                "spring.rabbitmq.port=" + rabbitPort,
                "spring.rabbitmq.username=guest", "spring.rabbitmq.password=guest",
                "regards.amqp.management.host=" + rabbitHost,
                "regards.amqp.management.port=" + rabbitManagementPort,
                "regards.amqp.microservice.typeIdentifier=rs-procesing",
                "regards.amqp.microservice.instanceIdentifier=rs-processing-" + new Random().nextInt(100_000_000),

                "jwt.secret=!!!!!==========abcdefghijklmnopqrstuvwxyz0123456789==========!!!!!",
                "cloud.config.address=localhost", "cloud.config.port=9031",
                "cloud.config.searchLocations=classpath:/regards", "cloud.registry.host=localhost",
                "cloud.registry.port=9032"
            ).applyTo(applicationContext);
        }
    }

    private static boolean onCi() {
        return checkHostPortAvailability("rs-postgres", 5432) && checkHostPortAvailability("rs-rabbitmq", 5762);
    }

    private static boolean checkHostPortAvailability(String host, int port) {
        try {
            Socket socket = new Socket();
            socket.setReuseAddress(true);
            SocketAddress socketAddress = new InetSocketAddress(host, port);
            try {
                socket.connect(socketAddress, 500);
                LOGGER.info("{}:{} available", host, port);
                return true;
            } finally {
                socket.close();
            }
        } catch (Exception e) {
            LOGGER.info("{}:{} not available", host, port);
            return false;
        }
    }

}
