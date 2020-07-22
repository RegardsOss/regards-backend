package fr.cnes.regards.modules.processing.dao;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import fr.cnes.regards.framework.jpa.json.GsonUtil;
import fr.cnes.regards.framework.jpa.utils.FlywayDatasourceSchemaHelper;
import fr.cnes.regards.modules.processing.repository.IPBatchRepository;
import fr.cnes.regards.modules.processing.repository.IPExecutionRepository;
import fr.cnes.regards.modules.processing.entities.mapping.DaoCustomConverters;
import fr.cnes.regards.modules.processing.repository.IPExecutionStepRepository;
import fr.cnes.regards.modules.processing.testutils.RandomUtils;
import io.r2dbc.spi.ConnectionFactories;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.ConnectionFactoryOptions;
import io.vavr.Tuple;
import io.vavr.collection.Stream;
import io.vavr.gson.VavrGson;
import org.hibernate.cfg.AvailableSettings;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.r2dbc.config.AbstractR2dbcConfiguration;
import org.springframework.data.r2dbc.connectionfactory.R2dbcTransactionManager;
import org.springframework.data.r2dbc.core.DatabaseClient;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.ReactiveTransactionManager;
import org.springframework.transaction.TransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.testcontainers.containers.PostgreSQLContainer;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static io.r2dbc.postgresql.PostgresqlConnectionFactoryProvider.SCHEMA;
import static io.r2dbc.spi.ConnectionFactoryOptions.*;
import static org.springframework.context.annotation.FilterType.ASSIGNABLE_TYPE;

@RunWith(SpringRunner.class)
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "logging.level.org.springframework.data.r2dbc=DEBUG"
})
@ContextConfiguration(classes = AbstractRepoTest.Config.class)
public abstract class AbstractRepoTest implements RandomUtils {


    @Autowired protected IPBatchRepository domainBatchRepo;
    @Autowired protected IPExecutionRepository domainExecRepo;
    @Autowired protected IPExecutionStepRepository domainStepRepo;
    @Autowired protected IBatchEntityRepository entityBatchRepo;
    @Autowired protected IExecutionEntityRepository entityExecRepo;
    @Autowired protected IExecutionStepEntityRepository entityStepRepo;

    //==================================================================================================================
    //==================================================================================================================
    // BORING STUFF ====================================================================================================
    //==================================================================================================================
    //==================================================================================================================

    private static final String DBNAME = PBatchRepositoryImplTest.class.getSimpleName().toLowerCase();

    @ClassRule
    public static PostgreSQLContainer<?> postgreSQLContainer = new PostgreSQLContainer<>("postgres:11.5")
            .withDatabaseName(DBNAME)
            .withUsername("user")
            .withPassword("secret");

    @Autowired DataSource dataSource;
    @Autowired FlywayDatasourceSchemaHelper migrationHelper;
    @PostConstruct
    public void setup() {
        GsonUtil.setGson(new Gson());
        migrationHelper.migrate(dataSource, DBNAME);
    }


    @Configuration
    @EnableTransactionManagement
    @EntityScan("fr.cnes.regards.modules.processing.entities")
    @ComponentScan({ "fr.cnes.regards.modules.processing.entities", "fr.cnes.regards.modules.processing.dao" })
    @EnableJpaRepositories(excludeFilters = {
            @ComponentScan.Filter(type = ASSIGNABLE_TYPE, classes = { ReactiveCrudRepository.class })
    })
    @EnableR2dbcRepositories(includeFilters = {
            @ComponentScan.Filter(type = ASSIGNABLE_TYPE, classes = { ReactiveCrudRepository.class })
    })
    static class Config extends AbstractR2dbcConfiguration {

        @Bean
        public Gson gson() {
            GsonBuilder builder = new GsonBuilder();
            VavrGson.registerAll(builder);
            return builder.create();
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

        protected List<Object> getCustomConverters() {
            return DaoCustomConverters.getCustomConverters(gson());
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

        @Bean({"multitenantsJpaTransactionManager", "transactionManager"})
        public TransactionManager transactionManager(EntityManagerFactory entityManagerFactory) {
            JpaTransactionManager transactionManager = new JpaTransactionManager();
            transactionManager.setEntityManagerFactory(entityManagerFactory);
            return transactionManager;
        }


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

    }

}
