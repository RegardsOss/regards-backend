/**
 *
 */
package fr.cnes.regards.modules.jobs.service.manager;

import java.time.LocalDateTime;
import java.util.Map;

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

import fr.cnes.regards.framework.security.utils.jwt.JWTService;
import fr.cnes.regards.framework.security.utils.jwt.exception.JwtException;
import fr.cnes.regards.modules.jobs.domain.JobConfiguration;
import fr.cnes.regards.modules.jobs.domain.JobInfo;
import fr.cnes.regards.modules.jobs.domain.JobParameters;
import fr.cnes.regards.modules.jobs.domain.JobStatus;
import fr.cnes.regards.modules.jobs.domain.StatusInfo;
import fr.cnes.regards.modules.jobs.service.Application;
import fr.cnes.regards.modules.jobs.service.JobHandlerTestConfiguration;
import fr.cnes.regards.modules.jobs.service.systemservice.IJobInfoSystemService;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = { JobHandlerTestConfiguration.class })
@SpringBootTest(classes = Application.class)
public class JobHandlerIT {

    private static final Logger LOG = LoggerFactory.getLogger(JobHandlerIT.class);

    @Autowired
    private JobHandler jobHandler;

    private IJobInfoSystemService jobInfoSystemServiceMock;

    private JobInfo pJobInfo;

    private JobInfo pJobInfo2;

    private JobInfo pJobInfo3;

    private JobInfo pJobInfo4;

    @Autowired
    private JWTService jwtService;

    /**
     * Do some setup before each test
     *
     * @param pRole
     * @throws JwtException
     */
    @Before
    public void setUp() throws JwtException {
        jobInfoSystemServiceMock = Mockito.mock(IJobInfoSystemService.class);

        // Replace stubs by mocks
        ReflectionTestUtils.setField(jobHandler, "jobInfoSystemService", jobInfoSystemServiceMock,
                                     IJobInfoSystemService.class);

        // Create a new jobInfo
        final JobParameters pParameters = new JobParameters();
        pParameters.add("follow", "Kepler");
        final String jobClassName = "fr.cnes.regards.modules.jobs.service.manager.AJob";
        final LocalDateTime pEstimatedCompletion = LocalDateTime.now().plusHours(5);
        final LocalDateTime pExpirationDate = LocalDateTime.now().plusDays(15);
        final String description = "some job description";
        final String owner = "IntegrationTest";
        final JobConfiguration pJobConfiguration = new JobConfiguration(description, pParameters, jobClassName,
                pEstimatedCompletion, pExpirationDate, 1, null, owner);

        pJobInfo = new JobInfo(pJobConfiguration);
        pJobInfo.setId(1L);

        pJobInfo2 = new JobInfo(pJobConfiguration);
        pJobInfo2.setId(2L);

        pJobInfo3 = new JobInfo(pJobConfiguration);
        pJobInfo3.setId(3L);

        pJobInfo4 = new JobInfo(pJobConfiguration);
        pJobInfo4.setId(4L);

        jwtService.injectToken("project1", "USER");
    }

    @Test
    public void testExecuteJob() throws InterruptedException {
        final String tenantName = "project1";

        final Map<Long, CoupleThreadTenantName> threads = (Map<Long, CoupleThreadTenantName>) ReflectionTestUtils
                .getField(jobHandler, "threads");
        Mockito.when(jobInfoSystemServiceMock.findJobInfo(tenantName, pJobInfo.getId())).thenReturn(pJobInfo);
        final StatusInfo statusInfo = jobHandler.execute(tenantName, pJobInfo.getId());
        final Thread thread1 = threads.get(pJobInfo.getId()).getThread();

        Mockito.when(jobInfoSystemServiceMock.findJobInfo(tenantName, pJobInfo2.getId())).thenReturn(pJobInfo2);
        final StatusInfo statusInfo2 = jobHandler.execute(tenantName, pJobInfo2.getId());
        final Thread thread2 = threads.get(pJobInfo2.getId()).getThread();

        Mockito.when(jobInfoSystemServiceMock.findJobInfo(tenantName, pJobInfo3.getId())).thenReturn(pJobInfo3);
        final StatusInfo statusInfo3 = jobHandler.execute(tenantName, pJobInfo3.getId());
        final Thread thread3 = threads.get(pJobInfo3.getId()).getThread();

        Mockito.when(jobInfoSystemServiceMock.findJobInfo(tenantName, pJobInfo4.getId())).thenReturn(pJobInfo4);
        final StatusInfo statusInfo4 = jobHandler.execute(tenantName, pJobInfo4.getId());
        Assertions.assertThat(threads.size()).isGreaterThan(0);
        final Thread thread4 = threads.get(pJobInfo4.getId()).getThread();

        Assertions.assertThat(statusInfo).isNotNull();
        Assertions.assertThat(statusInfo.getJobStatus()).isEqualTo(JobStatus.RUNNING);
        Assertions.assertThat(statusInfo2.getJobStatus()).isEqualTo(JobStatus.RUNNING);
        Assertions.assertThat(statusInfo3.getJobStatus()).isEqualTo(JobStatus.RUNNING);
        Assertions.assertThat(statusInfo4.getJobStatus()).isEqualTo(JobStatus.RUNNING);
        // Succeed only if thread runs, dies, and the thread list from JobHandler is cleanup
        while (thread1.isAlive() || thread2.isAlive() || thread3.isAlive() || thread4.isAlive()
                || (threads.size() > 0)) {
            // Wait until thread are dead
        }

    }

