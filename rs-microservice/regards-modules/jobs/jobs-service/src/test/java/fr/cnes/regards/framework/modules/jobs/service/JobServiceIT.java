package fr.cnes.regards.framework.modules.jobs.service;

import com.google.gson.Gson;
import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.amqp.domain.IHandler;
import fr.cnes.regards.framework.jpa.json.GsonUtil;
import fr.cnes.regards.framework.modules.jobs.dao.IJobInfoRepository;
import fr.cnes.regards.framework.modules.jobs.domain.FailedAfter1sJob;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.domain.JobStatus;
import fr.cnes.regards.framework.modules.jobs.domain.SpringJob;
import fr.cnes.regards.framework.modules.jobs.domain.event.JobEvent;
import fr.cnes.regards.framework.modules.jobs.domain.event.JobEventType;
import fr.cnes.regards.framework.modules.jobs.task.JobInfoTaskScheduler;
import fr.cnes.regards.framework.modules.jobs.test.JobTestConfiguration;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.test.util.JUnitLogRule;
import org.awaitility.Awaitility;
import org.junit.*;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Test of Jobs executions (status, pool, spring autowiring, ...)
 *
 * @author oroussel
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = { JobTestConfiguration.class })
public class JobServiceIT {

    public static final String TENANT = "JOBS";

    private static final Logger LOGGER = LoggerFactory.getLogger(JobServiceIT.class);

    private static Set<UUID> runnings = Collections.synchronizedSet(new HashSet<>());

    private static Set<UUID> succeededs = Collections.synchronizedSet(new HashSet<>());

    private static Set<UUID> aborteds = Collections.synchronizedSet(new HashSet<>());

    private static Set<UUID> faileds = Collections.synchronizedSet(new HashSet<>());

    private final JobHandler jobHandler = new JobHandler();

    @Autowired
    private IJobInfoService jobInfoService;

    @Autowired
    private IJobInfoRepository jobInfoRepos;

    @Autowired
    private IRuntimeTenantResolver tenantResolver;

    @Autowired
    private ISubscriber subscriber;

    @Autowired
    private JobInfoTaskScheduler jobInfoTaskScheduler;

    @Autowired
    private JobTestCleaner jobTestCleaner;

    @Autowired
    private JobServiceJobCreator jobServiceJobCreator;

    @Rule
    public JUnitLogRule rule = new JUnitLogRule();

    @Value("${regards.jobs.pool.size:10}")
    private int poolSize;

    private boolean subscriptionsDone = false;

    @Autowired
    private Gson gson;

    @Autowired
    private ApplicationEventPublisher springPublisher;

    @Before
    public void setUp() {
        GsonUtil.setGson(gson);

        tenantResolver.forceTenant(TENANT);
        jobInfoRepos.deleteAll();

        if (!subscriptionsDone) {
            subscriber.subscribeTo(JobEvent.class, jobHandler);
            subscriptionsDone = true;
        }
        springPublisher.publishEvent(new ApplicationReadyEvent(Mockito.mock(SpringApplication.class),
                                                               null,
                                                               null,
                                                               null));
    }

    @After
    public void after() throws Exception {
        jobTestCleaner.cleanJob();
    }

    @Test
    public void testSucceeded() {
        JobInfo waitJobInfo = jobServiceJobCreator.createWaitJob(500L, 1, 10);
        waitJobInfo = jobInfoService.createAsQueued(waitJobInfo);

        // Wait for job to terminate
        Awaitility.await().atMost(10, TimeUnit.SECONDS).until(() -> {
            tenantResolver.forceTenant(TENANT);
            return jobInfoRepos.findAllByStatusStatus(JobStatus.SUCCEEDED).size() == 1;
        });
        Assert.assertTrue(runnings.contains(waitJobInfo.getId()));
        Assert.assertTrue(succeededs.contains(waitJobInfo.getId()));
    }

    @Test
    public void testAbortion() {
        JobInfo waitJobInfo = jobServiceJobCreator.createWaitJob(1000L, 3, 10);
        jobInfoService.createAsQueued(waitJobInfo);
        UUID jobInfoId = waitJobInfo.getId();

        jobInfoService.stopJob(jobInfoId);
        LOGGER.info("ASK for " + jobInfoId + " TO BE STOPPED");
        Awaitility.await().atMost(10, TimeUnit.SECONDS).until(() -> aborteds.contains(jobInfoId));

        Assert.assertFalse(succeededs.contains(jobInfoId));
        Assert.assertTrue(aborteds.contains(jobInfoId));
    }

    @Test
    public void testAborted() {
        JobInfo waitJobInfo = jobServiceJobCreator.createWaitJob(1000L, 300, 10);
        jobInfoService.createAsQueued(waitJobInfo);
        UUID jobInfoId = waitJobInfo.getId();

        Awaitility.await().atMost(5, TimeUnit.SECONDS).until(() -> runnings.contains(jobInfoId));
        LOGGER.info("ASK for " + jobInfoId + " TO BE STOPPED");
        jobInfoService.stopJob(jobInfoId);

        Awaitility.await().atMost(10, TimeUnit.SECONDS).until(() -> aborteds.contains(jobInfoId));
        Assert.assertTrue(runnings.contains(jobInfoId));
        Assert.assertFalse(succeededs.contains(jobInfoId));
        Assert.assertTrue(aborteds.contains(jobInfoId));
    }

