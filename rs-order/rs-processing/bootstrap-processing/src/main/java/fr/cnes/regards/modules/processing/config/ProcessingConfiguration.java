package fr.cnes.regards.modules.processing.config;

import fr.cnes.regards.framework.feign.autoconfigure.FeignWebMvcConfiguration;
import fr.cnes.regards.framework.security.autoconfigure.MethodSecurityAutoConfiguration;
import fr.cnes.regards.framework.security.autoconfigure.WebSecurityAutoConfiguration;
import fr.cnes.regards.modules.processing.client.IReactiveRolesClient;
import fr.cnes.regards.modules.processing.client.IReactiveStorageClient;
import name.nkonev.r2dbc.migrate.autoconfigure.R2dbcMigrateAutoConfiguration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.web.reactive.config.EnableWebFlux;
import reactivefeign.spring.config.EnableReactiveFeignClients;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
@EnableAutoConfiguration(exclude = {
        R2dbcMigrateAutoConfiguration.class,
        FeignWebMvcConfiguration.class,
        WebSecurityAutoConfiguration.class,
        MethodSecurityAutoConfiguration.class
})
@EnableWebFlux
@EnableWebFluxSecurity
@EnableJpaRepositories
@EnableFeignClients
@EnableReactiveFeignClients(
    basePackageClasses = {
        IReactiveRolesClient.class,
        IReactiveStorageClient.class
    }
)
public class ProcessingConfiguration {

    @Value("${regards.processing.sharedStorage.basePath}")
    private String sharedStorageBasePath;
    @Value("${regards.processing.executionWorkdir.basePath}")
    private String executionWorkdirBasePath;

    @Value("${regards.processing.r2dbc.host}")
    private String r2dbcHost;
    @Value("${regards.processing.r2dbc.port}")
    private Integer r2dbcPort;
    @Value("${regards.processing.r2dbc.username}")
    private String r2dbcUsername;
    @Value("${regards.processing.r2dbc.password}")
    private String r2dbcPassword;
    @Value("${regards.processing.r2dbc.dbname}")
    private String r2dbcDbname;
    @Value("${regards.processing.r2dbc.schema}")
    private String r2dbcSchema;

    @Bean(name = "executionWorkdirParentPath")
    public Path executionWorkdirParentPath() throws IOException {
        return Files.createDirectories(Paths.get(executionWorkdirBasePath));
    }

    @Bean(name = "sharedStorageBasePath")
    public Path sharedStorageBasePath() throws IOException {
        return Files.createDirectories(Paths.get(sharedStorageBasePath));
    }

    @Bean
    public PgSqlConfig r2dbcPgSqlConfig() {
        return new PgSqlConfig(
            r2dbcHost,
            r2dbcPort,
            r2dbcDbname,
            r2dbcSchema,
            r2dbcUsername,
            r2dbcPassword
        );
    }

}
