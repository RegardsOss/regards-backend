/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.jobs.service.puller;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.scheduling.annotation.Scheduled;

import fr.cnes.regards.framework.security.utils.jwt.JWTService;
import fr.cnes.regards.framework.security.utils.jwt.exception.JwtException;
import fr.cnes.regards.modules.jobs.service.crossmoduleallocationstrategy.IJobAllocationStrategy;
import fr.cnes.regards.modules.jobs.service.crossmoduleallocationstrategy.IJobQueue;
import fr.cnes.regards.modules.jobs.service.crossmoduleallocationstrategy.JobAllocationStrategyResponse;
import fr.cnes.regards.modules.jobs.service.manager.IJobHandler;
import fr.cnes.regards.modules.project.client.IProjectsClient;
import fr.cnes.regards.modules.project.domain.Project;

/**
 *
 */
public class JobPuller {

    /**
     *
     */
    private static final int MAX_THREAD = 5;

    private static final Logger LOG = LoggerFactory.getLogger(JobPuller.class);

    private final IProjectsClient projectsClient;

    private List<Project> projects;

    private final IJobAllocationStrategy jobAllocationStrategy;

    private List<IJobQueue> jobQueueList;

    private final IJobHandler jobHandler;

    private final MessageBroker messageBroker;

    /**
     * Gateway role
     */
    private static final String MODULE_JOB_ROLE = "USER";

    /**
     * Security JWT service
     */
    private final JWTService jwtService;

    /**
     *
     */
    public JobPuller(IJobHandler pJobHandler, JWTService pJwtService, IProjectsClient pProjectsClient,
            IJobAllocationStrategy pJobAllocationStrategy, MessageBroker pMessageBroker) {
        jobHandler = pJobHandler;
        jwtService = pJwtService;
        projectsClient = pProjectsClient;
        jobAllocationStrategy = pJobAllocationStrategy;
        messageBroker = pMessageBroker;
        populateProjectList();
    }

    /**
     *
     */
    private void populateProjectList() {
        try {
            jwtService.injectToken("", MODULE_JOB_ROLE);
        } catch (JwtException e1) {
            LOG.error(e1.getMessage(), e1);
        }
        try {
            HttpEntity<List<Resource<Project>>> response = projectsClient.retrieveProjectList();
            projects = StreamSupport.stream(response.getBody().spliterator(), true).map(a -> a.getContent())
                    .collect(Collectors.toList());
        } catch (final NoSuchElementException e) {
            LOG.error(e.getMessage(), e);
        }
    }

    /**
     * Using the plugin job allocation strategy, it updates the jobQueueList and the project we can fectch job depending
     * of the allocation strategy. Then it asks the JobHandler to execute the corresponding jobId
     */
    @Scheduled(fixedDelay = 1000)
    public void pullJobs() {
        if (projects.size() == 0) {
            LOG.error("Jobs are desactivated because there is currently no project");
            return;
        }

        // try to found a job
        JobAllocationStrategyResponse response = jobAllocationStrategy.getNextQueue(projects, jobQueueList, MAX_THREAD);
        String projectName = response.getProjectName();
        if ((projectName != null) && (projectName.length() > 0)) {
            Long jobId = messageBroker.getJob(projectName);
            if (jobId != null) {
                jobHandler.execute(jobId);
            }
            jobQueueList = response.getJobQueueList();
        }
    }

    /**
     * @return the projects
     */
    public List<Project> getProjects() {
        return projects;
    }

    /**
     * @param pProjects
     *            the projects to set
     */
    public void setProjects(List<Project> pProjects) {
        projects = pProjects;
    }

    /**
     * @return the jobQueueList
     */
    public List<IJobQueue> getJobQueueList() {
        return jobQueueList;
    }

}
