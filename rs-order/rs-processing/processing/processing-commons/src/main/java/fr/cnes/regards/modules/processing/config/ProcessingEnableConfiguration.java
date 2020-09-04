package fr.cnes.regards.modules.processing.config;

import fr.cnes.regards.framework.feign.autoconfigure.FeignWebMvcConfiguration;
import fr.cnes.regards.framework.security.autoconfigure.MethodSecurityAutoConfiguration;
import fr.cnes.regards.framework.security.autoconfigure.WebSecurityAutoConfiguration;
import name.nkonev.r2dbc.migrate.autoconfigure.R2dbcMigrateAutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.web.reactive.config.EnableWebFlux;

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
public class ProcessingEnableConfiguration {

}
