package fr.cnes.regards.framework.modules.jobs.service;

/**
 * Job service. It's a daemon that listen for jobs to be executed and a service that handles abort, suspend or
 * resume messages.
 * @author oroussel
 */
public interface IJobService {

    /**
     * Daemon method
     * This method is called asynchronously and never ends.
     * It listen for jobs to be executed on all tenants
     */
    void manage();

}
