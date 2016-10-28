/**
 *
 */
package fr.cnes.regards.modules.jobs.service.puller;

import java.util.Set;
import java.util.TreeSet;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;

import fr.cnes.regards.framework.multitenant.autoconfigure.tenant.ITenantResolver;
import fr.cnes.regards.framework.security.utils.jwt.JWTService;
import fr.cnes.regards.framework.security.utils.jwt.exception.JwtException;
import fr.cnes.regards.modules.jobs.service.JobDaoTestConfiguration;
import fr.cnes.regards.modules.jobs.service.allocationstrategy.DefaultJobAllocationStrategy;
import fr.cnes.regards.modules.jobs.service.communication.INewJobPullerMessageBroker;
import fr.cnes.regards.modules.jobs.service.crossmoduleallocationstrategy.IJobAllocationStrategy;
import fr.cnes.regards.modules.jobs.service.manager.IJobHandler;

/**
 * @author lmieulet
 *
 */
@RunWith(SpringRunner.class)
@SpringBootTest
@ContextConfiguration(classes = { JobDaoTestConfiguration.class })
public class JobPullerTest {

    private static final Logger LOG = LoggerFactory.getLogger(JobPullerTest.class);

    /**
     * Utility service for handling JWT. Autowired by Spring.
     */
    private JWTService jwtServiceMocked;

    private ITenantResolver tenantResolver;

    private IJobHandler iJobHandlerMock;

    /**
     * The class we test
     */
    private JobPuller jobPuller;

    private Set<String> projectList;

    private IJobAllocationStrategy jobAllocationStrategy;

    private INewJobPullerMessageBroker newJobPuller;

    /**
     * Do some setup before each test
     */
    @Before
    public void setUp() {
        iJobHandlerMock = Mockito.mock(IJobHandler.class);
        Mockito.when((iJobHandlerMock).getMaxJobCapacity()).thenReturn(5);

        jwtServiceMocked = Mockito.mock(JWTService.class);
        tenantResolver = Mockito.mock(ITenantResolver.class);
        newJobPuller = Mockito.mock(INewJobPullerMessageBroker.class);
        jobAllocationStrategy = new DefaultJobAllocationStrategy();

        projectList = new TreeSet<>();
        projectList.add("project1");
        projectList.add("project2");
        projectList.add("project3");
        projectList.add("project4");

        Mockito.when(tenantResolver.getAllTenants()).thenReturn(projectList);
        jobPuller = new JobPuller(iJobHandlerMock, jwtServiceMocked, tenantResolver, jobAllocationStrategy,
                newJobPuller);

    }

    @Test
    public void testPullJob() {
        Assertions.assertThat(jobPuller.getJobQueueList()).isEqualTo(null);
        final String projectName = "project1";
        Mockito.when(newJobPuller.getJob(projectName)).thenReturn(1L);
        jobPuller.pullJobs();
        Mockito.verify(iJobHandlerMock).execute(projectName, 1L);
        Assertions.assertThat(jobPuller.getJobQueueList().size()).isEqualTo(4);
        Assertions.assertThat(jobPuller.getJobQueueList().get(0).getCurrentSize()).isEqualTo(0);
        Assertions.assertThat(jobPuller.getJobQueueList().get(0).getMaxSize()).isEqualTo(2);
        Assertions.assertThat(jobPuller.getJobQueueList().get(1).getCurrentSize()).isEqualTo(0);
        Assertions.assertThat(jobPuller.getJobQueueList().get(1).getMaxSize()).isEqualTo(2);
    }

    @Test
    public void testPullJobWhenNoTenant() {
        Mockito.when(tenantResolver.getAllTenants()).thenReturn(new TreeSet<>());
        jobPuller.pullJobs();
        Mockito.verifyZeroInteractions(iJobHandlerMock);

    }

    @Test
    public void testPullJobWhenJWTTokenFail() throws JwtException {
        final String moduleJobRole = (String) ReflectionTestUtils.getField(jobPuller, "MODULE_JOB_ROLE");
        Mockito.doThrow(new JwtException("some exception")).when(jwtServiceMocked).injectToken("", moduleJobRole);
        jobPuller.pullJobs();
        Mockito.verifyZeroInteractions(iJobHandlerMock);

    }
}