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
import fr.cnes.regards.framework.security.utils.jwt.exception.JwtException;
import fr.cnes.regards.modules.jobs.service.crossmoduleallocationstrategy.IJobAllocationStrategy;
import fr.cnes.regards.modules.jobs.service.crossmoduleallocationstrategy.IJobQueue;
import fr.cnes.regards.modules.jobs.service.crossmoduleallocationstrategy.JobAllocationStrategyResponse;
import fr.cnes.regards.modules.jobs.service.manager.IJobHandler;

/**
 *
 */
public class JobPuller {

    private static final Logger LOG = LoggerFactory.getLogger(JobPuller.class);

    private List<String> projects = new ArrayList<>();

    private final IJobAllocationStrategy jobAllocationStrategy;

    private List<IJobQueue> jobQueueList;

    private final IJobHandler jobHandler;

    /**
     * Bean which allows to get the existing tenants.
     */
    private final ITenantResolver tenantResolver;

    private final MessageBroker messageBroker;

    /**
     * Gateway role
     */
    private static final String MODULE_JOB_ROLE = "USER";

    /**
     * Security JWT service
     */
    private final JWTService jwtService;

    public JobPuller(final IJobHandler pJobHandler, final JWTService pJwtService, final ITenantResolver pTenantResolver,
            final IJobAllocationStrategy pJobAllocationStrategy, final MessageBroker pMessageBroker) {
        jobHandler = pJobHandler;
        jwtService = pJwtService;
        tenantResolver = pTenantResolver;
        jobAllocationStrategy = pJobAllocationStrategy;
        messageBroker = pMessageBroker;
        populateProjectList();
    }

    private void populateProjectList() {
        try {
            jwtService.injectToken("", MODULE_JOB_ROLE);
        } catch (final JwtException e1) {
            LOG.error(e1.getMessage(), e1);
        }
        final Set<String> projectsSet = tenantResolver.getAllTenants();
        projectsSet.forEach(project -> projects.add(project));
    }

    /**
     * Using the plugin job allocation strategy, it retrieve the updated jobQueueList and the project name that we can
     * fetch one job. Then it asks the JobHandler to execute the corresponding jobId
     */
    @Scheduled(fixedDelay = 1000)
    public void pullJobs() {
        if (projects.isEmpty()) {
            LOG.error("Jobs are desactivated because there is currently no project");
            return;
        }

        // try to found a job
        final JobAllocationStrategyResponse response = jobAllocationStrategy
                .getNextQueue(projects, jobQueueList, jobHandler.getMaxJobCapacity());
        final String projectName = response.getProjectName();
        if ((projectName != null) && (projectName.length() > 0)) {
            final Long jobId = messageBroker.getJob(projectName);
            if (jobId != null) {
                jobHandler.execute(projectName, jobId);
            }
            jobQueueList = response.getJobQueueList();
        }
    }

    /**
     * @return the projects
     */
    public List<String> getProjects() {
        return projects;
    }

    /**
     * @param pProjects
     *            the projects to set
     */
    public void setProjects(final List<String> pProjects) {
        projects = pProjects;
    }

    /**
     * @return the jobQueueList
     */
    public List<IJobQueue> getJobQueueList() {
        return jobQueueList;
    }

}
