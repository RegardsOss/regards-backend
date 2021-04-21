package fr.cnes.regards.framework.modules.jobs.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Job initializer.
 * This component is used to launch job service as a daemon.
 * @author oroussel
 */
@Component
public class JobInitializer {

    private static final Logger LOGGER = LoggerFactory.getLogger(JobInitializer.class);

    @Autowired
    private IJobService jobService;

    @EventListener
    public void onApplicationEvent(ApplicationReadyEvent event) {
        LOGGER.info("[JOB INITIALIZER] Start job manager.");
        jobService.manage();
    }
}
