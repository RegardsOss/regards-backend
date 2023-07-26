package fr.cnes.regards.framework.modules.jobs.service;

import fr.cnes.regards.framework.modules.jobs.dao.IJobInfoRepository;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.domain.JobStatus;
import fr.cnes.regards.framework.modules.jobs.test.JobTestConfiguration;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;

import static org.junit.Assert.assertEquals;

/**
 * Test of Jobs executions (status, pool, spring autowiring, ...)
 *
 * @author oroussel
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = { JobTestConfiguration.class })
@ActiveProfiles({ "test", "noscheduler" })
public class JobSchedulerCleanerIT {

    @Autowired
    private IJobInfoRepository jobInfoRepo;

    @SpyBean
    private IJobInfoService jobInfoService;

    // number of time slots after that we consider a job is dead
    @Value("${regards.jobs.slot.number:2}")
    private int timeSlotNumber;

    @Before
    public void setUp() {
        jobInfoRepo.deleteAll();
    }

    @Test
    public void testCleaner() throws InterruptedException {
        // Given
        JobInfo fakeJobInfo = new JobInfo(false, 0, null, "", "Fake");
        fakeJobInfo.setLastHeartbeatDate(OffsetDateTime.now().minus(JobService.HEARTBEAT_DELAY, ChronoUnit.MILLIS));
        fakeJobInfo.updateStatus(JobStatus.RUNNING);
        jobInfoRepo.save(fakeJobInfo);
        Thread.sleep(JobService.HEARTBEAT_DELAY * timeSlotNumber);
        // When
        jobInfoService.cleanDeadJobs();
        // Then
        assertEquals(1, this.jobInfoService.retrieveJobs(JobStatus.FAILED).size());
    }
}