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
import com.google.gson.GsonBuilder;
import fr.cnes.regards.framework.amqp.autoconfigure.AmqpAutoConfiguration;
import fr.cnes.regards.framework.authentication.autoconfigure.Oauth2AutoConfiguration;
import fr.cnes.regards.framework.feign.autoconfigure.FeignWebMvcConfiguration;
import fr.cnes.regards.framework.gson.GsonBuilderFactory;
import fr.cnes.regards.framework.gson.GsonCustomizer;
import fr.cnes.regards.framework.gson.GsonProperties;
import fr.cnes.regards.framework.jpa.multitenant.autoconfigure.DataSourcesAutoConfiguration;
import fr.cnes.regards.framework.jpa.multitenant.autoconfigure.MultitenantJpaAutoConfiguration;
import fr.cnes.regards.framework.microservice.autoconfigure.MicroserviceAutoConfiguration;
import fr.cnes.regards.framework.multitenant.autoconfigure.MultitenantAutoConfiguration;
import fr.cnes.regards.framework.security.autoconfigure.MethodSecurityAutoConfiguration;
import fr.cnes.regards.framework.security.autoconfigure.WebSecurityAutoConfiguration;
import fr.cnes.regards.framework.security.endpoint.MethodAuthorizationService;
import fr.cnes.regards.framework.security.endpoint.voter.ResourceAccessVoter;
import fr.cnes.regards.framework.security.utils.jwt.JWTAuthentication;
import fr.cnes.regards.framework.security.utils.jwt.JWTService;
import fr.cnes.regards.framework.security.utils.jwt.exception.JwtException;
import fr.cnes.regards.modules.processing.domain.service.IRoleCheckerService;
import fr.cnes.regards.modules.processing.utils.gson.GsonInefficientHttpMessageCodec;
import fr.cnes.regards.modules.processing.utils.gson.TypedGsonTypeAdapter;
import io.r2dbc.spi.ConnectionFactories;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.ConnectionFactoryOptions;
import io.vavr.gson.VavrGson;
import name.nkonev.r2dbc.migrate.autoconfigure.R2dbcMigrateAutoConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.access.AccessDecisionManager;
import org.springframework.security.access.AccessDecisionVoter;
import org.springframework.security.access.vote.AffirmativeBased;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.context.ServerSecurityContextRepository;
import org.springframework.web.reactive.config.EnableWebFlux;
import org.springframework.web.reactive.config.WebFluxConfigurer;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;

import static io.r2dbc.pool.PoolingConnectionFactoryProvider.ACQUIRE_RETRY;
import static io.r2dbc.pool.PoolingConnectionFactoryProvider.MAX_ACQUIRE_TIME;
import static io.r2dbc.postgresql.PostgresqlConnectionFactoryProvider.SCHEMA;
import static io.r2dbc.spi.ConnectionFactoryOptions.*;

/**
 * Base test configuration in reactive context.
 *
 * @author gandrieu
 */
@Configuration
@EnableAutoConfiguration(
    exclude = { R2dbcMigrateAutoConfiguration.class, WebSecurityAutoConfiguration.class, WebMvcAutoConfiguration.class,
        FeignWebMvcConfiguration.class, MethodSecurityAutoConfiguration.class, Oauth2AutoConfiguration.class, })
@EnableWebFlux
@EnableWebFluxSecurity
@EnableJpaRepositories
@EnableFeignClients
@Import({ MultitenantAutoConfiguration.class, MicroserviceAutoConfiguration.class, AmqpAutoConfiguration.class,
    DataSourcesAutoConfiguration.class, MultitenantJpaAutoConfiguration.class, JacksonAutoConfiguration.class })
public class TestSpringConfiguration implements WebFluxConfigurer {

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

