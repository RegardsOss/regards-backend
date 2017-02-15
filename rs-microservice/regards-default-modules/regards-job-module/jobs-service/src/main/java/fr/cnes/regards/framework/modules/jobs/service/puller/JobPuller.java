/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.modules.jobs.service.puller;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;

import fr.cnes.regards.framework.modules.jobs.service.communication.INewJobPuller;
import fr.cnes.regards.framework.modules.jobs.service.crossmoduleallocationstrategy.IJobAllocationStrategy;
import fr.cnes.regards.framework.modules.jobs.service.crossmoduleallocationstrategy.IJobQueue;
import fr.cnes.regards.framework.modules.jobs.service.crossmoduleallocationstrategy.JobAllocationStrategyResponse;
import fr.cnes.regards.framework.modules.jobs.service.manager.IJobHandler;
import fr.cnes.regards.framework.multitenant.ITenantResolver;
import fr.cnes.regards.framework.security.utils.jwt.JWTService;
import fr.cnes.regards.framework.security.utils.jwt.exception.JwtException;

/**
 * @author Léo Mieulet
 */
public class JobPuller {

    /**
     * Time to wait between each pullJob
     */
    private static final int PULL_JOB_DELAY = 1000;

    /**
     * Logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(JobPuller.class);

    /**
     * Gateway role
     */
    private static final String MODULE_JOB_ROLE = "USER";

    /**
     * Plugin that manages the allocation strategy. Returns the tenant name that is authorized to fetch a job
     */
    private final IJobAllocationStrategy jobAllocationStrategy;

    /**
     * Store by tenant the number of current active jobs and the max number of job for this tenant
     */
    private List<IJobQueue> jobQueueList;

    /**
     * Job runner
     */
    private final IJobHandler jobHandler;

    /**
     * Bean which allows to get the existing tenants.
     */
    private final ITenantResolver tenantResolver;

    /**
     * Allows to fetch one job from AMQP
     */
    private final INewJobPuller newJobPuller;

    /**
     * Security JWT service
     */
    private final JWTService jwtService;

    public JobPuller(final IJobHandler pJobHandler, final JWTService pJwtService, final ITenantResolver pTenantResolver,
            final IJobAllocationStrategy pJobAllocationStrategy, final INewJobPuller pNewJobPuller) {
        jobHandler = pJobHandler;
        jwtService = pJwtService;
        tenantResolver = pTenantResolver;
        jobAllocationStrategy = pJobAllocationStrategy;
        newJobPuller = pNewJobPuller;
    }

    /**
     * Using the plugin job allocation strategy, it retrieve the updated jobQueueList and the project name that we can
     * fetch one job. Then it asks the JobHandler to execute the corresponding jobId
     */
    @Scheduled(fixedDelay = PULL_JOB_DELAY)
    public void pullJobs() {
        final List<String> projects;
        try {
            jwtService.injectToken("", MODULE_JOB_ROLE, "");
            projects = getTenants();
            if (projects.isEmpty()) {
                LOG.warn("Jobs are deactivated because there is currently no project");
                return;
            }
        } catch (final JwtException e1) {
            LOG.error(e1.getMessage(), e1);
            return;
        }

        // Do not run any job if the microservice has already reached maxJobCapacity
        if (jobHandler.isThreadPoolFull()) {
            LOG.info("This microservice has reach his max number of active jobs");
            return;
        }

        // Update the current number of active thread inside the jobQueueList
        if (jobQueueList != null) {
            updateJobQueueList();
        }

        // try to found a job
        final JobAllocationStrategyResponse response = jobAllocationStrategy
                .getNextQueue(projects, jobQueueList, jobHandler.getMaxJobCapacity());
        final String projectName = response.getProjectName();
        if ((projectName != null) && (projectName.length() > 0)) {
            final Long jobId = newJobPuller.getJob(projectName);
            if (jobId != null) {
                jobHandler.execute(projectName, jobId);
            }
            jobQueueList = response.getJobQueueList();
        }
    }

    /**
     * Update the jobQueueList with current number of active job
     */
    private void updateJobQueueList() {
        final Map<String, Integer> nbActiveThreadByTenant = jobHandler.getNbActiveThreadByTenant();
        for (final Entry<String, Integer> activeThreadEntry : nbActiveThreadByTenant.entrySet()) {
            for (final IJobQueue jobQueue : jobQueueList) {
                if (jobQueue.getName().equals(activeThreadEntry.getKey())) {
                    jobQueue.setCurrentSize(activeThreadEntry.getValue());
                }
            }
        }
    }

    /**
     * @return the jobQueueList
     */
    public List<IJobQueue> getJobQueueList() {
        return jobQueueList;
    }

    private List<String> getTenants() {
        final List<String> projects = new ArrayList<>();
        final Set<String> projectsSet = tenantResolver.getAllTenants();
        projectsSet.forEach(project -> projects.add(project));
        return projects;
    }

}