    @Test
    public void testFailed() {
        JobInfo failedJobInfo = new JobInfo(false);
        failedJobInfo.setPriority(10);
        failedJobInfo.setClassName(FailedAfter1sJob.class.getName());
        failedJobInfo = jobInfoService.createAsQueued(failedJobInfo);

        LOGGER.info("Failed job : {}", failedJobInfo.getId());
        // Wait for job to terminate
        Awaitility.await().atMost(10, TimeUnit.SECONDS).until(() -> {
            tenantResolver.forceTenant(TENANT);
            return jobInfoRepos.findAllByStatusStatus(JobStatus.FAILED).size() == 1;
        });

        Assert.assertTrue(runnings.contains(failedJobInfo.getId()));
        Assert.assertFalse(succeededs.contains(failedJobInfo.getId()));
        Assert.assertTrue(faileds.contains(failedJobInfo.getId()));
    }

    @Test
    public void testPendingTrigger() {
        OffsetDateTime triggerDate = OffsetDateTime.now();
        JobInfo jobToBeTriggered = jobInfoService.createPendingTriggerJob(jobServiceJobCreator.createWaitJob(1L, 1, 10),
                                                                          triggerDate);
        JobInfo jobNotToBeTriggered = jobInfoService.createPendingTriggerJob(jobServiceJobCreator.createWaitJob(1L,
                                                                                                                1,
                                                                                                                10),
                                                                             triggerDate.plusDays(1));
        jobInfoTaskScheduler.triggerPendingJobs();
        // check the status of jobToBeTriggered has changed to QUEUED or TO_BE_RUN (triggerPendingJobs changes the
        // status to QUEUED and JobService#manage changes it to TO_BE_RUN)
        // the status of jobNotToBeTriggered is still the same because the trigger date is not expired
        Optional<JobInfo> jobToBeTriggeredUpdated = jobInfoRepos.findById(jobToBeTriggered.getId());
        Optional<JobInfo> jobNotToBeTriggeredNotUpdated = jobInfoRepos.findById(jobNotToBeTriggered.getId());
        Assert.assertTrue("jobToBeTriggered should be present", jobToBeTriggeredUpdated.isPresent());
        Assert.assertTrue("jobNotToBeTriggered should be present", jobNotToBeTriggeredNotUpdated.isPresent());
        Assert.assertTrue("Unexpected jobToBeTriggered status",
                          jobToBeTriggeredUpdated.get().getStatus().getStatus().equals(JobStatus.QUEUED)
                          || jobToBeTriggeredUpdated.get().getStatus().getStatus().equals(JobStatus.TO_BE_RUN));
        Assert.assertEquals("Unexpected jobNotToBeTriggered status",
                            JobStatus.PENDING,
                            jobNotToBeTriggeredNotUpdated.get().getStatus().getStatus());
    }

    @Test
    public void testPool() {
        // Create 6 waitJob
        JobInfo[] jobInfos = jobServiceJobCreator.runWaitJobs();

        // Wait to be sure jobs are treated by pool
        Awaitility.await().atMost(10, TimeUnit.SECONDS).until(() -> {
            tenantResolver.forceTenant(TENANT);
            // Only poolSize jobs should be runnings
            return jobInfoRepos.findAllByStatusStatus(JobStatus.SUCCEEDED).size() == jobInfos.length;
        });
        // Retrieve all jobs
        List<JobInfo> results = new ArrayList<>(jobInfoRepos.findAllByStatusStatus(JobStatus.SUCCEEDED));
        // sort by startDate to make sure the top priority jobs were executed first
        results.sort(Comparator.comparing(j -> j.getStatus().getStartDate()));
        LOGGER.info(results.toString());
        int lastPriority = -1;
        for (JobInfo job : results) {
            LOGGER.info("Job {} start date at {} with priority {}",
                        job.getId(),
                        job.getStatus().getStartDate(),
                        job.getPriority());
            if (lastPriority != -1) {
                Assert.assertTrue("The jobs were not launched by top priority", job.getPriority() <= lastPriority);
            }
            lastPriority = job.getPriority();
        }
    }

    @Test
    public void testSpringJob() {
        JobInfo springJobInfo = new JobInfo(false);
        springJobInfo.setPriority(100);
        springJobInfo.setClassName(SpringJob.class.getName());

        jobInfoService.createAsQueued(springJobInfo);

        // Wait for job to terminate
        Awaitility.await().atMost(10, TimeUnit.SECONDS).until(() -> {
            tenantResolver.forceTenant(TENANT);
            return jobInfoRepos.findAllByStatusStatus(JobStatus.SUCCEEDED).size() == 1;
        });
    }

    private class JobHandler implements IHandler<JobEvent> {

        @Override
        public void handle(String tenant, JobEvent jobEvent) {
            JobEventType type = jobEvent.getJobEventType();
            switch (type) {
                case RUNNING:
                    runnings.add(jobEvent.getJobId());
                    LOGGER.info("RUNNING for " + jobEvent.getJobId());
                    break;
                case SUCCEEDED:

                    succeededs.add(jobEvent.getJobId());
                    LOGGER.info("SUCCEEDED for " + jobEvent.getJobId());
                    break;
                case ABORTED:
                    aborteds.add(jobEvent.getJobId());
                    LOGGER.info("ABORTED for " + jobEvent.getJobId());
                    break;
                case FAILED:
                    faileds.add(jobEvent.getJobId());
                    LOGGER.info("FAILED for " + jobEvent.getJobId());
                    break;
                default:
                    throw new IllegalArgumentException(type
                                                       + " is not an handled type of JobEvent for this test: "
                                                       + JobServiceIT.class.getSimpleName());
            }
        }
    }
}