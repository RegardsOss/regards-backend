package fr.cnes.regards.framework.modules.jobs.service;

import fr.cnes.regards.framework.modules.jobs.dao.IJobInfoRepository;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.framework.modules.jobs.domain.JobStatus;
import fr.cnes.regards.framework.modules.jobs.test.JobTestConfiguration;
import org.junit.Assert;
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
import java.util.Set;

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
    public void test_clean_dead_jobs() throws InterruptedException {
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

    @Test
    public void test_clean_expired_jobs() {
        // Given
        // One job Info (RUNNING, expirationDate=now+1day) -> not expired
        createJobInfo(JobStatus.RUNNING, OffsetDateTime.now().plusDays(1), null);
        // One job Info (SUCCEEDED, stopDate=now-2days) -> expired
        createJobInfo(JobStatus.SUCCEEDED, OffsetDateTime.now().plusDays(1), OffsetDateTime.now().minusDays(2));
        // One job Info (RUNNING, expirationDate=now-1days) -> expired
        createJobInfo(JobStatus.RUNNING, OffsetDateTime.now().minusDays(1), null);
        // One job Info (FAILED, stopDate=now) -> not expired
        createJobInfo(JobStatus.FAILED, OffsetDateTime.now().minusDays(1), OffsetDateTime.now());
        // One job Info (FAILED, stopDate=now-30days) -> expired
        createJobInfo(JobStatus.FAILED, OffsetDateTime.now().minusDays(1), OffsetDateTime.now().minusDays(30));
        Assert.assertEquals(5L, jobInfoRepo.count());

        // When
        jobInfoService.cleanOutOfDateJobsOnTenant();

        // Then
        // It should remain :
        // - one succeed job cause stop date is too recent
        // - one failed job cause stop date is too recent
        Assert.assertEquals(2L, jobInfoRepo.count());
    }

    private void createJobInfo(JobStatus status, OffsetDateTime expirationDate, OffsetDateTime stopDate) {
        JobInfo jobInfo = new JobInfo(false, 1, Set.of(new JobParameter("toto", "titi")), "owner", "class");
        jobInfo.setExpirationDate(expirationDate);
        jobInfo.getStatus().setStatus(status);
        jobInfo.getStatus().setStopDate(stopDate);
        jobInfoRepo.save(jobInfo);
    }
}