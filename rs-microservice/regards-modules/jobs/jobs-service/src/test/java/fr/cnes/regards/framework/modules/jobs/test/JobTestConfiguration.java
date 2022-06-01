package fr.cnes.regards.framework.modules.jobs.test;

import fr.cnes.regards.framework.modules.jobs.service.JobServiceJobCreator;
import fr.cnes.regards.framework.modules.jobs.service.JobTestCleaner;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * @author oroussel
 */
@Configuration
@EnableAutoConfiguration
@PropertySource(value = { "classpath:test.properties", "classpath:test_${user.name}.properties" },
    ignoreResourceNotFound = true)
@EnableAsync
@EnableScheduling
public class JobTestConfiguration {
    @Bean
    public JobTestCleaner jobTestCleaner() {
        return new JobTestCleaner();
    }

    @Bean
    public JobServiceJobCreator jobServiceJobCreator() {
        return new JobServiceJobCreator();
    }
}
