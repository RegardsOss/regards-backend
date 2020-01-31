package fr.cnes.regards.framework.modules.jobs.service;

import static org.junit.Assert.assertEquals;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import fr.cnes.regards.framework.modules.jobs.dao.IJobInfoRepository;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.domain.JobStatus;
import fr.cnes.regards.framework.modules.jobs.test.JobConfiguration;

/**
 * Test of Jobs executions (status, pool, spring autowiring, ...)
 * @author oroussel
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = { JobConfiguration.class })
@Ignore
public class JobSchedulerCleanerTest {

    @Autowired
    private IJobInfoRepository jobInfoRepo;

    @Autowired
    private IJobInfoService jobInfoService;

    // number of time slots after that we consider a job is dead
    @Value("${regards.jobs.slot.number:2}")
    private int timeSlotNumber;

    @Value("${regards.job.cleaner.scheduling.delay:1000}")
    private int deadJobSchedulerPeriod;

    @Before
    public void setUp() {
        jobInfoRepo.deleteAll();
    }

    @Test
    public void testCleaner() throws InterruptedException {
        JobInfo fake = new JobInfo(false, 0, null, "", "Fake");
        fake.setLastCompletionUpdate(OffsetDateTime.now().minus(deadJobSchedulerPeriod, ChronoUnit.MILLIS));
        Thread.sleep(deadJobSchedulerPeriod * timeSlotNumber);
        assertEquals(1, this.jobInfoService.retrieveJobs(JobStatus.FAILED));
    }

}