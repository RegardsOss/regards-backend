package fr.cnes.regards.framework.modules.jobs.service;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import fr.cnes.regards.framework.modules.jobs.dao.IJobInfoRepository;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.framework.modules.jobs.domain.JobStatus;
import fr.cnes.regards.framework.modules.jobs.domain.WaiterJob;
import fr.cnes.regards.framework.modules.jobs.test.JobConfiguration;
import static org.junit.Assert.assertEquals;

/**
 * Test of Jobs executions (status, pool, spring autowiring, ...)
 * @author oroussel
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = { JobConfiguration.class })
@ActiveProfiles("noscheduler")
public class JobSchedulerCleanerTest {

    @Autowired
    private IJobInfoRepository jobInfoRepo;

    @SpyBean
    private IJobInfoService jobInfoService;

    @SpyBean
    private IJobService jobService;

    // number of time slots after that we consider a job is dead
    @Value("${regards.jobs.slot.number:2}")
    private int timeSlotNumber;

    @Value("${regards.jobs.completion.update.rate.ms}")
    private int updateCompletionPeriod;

    @Value("${regards.tenant}")
    private String tenant;

    @Before
    public void setUp() {
        jobInfoRepo.deleteAll();
    }

    @Test
    public void testCleaner() throws InterruptedException {
        JobInfo fake = new JobInfo(false, 0, null, "", "Fake");
        fake.setLastHeartbeatDate(OffsetDateTime.now().minus(JobService.HEARTBEAT_DELAY, ChronoUnit.MILLIS));
        fake.updateStatus(JobStatus.RUNNING);
        jobInfoRepo.save(fake);
        Thread.sleep(JobService.HEARTBEAT_DELAY * timeSlotNumber);
        jobInfoService.cleanDeadJobs();
        assertEquals(1, this.jobInfoService.retrieveJobs(JobStatus.FAILED).size());
    }
}