    @Bean
    @ConditionalOnMissingBean
    public ConnectionFactory connectionFactory() {
        ConnectionFactoryOptions.Builder builder = builder().option(DRIVER, "pool")
                                                            .option(ACQUIRE_RETRY, 5)
                                                            .option(MAX_ACQUIRE_TIME, Duration.ofSeconds(5))
                                                            .option(PROTOCOL, "postgresql")
                                                            .option(HOST, r2dbcHost)
                                                            .option(PORT, r2dbcPort)
                                                            .option(DATABASE, r2dbcDbname)
                                                            .option(SCHEMA, r2dbcSchema)
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
            throw new RuntimeException("Can not create execution workdir base directory.", e);
        }
    }

    @Bean
    @ConditionalOnMissingBean
    public GsonBuilderFactory gsonBuilderFactory() {
        return new GsonBuilderFactory(properties, applicationContext) {

            @Override
            public GsonBuilder newBuilder() {
                GsonBuilder builder = GsonCustomizer.gsonBuilder(Optional.ofNullable(properties),
                                                                 Optional.ofNullable(applicationContext));
                ServiceLoader<TypedGsonTypeAdapter> loader = ServiceLoader.load(TypedGsonTypeAdapter.class);
                loader.iterator().forEachRemaining(tr -> {
                    builder.registerTypeAdapter(tr.type(), tr.serializer());
                    builder.registerTypeAdapter(tr.type(), tr.deserializer());
                });
                VavrGson.registerAll(builder);
                return builder.setPrettyPrinting();
            }
        };
    }

    @Bean
    @ConditionalOnMissingBean
    public ReactiveAuthenticationManager authenticationManager(JWTService jwtService) {
        return auth -> {
            if (auth instanceof JWTAuthentication) {
                return Mono.fromCallable(() -> jwtService.parseToken((JWTAuthentication) auth));
            } else {
                return Mono.empty();
            }
        };
    }

    @Bean
    @ConditionalOnMissingBean
    public ResourceAccessVoter resourceAccessVoter() {
        return new ResourceAccessVoter(new MethodAuthorizationService());
    }

    @Bean
    @ConditionalOnMissingBean
    public AccessDecisionManager accessDecisionManager(ResourceAccessVoter resourceAccessVoter) {
        final List<AccessDecisionVoter<?>> decisionVoters = new ArrayList<>();
        decisionVoters.add(resourceAccessVoter);
        return new AffirmativeBased(decisionVoters);
    }

    @Bean
    @ConditionalOnMissingBean
    public ServerSecurityContextRepository securityContextRepository(ReactiveAuthenticationManager authenticationManager,
                                                                     JWTService jwtService) {
        return new ServerSecurityContextRepository() {

            @Override
            public Mono<Void> save(ServerWebExchange swe, SecurityContext sc) {
                throw new UnsupportedOperationException("Not supported yet.");
            }

            @Override
            public Mono<SecurityContext> load(ServerWebExchange swe) {
                return Mono.defer(() -> {
                    ServerHttpRequest request = swe.getRequest();
                    String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
                    if ((authHeader != null) && authHeader.startsWith("Bearer ")) {
                        String authToken = authHeader.substring(7);
                        JWTAuthentication auth = new JWTAuthentication(authToken);
                        try {
                            auth = jwtService.parseToken(auth);
                        } catch (JwtException e) {
                            LOGGER.error("Failed to parse JWT token", e);
                        }
                        return authenticationManager.authenticate(auth).map(SecurityContextImpl::new);
                    } else {
                        return Mono.empty();
                    }
                });
            }
        };
    }

    @Bean
    @ConditionalOnMissingBean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http,
                                                         ReactiveAuthenticationManager authenticationManager,
                                                         ServerSecurityContextRepository securityContextRepository) {
        return http.exceptionHandling()
                   .authenticationEntryPoint((swe, e) -> {
                       return Mono.fromRunnable(() -> {
                           swe.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                       });
                   })
                   .accessDeniedHandler((swe, e) -> {
                       return Mono.fromRunnable(() -> {
                           swe.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
                       });
                   })
                   .and()
                   .csrf()
                   .disable()
                   .formLogin()
                   .disable()
                   .httpBasic()
                   .disable()
                   .authenticationManager(authenticationManager)
                   .securityContextRepository(securityContextRepository)
                   .authorizeExchange()
                   .pathMatchers(HttpMethod.OPTIONS)
                   .permitAll()
                   .pathMatchers("/**")
                   .permitAll() // TODO restrict this?
                   .anyExchange()
                   .authenticated()
                   .and()
                   .build();
    }

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
    public Gson gson;

    @Override
    public void configureHttpMessageCodecs(ServerCodecConfigurer configurer) {
        configurer.customCodecs().register(new GsonInefficientHttpMessageCodec.Co(gson));
        configurer.customCodecs().register(new GsonInefficientHttpMessageCodec.Dec(gson));
    }

    @Bean
    @ConditionalOnMissingBean
    public IRoleCheckerService roleCheckerService() {
        return (a, b) -> Mono.just(true);
    }
}
