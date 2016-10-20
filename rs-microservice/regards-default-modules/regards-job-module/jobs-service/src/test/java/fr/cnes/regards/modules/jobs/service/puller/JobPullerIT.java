/**
 *
 */
package fr.cnes.regards.modules.jobs.service.puller;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import fr.cnes.regards.framework.security.utils.jwt.JWTService;
import fr.cnes.regards.modules.jobs.service.JobDaoTestConfiguration;
import fr.cnes.regards.modules.jobs.service.allocationstrategy.DefaultJobAllocationStrategy;
import fr.cnes.regards.modules.jobs.service.crossmoduleallocationstrategy.IJobAllocationStrategy;
import fr.cnes.regards.modules.jobs.service.manager.IJobHandler;
import fr.cnes.regards.modules.project.client.IProjectsClient;
import fr.cnes.regards.modules.project.domain.Project;

/**
 * @author lmieulet
 *
 */
@RunWith(SpringRunner.class)
@SpringBootTest
@ContextConfiguration(classes = { JobDaoTestConfiguration.class })
public class JobPullerIT {

    private static final Logger LOG = LoggerFactory.getLogger(JobPullerIT.class);

    /**
     * Utility service for handling JWT. Autowired by Spring.
     */
    @Autowired
    private JWTService jwtService;

    private IProjectsClient projectsClient;

    private IJobHandler iJobHandlerMock;

    /**
     * The class we test
     */
    private JobPuller jobPuller;

    private List<Project> projectList;

    private IJobAllocationStrategy jobAllocationStrategy;

    private MessageBroker messageBroker;

    /**
     * Do some setup before each test
     */
    @Before
    public void setUp() {
        iJobHandlerMock = Mockito.mock(IJobHandler.class);
        projectsClient = Mockito.mock(IProjectsClient.class);
        messageBroker = Mockito.mock(MessageBroker.class);
        jobAllocationStrategy = new DefaultJobAllocationStrategy();

        projectList = new ArrayList<>();
        projectList.add(new Project(1L, "description", "icon", true, "project1"));
        projectList.add(new Project(2L, "description", "icon", true, "project2"));
        projectList.add(new Project(3L, "description", "icon", true, "project3"));
        projectList.add(new Project(4L, "description", "icon", true, "project4"));
        final ResponseEntity<List<Resource<Project>>> projects = new ResponseEntity<>(
                StreamSupport.stream(projectList.spliterator(), true).map(a -> new Resource<Project>(a))
                        .collect(Collectors.toList()),
                HttpStatus.OK);

        Mockito.when(projectsClient.retrieveProjectList()).thenReturn(projects);
        jobPuller = new JobPuller(iJobHandlerMock, jwtService, projectsClient, jobAllocationStrategy, messageBroker);
    }

    @Test
    public void testProjectList() {
        final List<Project> projectList = new ArrayList<>();
        projectList.add(new Project(1L, "description", "icon", true, "project1"));
        projectList.add(new Project(2L, "description", "icon", true, "project2"));
        projectList.add(new Project(3L, "description", "icon", true, "project3"));
        projectList.add(new Project(4L, "description", "icon", true, "project4"));
        final ResponseEntity<List<Resource<Project>>> projects = new ResponseEntity<>(
                StreamSupport.stream(projectList.spliterator(), true).map(a -> new Resource<Project>(a))
                        .collect(Collectors.toList()),
                HttpStatus.OK);
        ;
        Mockito.when(projectsClient.retrieveProjectList()).thenReturn(projects);

        Assertions.assertThat(jobPuller.getProjects()).containsAll(projectList);
        Assertions.assertThat(jobPuller.getProjects().size()).isEqualTo(projectList.size());

    }

    @Test
    public void testPullJob() {
        Assertions.assertThat(jobPuller.getJobQueueList()).isEqualTo(null);
        Mockito.when(messageBroker.getJob("project1")).thenReturn(1L);
        jobPuller.pullJobs();
        Mockito.verify(iJobHandlerMock).execute(1L);
        Assertions.assertThat(jobPuller.getJobQueueList().size()).isEqualTo(4);
        Assertions.assertThat(jobPuller.getJobQueueList().get(0).getCurrentSize()).isEqualTo(0);
        Assertions.assertThat(jobPuller.getJobQueueList().get(0).getMaxSize()).isEqualTo(2);
        Assertions.assertThat(jobPuller.getJobQueueList().get(1).getCurrentSize()).isEqualTo(0);
        Assertions.assertThat(jobPuller.getJobQueueList().get(1).getMaxSize()).isEqualTo(2);

    }
}