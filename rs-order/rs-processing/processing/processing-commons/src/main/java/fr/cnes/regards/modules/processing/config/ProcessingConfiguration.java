package fr.cnes.regards.modules.processing.config;

import com.google.gson.GsonBuilder;
import fr.cnes.regards.framework.feign.autoconfigure.FeignWebMvcConfiguration;
import fr.cnes.regards.framework.gson.GsonBuilderFactory;
import fr.cnes.regards.framework.gson.GsonCustomizer;
import fr.cnes.regards.framework.gson.GsonProperties;
import fr.cnes.regards.framework.security.autoconfigure.MethodSecurityAutoConfiguration;
import fr.cnes.regards.framework.security.autoconfigure.WebSecurityAutoConfiguration;
import io.vavr.gson.VavrGson;
import name.nkonev.r2dbc.migrate.autoconfigure.R2dbcMigrateAutoConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springdoc.core.SpringDocUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.web.reactive.config.EnableWebFlux;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

@Configuration
@EnableAutoConfiguration(exclude = {
        R2dbcMigrateAutoConfiguration.class,
        WebMvcAutoConfiguration.class,
        FeignWebMvcConfiguration.class,
        WebSecurityAutoConfiguration.class,
        MethodSecurityAutoConfiguration.class
})
@EnableWebFlux
@EnableWebFluxSecurity
@EnableJpaRepositories
@EnableFeignClients
public class ProcessingConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessingConfiguration.class);

    static {
        SpringDocUtils.getConfig()
                .replaceWithClass(io.vavr.collection.Set.class, java.util.Set.class)
                .replaceWithClass(io.vavr.collection.Seq.class, java.util.List.class)
                .replaceWithClass(io.vavr.collection.List.class, java.util.List.class)
                .replaceWithClass(io.vavr.collection.Map.class, java.util.Map.class);
    }

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

    @Autowired private GsonProperties properties;
    @Autowired private ApplicationContext applicationContext;


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

    @Bean
    public GsonBuilderFactory gsonBuilderFactory() {
        return new GsonBuilderFactory(properties, applicationContext){
            @Override public GsonBuilder newBuilder() {
                GsonBuilder builder = GsonCustomizer.gsonBuilder(
                    Optional.ofNullable(properties),
                    Optional.ofNullable(applicationContext)
                );
                VavrGson.registerAll(builder);
                return builder;
            }
        };
    }

}
