package fr.cnes.regards.framework.modules.jobs.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Job initializer.
 * This component is used to launch job service as a daemon.
 * @author oroussel
 */
@Component
public class JobInitializer {

    @Autowired
    private IJobService jobService;

    @EventListener
    public void onApplicationEvent(ApplicationReadyEvent event) {
        jobService.manage();
    }
}
