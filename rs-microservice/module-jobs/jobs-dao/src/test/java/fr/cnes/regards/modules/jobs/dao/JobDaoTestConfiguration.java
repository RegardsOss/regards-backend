/**
 *
 */
package fr.cnes.regards.modules.jobs.dao;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.PropertySource;

import fr.cnes.regards.microservices.core.configuration.swagger.SwaggerConfiguration;
import fr.cnes.regards.microservices.core.security.configuration.WebSocketConfiguration;

@Configuration
@EnableAutoConfiguration(exclude = { DataSourceAutoConfiguration.class })
@ComponentScan(
        basePackages = { "fr.cnes.regards.microservices.core", "fr.cnes.regards.modules.jobs",
                "fr.cnes.regards.security.utils" },
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,
                value = { SwaggerConfiguration.class, WebSocketConfiguration.class }))
@PropertySource("classpath:tests.properties")
public class JobDaoTestConfiguration {

}