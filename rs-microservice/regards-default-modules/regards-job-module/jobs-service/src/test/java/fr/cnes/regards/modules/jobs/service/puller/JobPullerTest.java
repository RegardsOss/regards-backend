/**
 *
 */
package fr.cnes.regards.modules.jobs.service.puller;

import java.util.HashSet;
import java.util.Set;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;

import fr.cnes.regards.framework.multitenant.autoconfigure.tenant.ITenantResolver;
import fr.cnes.regards.framework.security.utils.jwt.JWTService;
import fr.cnes.regards.modules.jobs.service.JobDaoTestConfiguration;
import fr.cnes.regards.modules.jobs.service.allocationstrategy.DefaultJobAllocationStrategy;
import fr.cnes.regards.modules.jobs.service.crossmoduleallocationstrategy.IJobAllocationStrategy;
import fr.cnes.regards.modules.jobs.service.manager.IJobHandler;

/**
 * This test run with spring in order to get the jwtService
 */
@RunWith(SpringRunner.class)
@SpringBootTest
@ContextConfiguration(classes = { JobDaoTestConfiguration.class })
public class JobPullerTest {

    private static final Logger LOG = LoggerFactory.getLogger(JobPullerTest.class);

    /**
     * Utility service for handling JWT. Autowired by Spring.
     */
    @Autowired
    private JWTService jwtService;

    private ITenantResolver tenantResolver;

    private IJobHandler iJobHandlerMock;

    /**
     * The class we test
     */
    private JobPuller jobPuller;

    private Set<String> projectList;

    private IJobAllocationStrategy jobAllocationStrategy;

    private MessageBroker messageBroker;

    /**
     * Do some setup before each test
     */
    @Before
    public void setUp() {
        iJobHandlerMock = Mockito.mock(IJobHandler.class);
        tenantResolver = Mockito.mock(ITenantResolver.class);
        messageBroker = Mockito.mock(MessageBroker.class);
        jobAllocationStrategy = new DefaultJobAllocationStrategy();

        projectList = new HashSet<>();
        projectList.add("project1");
        projectList.add("project2");
        projectList.add("project3");
        projectList.add("project4");

        Mockito.when(tenantResolver.getAllTenants()).thenReturn(projectList);
        jobPuller = new JobPuller(iJobHandlerMock, jwtService, tenantResolver, jobAllocationStrategy, messageBroker);
        ReflectionTestUtils.setField(jobPuller, "maxJobCapacity", 5, Integer.class);

    }

    @Test
    public void testProjectList() {

        Mockito.when(tenantResolver.getAllTenants()).thenReturn(projectList);

        Assertions.assertThat(jobPuller.getProjects()).containsAll(projectList);
        Assertions.assertThat(jobPuller.getProjects().size()).isEqualTo(projectList.size());

    }

    @Test
    public void testPullJob() {
        Assertions.assertThat(jobPuller.getJobQueueList()).isEqualTo(null);
        final String projectName = "project2";
        Mockito.when(messageBroker.getJob(projectName)).thenReturn(1L);
        jobPuller.pullJobs();
        Mockito.verify(iJobHandlerMock).execute(projectName, 1L);
        Assertions.assertThat(jobPuller.getJobQueueList().size()).isEqualTo(4);
        Assertions.assertThat(jobPuller.getJobQueueList().get(0).getCurrentSize()).isEqualTo(0);
        Assertions.assertThat(jobPuller.getJobQueueList().get(0).getMaxSize()).isEqualTo(2);
        Assertions.assertThat(jobPuller.getJobQueueList().get(1).getCurrentSize()).isEqualTo(0);
        Assertions.assertThat(jobPuller.getJobQueueList().get(1).getMaxSize()).isEqualTo(2);

    }
}