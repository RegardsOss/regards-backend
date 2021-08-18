package fr.cnes.regards.framework.modules.jobs.service;

import java.util.concurrent.RunnableFuture;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.scheduling.annotation.Scheduled;

import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;

/**
 * Job service. It's a daemon that listen for jobs to be executed and a service that handles abort, suspend or
 * resume messages.
 * @author oroussel
 */
public interface IJobService {

    void onApplicationEvent(ApplicationReadyEvent event);

    /**
     * Daemon method
     * This method is called asynchronously and never ends.
     * It listen for jobs to be executed on all tenants
     */
    void manage();

    /**
     * Scheduled method to update all current running jobs completions values into database
     */
    void updateCurrentJobsCompletions();

    void jobsHeartbeat();

    RunnableFuture<Void> runJob(JobInfo jobInfo, String tenant);

    /**
     * For test purpose, allow to reset the job service threadPool
     */
    void cleanAndRestart();
}
