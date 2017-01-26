/**
 *
 */
package fr.cnes.regards.framework.modules.jobs.service.puller;

import java.util.HashMap;
import java.util.Map;
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

import fr.cnes.regards.framework.modules.jobs.service.JobServiceTestConfiguration;
import fr.cnes.regards.framework.modules.jobs.service.allocationstrategy.DefaultJobAllocationStrategy;
import fr.cnes.regards.framework.modules.jobs.service.communication.INewJobPuller;
import fr.cnes.regards.framework.modules.jobs.service.crossmoduleallocationstrategy.IJobAllocationStrategy;
import fr.cnes.regards.framework.modules.jobs.service.manager.IJobHandler;
import fr.cnes.regards.framework.modules.jobs.service.puller.JobPuller;
import fr.cnes.regards.framework.multitenant.ITenantResolver;
import fr.cnes.regards.framework.security.utils.jwt.JWTService;
import fr.cnes.regards.framework.security.utils.jwt.exception.JwtException;

/**
 * @author Léo Mieulet
 *
 */
@RunWith(SpringRunner.class)
@SpringBootTest
@ContextConfiguration(classes = { JobServiceTestConfiguration.class })
public class JobPullerTest {

    private static final Logger LOG = LoggerFactory.getLogger(JobPullerTest.class);

    /**
     * Utility service for handling JWT. Autowired by Spring.
     */
    private JWTService jwtServiceMocked;

    private ITenantResolver tenantResolver;

    private IJobHandler jobHandlerMock;

    /**
     * The class we test
     */
    private JobPuller jobPuller;

    private Set<String> projectList;

    private IJobAllocationStrategy jobAllocationStrategy;

    private INewJobPuller newJobPuller;

    /**
     * Do some setup before each test
     */
    @Before
    public void setUp() {
        jobHandlerMock = Mockito.mock(IJobHandler.class);
        Mockito.when((jobHandlerMock).getMaxJobCapacity()).thenReturn(5);

        jwtServiceMocked = Mockito.mock(JWTService.class);
        tenantResolver = Mockito.mock(ITenantResolver.class);
        newJobPuller = Mockito.mock(INewJobPuller.class);
        jobAllocationStrategy = new DefaultJobAllocationStrategy();

        projectList = new TreeSet<>();
        projectList.add("project1");
        projectList.add("project2");
        projectList.add("project3");
        projectList.add("project4");

        Mockito.when(tenantResolver.getAllTenants()).thenReturn(projectList);
        jobPuller = new JobPuller(jobHandlerMock, jwtServiceMocked, tenantResolver, jobAllocationStrategy,
                newJobPuller);

    }

    @Test
    public void testPullJob() {
        Assertions.assertThat(jobPuller.getJobQueueList()).isEqualTo(null);
        final String projectName = "project1";
        Mockito.when(newJobPuller.getJob(projectName)).thenReturn(1L);
        jobPuller.pullJobs();
        Mockito.verify(jobHandlerMock).execute(projectName, 1L);
        Assertions.assertThat(jobPuller.getJobQueueList().size()).isEqualTo(4);
        Assertions.assertThat(jobPuller.getJobQueueList().get(0).getCurrentSize()).isEqualTo(0);
        Assertions.assertThat(jobPuller.getJobQueueList().get(0).getMaxSize()).isEqualTo(2);
        Assertions.assertThat(jobPuller.getJobQueueList().get(1).getCurrentSize()).isEqualTo(0);
        Assertions.assertThat(jobPuller.getJobQueueList().get(1).getMaxSize()).isEqualTo(2);

        final Map<String, Integer> nbActivbeThreadByTenant = new HashMap<>();
        nbActivbeThreadByTenant.put(projectName, 1);
        Mockito.when(jobHandlerMock.getNbActiveThreadByTenant()).thenReturn(nbActivbeThreadByTenant);

        jobPuller.pullJobs();

    }

    @Test
    public void testPullJobWhenNoTenant() {
        Mockito.when(tenantResolver.getAllTenants()).thenReturn(new TreeSet<>());
        jobPuller.pullJobs();
        Mockito.verifyZeroInteractions(jobHandlerMock);

    }

    @Test
    public void testPullJobWhenJWTTokenFail() throws JwtException {
        final String moduleJobRole = (String) ReflectionTestUtils.getField(jobPuller, "MODULE_JOB_ROLE");
        Mockito.doThrow(new JwtException("some exception")).when(jwtServiceMocked).injectToken("", moduleJobRole);
        jobPuller.pullJobs();
        Mockito.verifyZeroInteractions(jobHandlerMock);
    }

    @Test
    public void testPullJobWhenThreadPoolFull() {
        Mockito.when(jobHandlerMock.isThreadPoolFull()).thenReturn(true);
        jobPuller.pullJobs();
        Mockito.verifyZeroInteractions(newJobPuller);

    }
}