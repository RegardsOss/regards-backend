package fr.cnes.regards.framework.modules.jobs.test;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * @author oroussel
 */
@Configuration
@EnableAutoConfiguration
@PropertySource(
        value = { "classpath:test-multitenant.properties", "classpath:test-multitenant_${user.name}.properties" },
        ignoreResourceNotFound = true)
@EnableAsync
@EnableScheduling
public class JobMultitenantConfiguration {

}
