/* Copyright 2017-2024 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.processing.testutils.servlet;

import com.google.gson.Gson;
import fr.cnes.regards.framework.amqp.autoconfigure.AmqpAutoConfiguration;
import fr.cnes.regards.framework.feign.FeignClientConfiguration;
import fr.cnes.regards.framework.gson.GsonProperties;
import fr.cnes.regards.framework.gson.autoconfigure.GsonAutoConfiguration;
import fr.cnes.regards.framework.gson.autoconfigure.GsonHttpMessageConverterCustom;
import fr.cnes.regards.framework.jpa.multitenant.autoconfigure.DataSourcesAutoConfiguration;
import fr.cnes.regards.framework.jpa.multitenant.autoconfigure.MultitenantJpaAutoConfiguration;
import fr.cnes.regards.framework.jpa.multitenant.test.AmqpTestConfiguration;
import fr.cnes.regards.framework.jpa.multitenant.test.AppDaoTestConfiguration;
import fr.cnes.regards.framework.microservice.autoconfigure.MicroserviceAutoConfiguration;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.multitenant.autoconfigure.MultitenantAutoConfiguration;
import fr.cnes.regards.framework.security.autoconfigure.test.SecureTestRuntimeTenantResolver;
import fr.cnes.regards.framework.security.endpoint.MethodAuthorizationService;
import fr.cnes.regards.framework.security.utils.jwt.JWTAuthentication;
import fr.cnes.regards.framework.test.integration.DefaultTestFeignConfiguration;
import fr.cnes.regards.modules.accessrights.client.IRolesClient;
import fr.cnes.regards.modules.accessrights.instance.client.IAccountsClient;
import fr.cnes.regards.modules.processing.domain.service.IRoleCheckerService;
import fr.cnes.regards.modules.storage.client.IStorageRestClient;
import io.r2dbc.pool.PoolingConnectionFactoryProvider;
import io.r2dbc.postgresql.PostgresqlConnectionFactoryProvider;
import io.r2dbc.spi.ConnectionFactories;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.ConnectionFactoryOptions;
import jakarta.persistence.EntityManagerFactory;
import name.nkonev.r2dbc.migrate.autoconfigure.R2dbcMigrateAutoConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

import static fr.cnes.regards.modules.processing.testutils.servlet.AbstractProcessingIT.TENANT_PROJECTA;
import static io.r2dbc.spi.ConnectionFactoryOptions.*;

/**
 * Base test configuration in servlet context.
 *
 * @author gandrieu
 */
@Configuration
@EnableAutoConfiguration(exclude = { R2dbcMigrateAutoConfiguration.class })
@EnableWebMvc
@EnableJpaRepositories
@EnableFeignClients(basePackageClasses = { IAccountsClient.class, IRolesClient.class, IStorageRestClient.class })
@ContextConfiguration(classes = { DefaultTestFeignConfiguration.class,
                                  AppDaoTestConfiguration.class,
                                  AmqpTestConfiguration.class })
@ImportAutoConfiguration({ MultitenantAutoConfiguration.class,
                           MicroserviceAutoConfiguration.class,
                           AmqpAutoConfiguration.class,
                           DataSourcesAutoConfiguration.class,
                           MultitenantJpaAutoConfiguration.class,
                           JacksonAutoConfiguration.class,
                           FeignClientConfiguration.class,
                           GsonAutoConfiguration.class, })
public class TestSpringConfiguration implements WebMvcConfigurer {

    private static final Logger LOGGER = LoggerFactory.getLogger(TestSpringConfiguration.class);

    @Value("${regards.processing.r2dbc.host:localhost}")
    public String r2dbcHost;

    @Value("${regards.processing.r2dbc.port:5433}")
    public Integer r2dbcPort;

    @Value("${regards.processing.r2dbc.username:user}")
    public String r2dbcUsername;

    @Value("${regards.processing.r2dbc.password:secret}")
    public String r2dbcPassword;

    @Value("${regards.processing.r2dbc.dbname:testdb}")
    public String r2dbcDbname;

