/* Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
 *
 * This file is part of REGARDS.
 *
 * REGARDS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * REGARDS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with REGARDS. If not, see <http://www.gnu.org/licenses/>.
 */
package fr.cnes.regards.modules.processing.testutils;

import com.google.gson.Gson;
import fr.cnes.regards.framework.feign.security.FeignSecurityManager;
import fr.cnes.regards.framework.jpa.utils.FlywayDatasourceSchemaHelper;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import io.vavr.collection.Stream;
import io.vavr.control.Try;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
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

import javax.sql.DataSource;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.Random;

/**
 * This class is a test base class in reactive context.
 *
 * @author gandrieu
 */
@RunWith(SpringRunner.class)

@SpringBootTest(classes = TestReactiveApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = { "spring.main.web-application-type=reactive", "spring.http.converters.preferred-json-mapper=gson" })

@ActiveProfiles(value = { "default", "test" }, inheritProfiles = false)

@ContextConfiguration(initializers = { AbstractProcessingIT.Initializer.class },
    classes = { TestSpringConfiguration.class })

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)

public abstract class AbstractProcessingIT implements InitializingBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractProcessingIT.class);

    private static final String CREATE_DB = "createdb";

    private static final String LOGGER_MSG = "################## Created DB for tenant {}: {}\n{}\n{}";

    protected static final String R2DBCDB_NAME = "r2dbcdb";

    public static final String DEFAULT_PROJECT_TENANT = "project";

    public static final String TENANT_PROJECTA = "projecta";

    public static final String TENANT_PROJECTB = "projectb";

    protected static final String PGSQL_USER = "azertyuiop123456789";

    protected static final String PGSQL_SECRET = "azertyuiop123456789";

    protected static PostgreSQLContainer<?> postgreSQLContainer = new PostgreSQLContainer<>("postgres:11.5").withDatabaseName(
        "postgres").withUsername(PGSQL_USER).withPassword(PGSQL_SECRET);

    protected static RabbitMQContainer rabbitMQContainer = new RabbitMQContainer("rabbitmq:3.6.5-management").withUser(
        "guest",
        "guest");

    protected static boolean onCi = onCi();

    protected static boolean onLocal = !onCi;

    @Value("${server.address}")
    protected String serverAddress;

    @LocalServerPort
    protected int port;

    @Autowired
    protected DataSource dataSource;

    @Autowired
    protected FlywayDatasourceSchemaHelper migrationHelper;

    @Autowired
    protected Gson gson;

    @Autowired
    protected FeignSecurityManager feignSecurityManager;

    @Autowired
    protected IRuntimeTenantResolver runtimeTenantResolver;

    @BeforeClass
    public static void launchContainers() {
        if (onLocal) {
            postgreSQLContainer.start();
            rabbitMQContainer.start();
        }
    }

    @Override
    public void afterPropertiesSet() {
        migrationHelper.migrateSchema(dataSource, R2DBCDB_NAME);
    }

    @AfterClass
    public static void stopContainers() {
        if (postgreSQLContainer.isRunning()) {
            postgreSQLContainer.stop();
        }
        if (rabbitMQContainer.isRunning()) {
            rabbitMQContainer.stop();
        }
    }

    public static class Initializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

        @Override
        public void initialize(ConfigurableApplicationContext applicationContext) {
            if (onLocal) {
                Try.run(() -> {
                    LOGGER.info("################## Creating DB for tenant {}", DEFAULT_PROJECT_TENANT);
                    Container.ExecResult result = postgreSQLContainer.execInContainer(CREATE_DB,
                                                                                      "-U",
                                                                                      PGSQL_USER,
                                                                                      DEFAULT_PROJECT_TENANT);
                    LOGGER.info(LOGGER_MSG,
                                DEFAULT_PROJECT_TENANT,
                                result.getExitCode(),
                                result.getStdout(),
                                result.getStderr());

                    LOGGER.info("################## Creating DB for tenant {}", TENANT_PROJECTA);
                    Container.ExecResult resultA = postgreSQLContainer.execInContainer(CREATE_DB,
                                                                                       "-U",
                                                                                       PGSQL_USER,
                                                                                       TENANT_PROJECTA);
                    LOGGER.info(LOGGER_MSG,
                                TENANT_PROJECTA,
                                resultA.getExitCode(),
                                resultA.getStdout(),
                                resultA.getStderr());

                    LOGGER.info("################## Creating DB for tenant " + TENANT_PROJECTB);
                    Container.ExecResult resultB = postgreSQLContainer.execInContainer(CREATE_DB,
                                                                                       "-U",
                                                                                       PGSQL_USER,
                                                                                       TENANT_PROJECTB);
                    LOGGER.info(LOGGER_MSG,
                                TENANT_PROJECTB,
                                resultB.getExitCode(),
                                resultB.getStdout(),
                                resultB.getStderr());

                    LOGGER.info("################## Creating DB for r2dbc");
                    Container.ExecResult r2dbc = postgreSQLContainer.execInContainer(CREATE_DB,
                                                                                     "-U",
                                                                                     PGSQL_USER,
                                                                                     R2DBCDB_NAME);
                    LOGGER.info("################## Created DB for r2dbc: {}\n{}\n{}",
                                r2dbc.getExitCode(),
                                r2dbc.getStdout(),
                                r2dbc.getStderr());
                }).onFailure(t -> LOGGER.error(t.getMessage(), t));
            } else {
                Try.run(() -> {
                    Connection connection = DriverManager.getConnection("jdbc:postgresql://rs-postgres:5432/postgres",
                                                                        PGSQL_USER,
                                                                        PGSQL_SECRET);

                    Stream.of(DEFAULT_PROJECT_TENANT, TENANT_PROJECTA, TENANT_PROJECTB, R2DBCDB_NAME)
                          .forEach(dbName -> {
                              try {
                                  LOGGER.info("################## Creating DB {}", dbName);
                                  Statement statement = connection.createStatement();
                                  int resultA = statement.executeUpdate("DROP DATABASE IF EXISTS "
                                                                        + dbName
                                                                        + "; "
                                                                        + "CREATE DATABASE "
                                                                        + dbName
                                                                        + ";");
                                  statement.close();
                                  LOGGER.info("################## Created DB {}: {}", dbName, resultA);
                              } catch (Exception e) {
                                  LOGGER.error("################## Error creating DB {}: {}", dbName, e.getMessage());
                              }
                          });

                    connection.close();
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

            TestPropertyValues.of("regards.jpa.multitenant.enabled=true",
                                  "regards.jpa.multitenant.embedded=false",
                                  "regards.amqp.enabled=true",

                                  "debug=true",
                                  //"spring.main.allow-bean-definition-overriding=true",

                                  "regards.test.role=USER_ROLE",
                                  "regards.test.user=user@regards.fr",
                                  "regards.test.tenant=" + TENANT_PROJECTA,

                                  "regards.processing.sharedStorage.basePath=" + sharedStorage.toFile()
                                                                                              .getAbsolutePath(),
                                  "regards.processing.executionWorkdir.basePath=" + execWorkdir.toFile()
                                                                                               .getAbsolutePath(),

                                  "regards.cipher.keyLocation=" + keyPath.toFile().getAbsolutePath(),
                                  "regards.cipher.iv=1234567812345678",

                                  "regards.tenant=" + DEFAULT_PROJECT_TENANT,
                                  "regards.tenants=" + TENANT_PROJECTA + "," + TENANT_PROJECTB,
                                  "regards.test.tenant=" + DEFAULT_PROJECT_TENANT,

                                  "spring.jpa.properties.hibernate.default_schema=" + R2DBCDB_NAME,

                                  "regards.jpa.multitenant.tenants[0].url=jdbc:postgresql://"
                                  + pgHost
                                  + ":"
                                  + pgPort
                                  + "/"
                                  + DEFAULT_PROJECT_TENANT,
                                  "regards.jpa.multitenant.tenants[0].tenant=" + DEFAULT_PROJECT_TENANT,
                                  "regards.jpa.multitenant.tenants[0].driverClassName=org.postgresql.Driver",
                                  "regards.jpa.multitenant.tenants[0].userName=" + PGSQL_USER,
                                  "regards.jpa.multitenant.tenants[0].password=" + PGSQL_SECRET,

                                  "regards.jpa.multitenant.tenants[1].url=jdbc:postgresql://"
                                  + pgHost
                                  + ":"
                                  + pgPort
                                  + "/"
                                  + TENANT_PROJECTA,
                                  "regards.jpa.multitenant.tenants[1].tenant=" + TENANT_PROJECTA,
                                  "regards.jpa.multitenant.tenants[1].driverClassName=org.postgresql.Driver",
                                  "regards.jpa.multitenant.tenants[1].userName=" + PGSQL_USER,
                                  "regards.jpa.multitenant.tenants[1].password=" + PGSQL_SECRET,

                                  "regards.jpa.multitenant.tenants[2].url=jdbc:postgresql://"
                                  + pgHost
                                  + ":"
                                  + pgPort
                                  + "/"
                                  + TENANT_PROJECTB,
                                  "regards.jpa.multitenant.tenants[2].tenant=" + TENANT_PROJECTB,
                                  "regards.jpa.multitenant.tenants[2].driverClassName=org.postgresql.Driver",
                                  "regards.jpa.multitenant.tenants[2].userName=" + PGSQL_USER,
                                  "regards.jpa.multitenant.tenants[2].password=" + PGSQL_SECRET,

                                  "regards.processing.r2dbc.host=" + pgHost,
                                  "regards.processing.r2dbc.port=" + pgPort,
                                  "regards.processing.r2dbc.username=" + PGSQL_USER,
                                  "regards.processing.r2dbc.password=" + PGSQL_SECRET,
                                  "regards.processing.r2dbc.dbname=" + R2DBCDB_NAME,
                                  "regards.processing.r2dbc.schema=public",

                                  "spring.rabbitmq.host=" + rabbitHost,
                                  "spring.rabbitmq.port=" + rabbitPort,
                                  "spring.rabbitmq.username=guest",
                                  "spring.rabbitmq.password=guest",
                                  "regards.amqp.management.host=" + rabbitHost,
                                  "regards.amqp.management.port=" + rabbitManagementPort,
                                  "regards.amqp.microservice.typeIdentifier=rs-procesing",
                                  "regards.amqp.microservice.instanceIdentifier=rs-processing-" + new Random().nextInt(
                                      100_000_000),

                                  "jwt.secret=!!!!!==========abcdefghijklmnopqrstuvwxyz0123456789==========!!!!!",
                                  "cloud.config.address=localhost",
                                  "cloud.config.port=9031",
                                  "cloud.config.searchLocations=classpath:/regards",
                                  "cloud.registry.host=localhost",
                                  "cloud.registry.port=9032").applyTo(applicationContext);
        }
    }

    private static boolean onCi() {
        return checkSocketHostPortAvailability("rs-postgres", 5432)
            //    && checkHttpHostPortAvailability("rs-rabbitmq", 15762)
            ;
    }

    private static boolean checkSocketHostPortAvailability(String host, int port) {
        try (Socket socket = new Socket()) {
            socket.setReuseAddress(true);
            SocketAddress socketAddress = new InetSocketAddress(host, port);
            try {
                socket.connect(socketAddress, 2000);
                LOGGER.info("{}:{} available", host, port);
                return true;
            } finally {
                socket.close();
            }
        } catch (IOException | RuntimeException e) {
            LOGGER.info("{}:{} not available", host, port, e);
            return false;
        }
    }

    private static boolean checkHttpHostPortAvailability(String host, int port) {
        try {
            new URL("http://" + host + ":" + port).openStream();
            return true;
        } catch (IOException | RuntimeException e) {
            LOGGER.info("http://{}:{} not available", host, port, e);
            return false;
        }
    }

}
