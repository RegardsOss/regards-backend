package fr.cnes.regards.framework.module.autoconfigure;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

/**
 * Configure scheduling for the whole application.
 *
 * @author Sylvain VISSIERE-GUERINET
 */
@Configuration
@Profile("!noschedule")
@EnableScheduling
public class SchedulerConfiguration implements SchedulingConfigurer {

    @Value("${regards.scheduler.pool.size:1}")
    private  int schedulerPoolSize;

    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        taskRegistrar.setScheduler(taskExecutor());
    }

    @Bean(destroyMethod = "shutdown")
    public Executor taskExecutor() {
        return Executors.newScheduledThreadPool(schedulerPoolSize);
    }
}
