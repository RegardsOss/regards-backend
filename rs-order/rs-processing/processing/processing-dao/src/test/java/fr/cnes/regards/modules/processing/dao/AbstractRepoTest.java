package fr.cnes.regards.modules.processing.dao;

import fr.cnes.regards.modules.processing.config.PgSqlConfig;
import fr.cnes.regards.modules.processing.config.R2dbcSpringConfiguration;
import fr.cnes.regards.modules.processing.repository.IPBatchRepository;
import fr.cnes.regards.modules.processing.repository.IPExecutionRepository;
import fr.cnes.regards.modules.processing.testutils.RandomUtils;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.testcontainers.containers.PostgreSQLContainer;

import static org.testcontainers.containers.PostgreSQLContainer.POSTGRESQL_PORT;

@RunWith(SpringRunner.class)
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "logging.level.org.springframework.data.r2dbc=DEBUG"
})
@ContextConfiguration(classes = AbstractRepoTest.Config.class)
public abstract class AbstractRepoTest implements RandomUtils {

    @Autowired protected IPBatchRepository domainBatchRepo;
    @Autowired protected IPExecutionRepository domainExecRepo;
    @Autowired protected IBatchEntityRepository entityBatchRepo;
    @Autowired protected IExecutionEntityRepository entityExecRepo;

    //==================================================================================================================
    //==================================================================================================================
    // BORING STUFF ====================================================================================================
    //==================================================================================================================
    //==================================================================================================================

    private static final String DBNAME = PBatchRepositoryImplTest.class.getSimpleName().toLowerCase();
    private static final String USER = "user";
    private static final String SECRET = "secret";

    @ClassRule
    public static PostgreSQLContainer<?> postgreSQLContainer = new PostgreSQLContainer<>("postgres:11.5")
            .withDatabaseName(DBNAME)
            .withUsername(USER)
            .withPassword(SECRET);

    @Configuration
    @EnableTransactionManagement
    @EntityScan("fr.cnes.regards.modules.processing.entities")
    @Import({ R2dbcSpringConfiguration.class })
    static class Config {
        @Bean
        public PgSqlConfig pgSqlConfig() {
            return new PgSqlConfig(
                    "localhost",
                    postgreSQLContainer.getMappedPort(POSTGRESQL_PORT),
                    DBNAME,
                    "public",
                    USER,
                    SECRET
            );
        }

    }

}