    @Value("${regards.processing.r2dbc.schema:testschema}")
    public String r2dbcSchema;

    @Autowired
    private GsonProperties properties;

    @Autowired
    private ApplicationContext applicationContext;

    /**
     * A bean needs an EntityManagerFactory....We only have multitenantsEntityManagerFactory which isn't an
     * EntityManagerFactory so this method permits to get first one from second one.
     * Then...MultitenantJpaAutoConfiguration complains because there are 2 EntityManagerFactory instead of one...This
     * is why @Primary is set too
     */
    @Bean
    @ConditionalOnMissingBean
    @Primary
    public EntityManagerFactory entityManagerFactory(@Qualifier("multitenantsEntityManagerFactory")
                                                     LocalContainerEntityManagerFactoryBean localContainerEntityManagerFactoryBean) {
        return localContainerEntityManagerFactoryBean.getObject();
    }

    @Bean
    @ConditionalOnMissingBean
    public ConnectionFactory connectionFactory() {
        ConnectionFactoryOptions.Builder builder = ConnectionFactoryOptions.builder()
                                                                           .option(DRIVER, "pool")
                                                                           .option(PoolingConnectionFactoryProvider.ACQUIRE_RETRY,
                                                                                   5)
                                                                           .option(PoolingConnectionFactoryProvider.MAX_ACQUIRE_TIME,
                                                                                   Duration.ofSeconds(5))
                                                                           .option(PROTOCOL, "postgresql")
                                                                           .option(HOST, r2dbcHost)
                                                                           .option(PORT, r2dbcPort)
                                                                           .option(DATABASE, r2dbcDbname)
                                                                           .option(PostgresqlConnectionFactoryProvider.SCHEMA,
                                                                                   r2dbcSchema)
                                                                           .option(USER, r2dbcUsername)
                                                                           .option(PASSWORD, r2dbcPassword);
        return ConnectionFactories.get(builder.build());
    }

    @Bean(name = "executionWorkdirParentPath")
    @ConditionalOnMissingBean(value = Path.class, name = "executionWorkdirParentPath")
    @Qualifier("executionWorkdirParentPath")
    public Path executionWorkdirParentPath() {
        try {
            return Files.createTempDirectory("execWorkdir");
        } catch (IOException e) {
            throw new RuntimeException("Can not create execution workdir base directory.");
        }
    }


    /**
     *
     */

    @Value("${regards.test.role:USER_ROLE}")
    public String role;

    @Value("${regards.test.user:a@a.a}")
    public String user;

    @Value("${regards.test.tenant:PROJECTA}")
    public String tenant;

    @Bean
    @Qualifier("defaultRole")
    public String defaultRole() {
        return role;
    }

    @Bean
    @Qualifier("defaultUser")
    public String defaultUser() {
        return user;
    }

    @Bean
    @Qualifier("defaultTenant")
    public String defaultTenant() {
        return tenant;
    }

    @Autowired
    @Qualifier("gson")
    public Gson gson;

    @Autowired
    @Qualifier("prettyGson")
    public Gson prettyGson;

    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        GsonHttpMessageConverterCustom msgConverter = new GsonHttpMessageConverterCustom();
        msgConverter.setGson(gson);
        msgConverter.setPrettyGson(prettyGson);
        converters.add(msgConverter);
        LOGGER.info("Message converters: {}", converters);
    }

    @Bean
    public MethodAuthorizationService methodAuthorizationService() {
        return new MethodAuthorizationService() {

            @Override
            public Boolean hasAccess(JWTAuthentication pJWTAuthentication, Method pMethod) {
                return true;
            }
        };
    }

    @Primary
    @Bean
    public IRuntimeTenantResolver runtimeTenantResolver() {
        return new SecureTestRuntimeTenantResolver(TENANT_PROJECTA);
    }

    @Bean
    @ConditionalOnMissingBean
    public IRoleCheckerService roleCheckerService() {
        return (a, b) -> Mono.just(true);
    }

}
