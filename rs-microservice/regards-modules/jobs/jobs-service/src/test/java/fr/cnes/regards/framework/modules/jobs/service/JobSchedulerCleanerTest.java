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
        fake.setLastCompletionUpdate(OffsetDateTime.now().minus(updateCompletionPeriod, ChronoUnit.MILLIS));
        fake.updateStatus(JobStatus.RUNNING);
        jobInfoRepo.save(fake);
        Thread.sleep(updateCompletionPeriod * timeSlotNumber);
        jobService.cleanDeadJobs();
        assertEquals(1, this.jobInfoService.retrieveJobs(JobStatus.FAILED).size());
    }

    @Test
    public void testUpdateCompletion() throws InterruptedException {
        int waitCount = 4;
        long wait = 500L;
        OffsetDateTime beforeJob = OffsetDateTime.now();
        JobInfo waitJobInfo = new JobInfo(false);
        waitJobInfo.setPriority(10);
        waitJobInfo.setClassName(WaiterJob.class.getName());
        waitJobInfo.setParameters(new JobParameter(WaiterJob.WAIT_PERIOD, wait),
                                  new JobParameter(WaiterJob.WAIT_PERIOD_COUNT, waitCount));
        waitJobInfo = jobInfoService.createAsQueued(waitJobInfo);
        waitJobInfo.updateStatus(JobStatus.TO_BE_RUN);
        waitJobInfo = jobInfoRepo.save(waitJobInfo);
        jobService.runJob(waitJobInfo, tenant);
        // Wait for job to start
        while (jobInfoRepo.findAllByStatusStatus(JobStatus.RUNNING).size() < 1) {
            Thread.sleep(1);
        }
        Thread.sleep((waitCount + 1) * wait);
        Assert.assertEquals("WaiterJob should be finished",
                            JobStatus.SUCCEEDED,
                            jobInfoRepo.findCompleteById(waitJobInfo.getId()).getStatus().getStatus());
        // now lets check that lastCompletionUpdate is updated properly
        Mockito.verify(jobService, Mockito.times(waitCount-1)).updateCurrentJobsCompletions();
        // there is one less JobInfoService#updateJobInfosCompletion than JobService#updateCurrentJobsCompletions
        // because JobService#updateCurrentJobsCompletions will be called when there is not jobs
        // moreover, JobInfoService#updateJobInfosCompletion is called 2 two less time that waitCount because during
        // the first iteration of WaiterJob, it has not been updated yet and during last iteration is will end before it could be checked
        Mockito.verify(jobInfoService, Mockito.times(waitCount-2)).updateJobInfosCompletion(Mockito.anyIterable());
    }
}