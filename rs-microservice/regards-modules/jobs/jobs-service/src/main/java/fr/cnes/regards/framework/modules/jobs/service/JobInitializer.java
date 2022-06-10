package fr.cnes.regards.framework.modules.jobs.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Job initializer.
 * This component is used to launch job service as a daemon.
 *
 * @author oroussel
 */
@Profile("!nojobs")
@Component
public class JobInitializer {

    private static final Logger LOGGER = LoggerFactory.getLogger(JobInitializer.class);

    @Autowired
    private IJobService jobService;

    /**
     * During tests, {@link ApplicationReadyEvent} is received several times by this listener.
     * Avoid re-running initialization as it is not re-entering.
     */
    private static AtomicReference<Future<Void>> jobManager = new AtomicReference<>();

    @EventListener
    public void onApplicationEvent(ApplicationReadyEvent event) {
        // Ensure only one daemon is running at the same time
        jobManager.getAndUpdate(this::createOrReplaceJobManager);
    }

    /**
     * Designed to be used during test, kills the job manager task that pulls runnable jobs
     */
    public void killJobManager() {
        jobManager.getAndUpdate(this::killJobManager);
    }

    private Future<Void> killJobManager(Future<Void> existingJobManagerFuture) {
        LOGGER.info("[JOB INITIALIZER] Killing job manager.");
        if (existingJobManagerFuture != null) {
            existingJobManagerFuture.cancel(true);
        }
        return null;
    }

    private Future<Void> createOrReplaceJobManager(Future<Void> existingJobManagerFuture) {
        LOGGER.info("[JOB INITIALIZER] Start job manager.");
        if (existingJobManagerFuture != null) {
            existingJobManagerFuture.cancel(true);
        }
        return jobService.manage();
    }
}
