package fr.cnes.regards.framework.modules.jobs.service;

import com.google.common.collect.BiMap;
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
import org.assertj.core.util.Lists;
import org.awaitility.Awaitility;
import org.junit.*;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Test of Jobs executions (status, pool, spring autowiring, ...)
 *
 * @author oroussel
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = { JobTestConfiguration.class })
@ActiveProfiles("test")
public class JobServiceIT {

    public static final String TENANT = "JOBS";

    private static final Logger LOGGER = LoggerFactory.getLogger(JobServiceIT.class);

    private static final Set<UUID> runnings = Collections.synchronizedSet(new HashSet<>());

    private static final Set<UUID> succeededs = Collections.synchronizedSet(new HashSet<>());

    private static final Set<UUID> aborteds = Collections.synchronizedSet(new HashSet<>());

    private static final Set<UUID> faileds = Collections.synchronizedSet(new HashSet<>());

    private final JobHandler jobHandler = new JobHandler();

    @Autowired
    private IJobService jobService;

    private BiMap<JobInfo, RunnableFuture<Void>> jobsMap;

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

    @Before
    public void setUp() {
        GsonUtil.setGson(gson);

        tenantResolver.forceTenant(TENANT);
        jobInfoRepos.deleteAll();

        if (!subscriptionsDone) {
            subscriber.subscribeTo(JobEvent.class, jobHandler);
            subscriptionsDone = true;
        }

        jobsMap = (BiMap<JobInfo, RunnableFuture<Void>>) ReflectionTestUtils.getField(jobService, "jobsMap");
        jobTestCleaner.startJobManager();
    }

    @After
    public void after() throws Exception {
        jobTestCleaner.cleanJob();
    }

    @Test
    public void testSucceeded() {
        JobInfo waitJobInfo = jobServiceJobCreator.createWaitJob(500L, 1, 10);
        waitJobInfo = jobInfoService.createAsQueued(waitJobInfo);
        UUID jobInfoId = waitJobInfo.getId();

        // Wait for job to terminate
        Awaitility.await().atMost(10, TimeUnit.SECONDS).until(() -> {
            tenantResolver.forceTenant(TENANT);
            return jobInfoRepos.countByStatusStatusIn(JobStatus.SUCCEEDED) == 1 && succeededs.contains(jobInfoId);
        });
        Assert.assertTrue(runnings.contains(jobInfoId));
        Assert.assertTrue(succeededs.contains(jobInfoId));
    }

    @Test
    public void testAbortion() throws InterruptedException {
        JobInfo waitJobInfo = jobServiceJobCreator.createWaitJob(1000L, 300, 10);
        jobInfoService.createAsQueued(waitJobInfo);
        UUID jobInfoId = waitJobInfo.getId();

        // TODO: This is not safe to stop a job while it's starting (see beforeExecute and afterExecute)
        // Wait jobs running in thread pool
        Awaitility.await().atMost(10, TimeUnit.SECONDS).until(() -> jobsMap.containsKey(waitJobInfo));

        LOGGER.info("ASK for " + jobInfoId + " TO BE STOPPED");
        jobInfoService.stopJob(jobInfoId);
        Awaitility.await().atMost(10, TimeUnit.SECONDS).until(() -> aborteds.contains(jobInfoId));

        Assert.assertFalse(succeededs.contains(jobInfoId));
        Assert.assertTrue(aborteds.contains(jobInfoId));
    }

    @Test
    public void testAborted() {
        JobInfo waitJobInfo = jobServiceJobCreator.createWaitJob(1000L, 300, 10);
        jobInfoService.createAsQueued(waitJobInfo);
        UUID jobInfoId = waitJobInfo.getId();

        // Wait jobs running in thread pool
        Awaitility.await().atMost(10, TimeUnit.SECONDS).until(() -> jobsMap.containsKey(waitJobInfo));

        Awaitility.await().atMost(5, TimeUnit.SECONDS).until(() -> runnings.contains(jobInfoId));
        Awaitility.await().atMost(5, TimeUnit.SECONDS).until(() -> {
            tenantResolver.forceTenant(TENANT);
            return jobInfoRepos.countByStatusStatusIn(JobStatus.RUNNING) == 1;
        });
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

        UUID failedJobId = failedJobInfo.getId();
        LOGGER.info("Failed job : {}", failedJobId);
        // Wait for job to terminate
        Awaitility.await().atMost(10, TimeUnit.SECONDS).until(() -> {
            tenantResolver.forceTenant(TENANT);
            return jobInfoRepos.findAllByStatusStatus(JobStatus.FAILED).size() == 1 && faileds.contains(failedJobId);
        });

        Assert.assertTrue(runnings.contains(failedJobId));
        Assert.assertFalse(succeededs.contains(failedJobId));
        Assert.assertTrue(faileds.contains(failedJobId));
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
        Collection<JobStatus> jobStatusesValid = Lists.newArrayList(JobStatus.QUEUED,
                                                                    JobStatus.TO_BE_RUN,
                                                                    JobStatus.RUNNING,
                                                                    JobStatus.SUCCEEDED);
        Assert.assertTrue("Unexpected jobToBeTriggered status",
                          jobStatusesValid.contains(jobToBeTriggeredUpdated.get().getStatus().getStatus()));
        Assert.assertEquals("Unexpected jobNotToBeTriggered status",
                            JobStatus.PENDING,
                            jobNotToBeTriggeredNotUpdated.get().getStatus().getStatus());
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
            return jobInfoRepos.countByStatusStatusIn(JobStatus.SUCCEEDED) == 1;
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