    @Test
    public void testExecuteJobClassNotFound() {
        final String tenantName = "project1";
        pJobInfo.setClassName("fr.cnes.regards.modules.jobs.that.does.not.exist");
        Mockito.when(jobInfoSystemServiceMock.findJobInfo(tenantName, pJobInfo.getId())).thenReturn(pJobInfo);

        final StatusInfo statusInfo = jobHandler.execute(tenantName, pJobInfo.getId());
        Assertions.assertThat(statusInfo).isNotNull();
        Assertions.assertThat(statusInfo.getJobStatus()).isEqualTo(JobStatus.FAILED);
    }

    @Test
    public void testShutdown() {
        final String tenantName = "project1";
        Mockito.when(jobInfoSystemServiceMock.findJobInfo(tenantName, pJobInfo.getId())).thenReturn(pJobInfo);
        final StatusInfo statusInfo = jobHandler.execute(tenantName, pJobInfo.getId());
        jobHandler.shutdown();
    }

    @Test
    public void testShutdownNow() {
        final String tenantName = "project1";
        Mockito.when(jobInfoSystemServiceMock.findJobInfo(tenantName, pJobInfo.getId())).thenReturn(pJobInfo);
        final StatusInfo statusInfo = jobHandler.execute(tenantName, pJobInfo.getId());
        jobHandler.shutdownNow();
        final StatusInfo statusInfo2 = jobHandler.execute(tenantName, pJobInfo2.getId());
    }

    @Test
    public void testAbort() {
        final String tenantName = "project1";
        Mockito.when(jobInfoSystemServiceMock.findJobInfo(tenantName, pJobInfo.getId())).thenReturn(pJobInfo);
        Mockito.when(jobInfoSystemServiceMock.updateJobInfo(tenantName, pJobInfo)).thenReturn(pJobInfo);

        final StatusInfo statusInfo = jobHandler.execute(tenantName, pJobInfo.getId());
        final StatusInfo statusInfoAbort = jobHandler.abort(pJobInfo.getId());
    }

    @Test
    public void testNbActiveThreadByTenant() {
        final String tenantName = "project1";
        Mockito.when(jobInfoSystemServiceMock.findJobInfo(tenantName, pJobInfo.getId())).thenReturn(pJobInfo);
        final StatusInfo statusInfo = jobHandler.execute(tenantName, pJobInfo.getId());
        final Map<String, Integer> nbActiveThreadByTenant = jobHandler.getNbActiveThreadByTenant();
        Assertions.assertThat(nbActiveThreadByTenant.size()).isEqualTo(1);
        Assertions.assertThat(nbActiveThreadByTenant.get(tenantName)).isEqualTo(1);
    }

    @Test
    public void testIsThreadPoolFull() {
        ReflectionTestUtils.setField(jobHandler, "maxJobCapacity", 0);
        Assertions.assertThat(jobHandler.isThreadPoolFull()).isEqualTo(true);
        ReflectionTestUtils.setField(jobHandler, "maxJobCapacity", 5);
        Assertions.assertThat(jobHandler.isThreadPoolFull()).isEqualTo(false);

    }

}