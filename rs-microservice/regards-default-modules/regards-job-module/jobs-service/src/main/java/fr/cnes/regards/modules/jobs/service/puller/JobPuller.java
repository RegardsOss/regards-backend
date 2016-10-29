/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.jobs.service.puller;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;

import fr.cnes.regards.framework.multitenant.autoconfigure.tenant.ITenantResolver;
import fr.cnes.regards.framework.security.utils.jwt.JWTService;
import fr.cnes.regards.modules.jobs.service.communication.INewJobPullerMessageBroker;
import fr.cnes.regards.modules.jobs.service.crossmoduleallocationstrategy.IJobAllocationStrategy;
import fr.cnes.regards.modules.jobs.service.crossmoduleallocationstrategy.IJobQueue;
import fr.cnes.regards.modules.jobs.service.crossmoduleallocationstrategy.JobAllocationStrategyResponse;
import fr.cnes.regards.modules.jobs.service.manager.IJobHandler;

/**
 *
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
     * Plugin that manages the allocation strategy. Returns the tenant name that is autorized to fetch a job
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
    private final INewJobPullerMessageBroker newJobPuller;

    /**
     * Security JWT service
     */
    private final JWTService jwtService;

    public JobPuller(final IJobHandler pJobHandler, final JWTService pJwtService, final ITenantResolver pTenantResolver,
            final IJobAllocationStrategy pJobAllocationStrategy, final INewJobPullerMessageBroker pNewJobPuller) {
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
            jwtService.injectToken("", MODULE_JOB_ROLE);
            projects = getTenants();
            if (projects.isEmpty()) {
                throw new Exception("Jobs are desactivated because there is currently no project");
            }
        } catch (final Exception e1) {
            LOG.error(e1.getMessage(), e1);
            return;
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
