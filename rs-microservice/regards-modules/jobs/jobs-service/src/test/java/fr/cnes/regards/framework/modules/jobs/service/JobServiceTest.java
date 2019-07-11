package fr.cnes.regards.framework.modules.jobs.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
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

import com.google.gson.Gson;
import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.amqp.configuration.IRabbitVirtualHostAdmin;
import fr.cnes.regards.framework.amqp.domain.IHandler;
import fr.cnes.regards.framework.amqp.domain.TenantWrapper;
import fr.cnes.regards.framework.jpa.json.GsonUtil;
import fr.cnes.regards.framework.modules.jobs.dao.IJobInfoRepository;
import fr.cnes.regards.framework.modules.jobs.domain.FailedAfter1sJob;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.framework.modules.jobs.domain.JobStatus;
import fr.cnes.regards.framework.modules.jobs.domain.SpringJob;
import fr.cnes.regards.framework.modules.jobs.domain.WaiterJob;
import fr.cnes.regards.framework.modules.jobs.domain.event.JobEvent;
import fr.cnes.regards.framework.modules.jobs.domain.event.JobEventType;
import fr.cnes.regards.framework.modules.jobs.test.JobConfiguration;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;

/**
 * Test of Jobs executions (status, pool, spring autowiring, ...)
 * @author oroussel
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = { JobConfiguration.class })
@Ignore
public class JobServiceTest {

    public static final String TENANT = "JOBS";

    private static final Logger LOGGER = LoggerFactory.getLogger(JobServiceTest.class);

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
    private IRabbitVirtualHostAdmin rabbitVhostAdmin;

    @Autowired
    private ISubscriber subscriber;

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

        if (!subscriptionsDone) {
            subscriber.subscribeTo(JobEvent.class, jobHandler);
            subscriptionsDone = true;
        }
        springPublisher.publishEvent(new ApplicationReadyEvent(Mockito.mock(SpringApplication.class), null, null));
    }

    @After
    public void tearDown() {
        tenantResolver.forceTenant(TENANT);
        jobInfoRepos.deleteAll();
    }

    @Test
    public void testSucceeded() throws InterruptedException {
        JobInfo waitJobInfo = new JobInfo(false);
        waitJobInfo.setPriority(10);
        waitJobInfo.setClassName(WaiterJob.class.getName());
        waitJobInfo.setParameters(new JobParameter(WaiterJob.WAIT_PERIOD, 500L),
                                  new JobParameter(WaiterJob.WAIT_PERIOD_COUNT, 1));
        waitJobInfo = jobInfoService.createAsQueued(waitJobInfo);

        // Wait for job to terminate
        while (jobInfoRepos.findAllByStatusStatus(JobStatus.SUCCEEDED).size() < 1) {
            Thread.sleep(1_000);
        }
        Assert.assertTrue(runnings.contains(waitJobInfo.getId()));
        Assert.assertTrue(succeededs.contains(waitJobInfo.getId()));
    }

    @Test
    public void testAbortion() throws InterruptedException {
        JobInfo waitJobInfo = new JobInfo(false);
        waitJobInfo.setPriority(10);
        waitJobInfo.setClassName(WaiterJob.class.getName());
        waitJobInfo.setParameters(new JobParameter(WaiterJob.WAIT_PERIOD, 1000L),
                                  new JobParameter(WaiterJob.WAIT_PERIOD_COUNT, 3));
        waitJobInfo = jobInfoService.createAsQueued(waitJobInfo);
        jobInfoService.stopJob(waitJobInfo.getId());
        LOGGER.info("ASK for " + waitJobInfo.getId() + " TO BE STOPPED");
        Thread.sleep(1_500);
        Assert.assertFalse(succeededs.contains(waitJobInfo.getId()));
        Assert.assertTrue(aborteds.contains(waitJobInfo.getId()));
    }

    @Test
    public void testAborted() throws InterruptedException {
        JobInfo waitJobInfo = new JobInfo(false);
        waitJobInfo.setPriority(10);
        waitJobInfo.setClassName(WaiterJob.class.getName());
        waitJobInfo.setParameters(new JobParameter(WaiterJob.WAIT_PERIOD, 1000L),
                                  new JobParameter(WaiterJob.WAIT_PERIOD_COUNT, 3));
        waitJobInfo = jobInfoService.createAsQueued(waitJobInfo);

        Thread.sleep(2_000);
        LOGGER.info("ASK for " + waitJobInfo.getId() + " TO BE STOPPED");
        jobInfoService.stopJob(waitJobInfo.getId());
        Thread.sleep(1_500);
        Assert.assertTrue(runnings.contains(waitJobInfo.getId()));
        Assert.assertFalse(succeededs.contains(waitJobInfo.getId()));
        Assert.assertTrue(aborteds.contains(waitJobInfo.getId()));
    }

    @Test
    public void testFailed() throws InterruptedException {
        JobInfo failedJobInfo = new JobInfo(false);
        failedJobInfo.setPriority(10);
        failedJobInfo.setClassName(FailedAfter1sJob.class.getName());
        failedJobInfo = jobInfoService.createAsQueued(failedJobInfo);

        LOGGER.info("Failed job : {}", failedJobInfo.getId());

        // Wait for job to terminate
        while (jobInfoRepos.findAllByStatusStatus(JobStatus.FAILED).size() < 1) {
            Thread.sleep(1_000);
        }
        Assert.assertTrue(runnings.contains(failedJobInfo.getId()));
        Assert.assertFalse(succeededs.contains(failedJobInfo.getId()));
        Assert.assertTrue(faileds.contains(failedJobInfo.getId()));
    }

    @Test
    public void testPool() throws InterruptedException {
        // Create 6 waitJob
        JobInfo[] jobInfos = new JobInfo[6];
        for (int i = 0; i < jobInfos.length; i++) {
            jobInfos[i] = new JobInfo(false);
            jobInfos[i].setPriority(20 - i); // Makes it easier to know which ones are launched first
            jobInfos[i].setClassName(WaiterJob.class.getName());
            jobInfos[i].setParameters(new JobParameter(WaiterJob.WAIT_PERIOD, 1000L),
                                      new JobParameter(WaiterJob.WAIT_PERIOD_COUNT, 2));
        }
        for (int i = 0; i < jobInfos.length; i++) {
            jobInfos[i] = jobInfoService.createAsQueued(jobInfos[i]);
        }
        try {
            // Wait to be sure jobs are treated by pool
            Thread.sleep(7_000);
            // Only poolSide jobs should be runnings
            Assert.assertEquals(jobInfos.length, jobInfoRepos.findAllByStatusStatus(JobStatus.SUCCEEDED).size());
        } finally {
            // Wait for all jobs to terminate
            while (jobInfoRepos.findAllByStatusStatus(JobStatus.SUCCEEDED).size() < jobInfos.length) {
                Thread.sleep(1_000);
            }
        }
        // Retrieve all jobs
        List<JobInfo> results = new ArrayList<>(jobInfoRepos.findAllByStatusStatus(JobStatus.SUCCEEDED));
        // sort by stopDate
        results.sort(Comparator.comparing(j -> j.getStatus().getStopDate()));
        int lastPriority = -1;
        for (JobInfo job : results) {
            if (lastPriority != -1) {
                Assert.assertTrue(job.getPriority() <= lastPriority);
            }
            lastPriority = job.getPriority();
        }
    }

    @Test
    public void testSpringJob() throws InterruptedException {
        JobInfo springJobInfo = new JobInfo(false);
        springJobInfo.setPriority(100);
        springJobInfo.setClassName(SpringJob.class.getName());

        jobInfoService.createAsQueued(springJobInfo);

        // Wait for job to terminate
        while (jobInfoRepos.findAllByStatusStatus(JobStatus.SUCCEEDED).size() < 1) {
            Thread.sleep(1_000);
        }
    }

    private class JobHandler implements IHandler<JobEvent> {

        @Override
        public void handle(TenantWrapper<JobEvent> wrapper) {
            JobEvent event = wrapper.getContent();
            JobEventType type = event.getJobEventType();
            switch (type) {
                case RUNNING:
                    runnings.add(wrapper.getContent().getJobId());
                    LOGGER.info("RUNNING for " + wrapper.getContent().getJobId());
                    break;
                case SUCCEEDED:

                    succeededs.add(wrapper.getContent().getJobId());
                    LOGGER.info("SUCCEEDED for " + wrapper.getContent().getJobId());
                    break;
                case ABORTED:
                    aborteds.add(wrapper.getContent().getJobId());
                    LOGGER.info("ABORTED for " + wrapper.getContent().getJobId());
                    break;
                case FAILED:
                    faileds.add(wrapper.getContent().getJobId());
                    LOGGER.info("FAILED for " + wrapper.getContent().getJobId());
                    break;
                default:
                    throw new IllegalArgumentException(
                            type + " is not an handled type of JobEvent for this test: " + JobServiceTest.class
                                    .getSimpleName());
            }
        }
    }
}