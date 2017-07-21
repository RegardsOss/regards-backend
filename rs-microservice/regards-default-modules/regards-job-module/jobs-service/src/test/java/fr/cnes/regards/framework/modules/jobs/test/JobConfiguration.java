package fr.cnes.regards.framework.modules.jobs.test;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * @author oroussel
 */
@Configuration
@ComponentScan(basePackages = { "fr.cnes.regards.framework.modules.jobs.service" })
@EnableAutoConfiguration
@PropertySource(value = { "classpath:test.properties", "classpath:test_${user.name}.properties" },
        ignoreResourceNotFound = true)
@EnableAsync
public class JobConfiguration {

}
