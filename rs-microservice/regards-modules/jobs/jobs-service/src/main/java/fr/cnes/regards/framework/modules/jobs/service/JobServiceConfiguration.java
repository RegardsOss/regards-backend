package fr.cnes.regards.framework.modules.jobs.service;

import java.util.concurrent.Executor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * @author oroussel
 */
@Configuration
@EnableAsync
public class JobServiceConfiguration implements AsyncConfigurer {
    private static final Logger LOGGER = LoggerFactory.getLogger(JobService.class);

    @Override
    public Executor getAsyncExecutor() {
        return new SimpleAsyncTaskExecutor("job-service-task");
    }

    /**
     * We don't give a shit of the content of this method, it will never be called anyway
     */
    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (throwable, method, params) -> {
            LOGGER.error("*****************************************************");
            LOGGER.error("Exception message - " + throwable.getMessage(), throwable);
            LOGGER.error("Method name - " + method.getName());
            LOGGER.error("*****************************************************");
        };
    }
}
