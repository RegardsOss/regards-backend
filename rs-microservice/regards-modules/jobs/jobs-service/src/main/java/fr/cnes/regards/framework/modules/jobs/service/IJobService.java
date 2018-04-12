package fr.cnes.regards.framework.modules.jobs.service;

import org.springframework.cloud.context.scope.refresh.RefreshScopeRefreshedEvent;
import org.springframework.context.event.ContextRefreshedEvent;

/**
 * Job service. It's a daemon that listen for jobs to be executed and a service that handles abort, suspend or
 * resume messages.
 * @author oroussel
 */
public interface IJobService {

    void handleContextRefreshedEvent(ContextRefreshedEvent event);

    void handleRefreshScopeRefreshedEvent(RefreshScopeRefreshedEvent event);

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
}
