package fr.cnes.regards.framework.modules.jobs.service;

import com.google.gson.Gson;
import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.amqp.domain.IHandler;
import fr.cnes.regards.framework.amqp.domain.TenantWrapper;
import fr.cnes.regards.framework.jpa.json.GsonUtil;
import fr.cnes.regards.framework.modules.jobs.dao.IJobInfoRepository;
import fr.cnes.regards.framework.modules.jobs.domain.*;
import fr.cnes.regards.framework.modules.jobs.domain.event.JobEvent;
import fr.cnes.regards.framework.modules.jobs.domain.event.JobEventType;
import fr.cnes.regards.framework.modules.jobs.test.JobMultitenantConfiguration;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import org.junit.*;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * @author oroussel
 */
@Ignore("Continuous integration fails! To do.")
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = { JobMultitenantConfiguration.class })
@ActiveProfiles("test")
public class MultitenantJobIT {

    private static final Logger LOGGER = LoggerFactory.getLogger(MultitenantJobIT.class);

    public static final String TENANT1 = "JOBS1";

    public static final String TENANT2 = "JOBS2";

    @Autowired
    private IJobInfoService jobInfoService;

    @Autowired
    private IJobInfoRepository jobInfoRepos;

    @Autowired
    private IRuntimeTenantResolver tenantResolver;

    @Autowired
    private ISubscriber subscriber;

    private static Set<UUID> runnings = Collections.synchronizedSet(new HashSet<>());

    private static Set<UUID> succeededs = Collections.synchronizedSet(new HashSet<>());

    private static Set<UUID> aborteds = Collections.synchronizedSet(new HashSet<>());

    private static Set<UUID> faileds = Collections.synchronizedSet(new HashSet<>());

    @Value("${regards.jobs.pool.size:10}")
    private int poolSize;

    private final MultitenantJobIT.JobHandler jobHandler = new MultitenantJobIT.JobHandler();

    private boolean subscriptionsDone = false;

    @Autowired
    private Gson gson;

    @Before
    public void setUp() {
        GsonUtil.setGson(gson);
        // tenantResolver.forceTenant(TENANT1);
        //
        // rabbitVhostAdmin.bind(tenantResolver.getTenant());
        //
        // try {
        // amqpAdmin.purgeQueue(StopJobEvent.class,
        // (Class<IHandler<StopJobEvent>>) Class
        // .forName("fr.cnes.regards.framework.modules.jobs.service.JobService$StopJobHandler"),
        // false);
        // amqpAdmin.purgeQueue(JobEvent.class, jobHandler.getClass(), false);
        // } catch (Exception e) {
        // // In case queues don't exist
        // }
        // rabbitVhostAdmin.unbind();
        //
        // tenantResolver.forceTenant(TENANT2);
        //
        // rabbitVhostAdmin.bind(tenantResolver.getTenant());
        //
        // try {
        // amqpAdmin.purgeQueue(StopJobEvent.class,
        // (Class<IHandler<StopJobEvent>>) Class
        // .forName("fr.cnes.regards.framework.modules.jobs.service.JobService$StopJobHandler"),
        // false);
        // amqpAdmin.purgeQueue(JobEvent.class, jobHandler.getClass(), false);
        // } catch (Exception e) {
        // // In case queues don't exist
        // }
        // rabbitVhostAdmin.unbind();

        if (!subscriptionsDone) {
            subscriber.subscribeTo(JobEvent.class, jobHandler);
            subscriptionsDone = true;
        }
    }

    @After
    public void tearDown() {
        tenantResolver.forceTenant(TENANT1);
        jobInfoRepos.deleteAll();
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
                    throw new IllegalArgumentException(type
                                                       + " is not an handled type of JobEvent for this test: "
                                                       + JobServiceIT.class.getSimpleName());
            }
        }
    }

    @Test
    public void testSucceeded() throws InterruptedException {
        tenantResolver.forceTenant(TENANT1);
        JobInfo waitJobInfo1 = new JobInfo(false);
        waitJobInfo1.setPriority(10);
        waitJobInfo1.setClassName(WaiterJob.class.getName());
        waitJobInfo1.setParameters(new JobParameter(WaiterJob.WAIT_PERIOD, 500L),
                                   new JobParameter(WaiterJob.WAIT_PERIOD_COUNT, 1));
        waitJobInfo1 = jobInfoService.createAsQueued(waitJobInfo1);

        tenantResolver.forceTenant(TENANT2);
        JobInfo waitJobInfo2 = new JobInfo(false);
        waitJobInfo2.setPriority(10);
        waitJobInfo2.setClassName(WaiterJob.class.getName());
        waitJobInfo2.setParameters(new JobParameter(WaiterJob.WAIT_PERIOD, 500L),
                                   new JobParameter(WaiterJob.WAIT_PERIOD_COUNT, 1));
        waitJobInfo2 = jobInfoService.createAsQueued(waitJobInfo2);

        int cpt = 0;
        tenantResolver.forceTenant(TENANT1);
        // Wait for job to terminate
        while (jobInfoRepos.findAllByStatusStatus(JobStatus.SUCCEEDED).size() < 1) {
            Thread.sleep(1_000);
            cpt++;
            if (cpt == 10) {
                Assert.fail();
            }
        }
        cpt = 0;
        tenantResolver.forceTenant(TENANT2);
        // Wait for job to terminate
        while (jobInfoRepos.findAllByStatusStatus(JobStatus.SUCCEEDED).size() < 1) {
            Thread.sleep(1_000);
            if (cpt == 10) {
                Assert.fail();
            }

        }
    }

    @Test
    public void testAbortion() throws InterruptedException {
        tenantResolver.forceTenant(TENANT1);
        JobInfo waitJobInfo1 = new JobInfo(false);
        waitJobInfo1.setPriority(10);
        waitJobInfo1.setClassName(WaiterJob.class.getName());
        waitJobInfo1.setParameters(new JobParameter(WaiterJob.WAIT_PERIOD, 1000L),
                                   new JobParameter(WaiterJob.WAIT_PERIOD_COUNT, 3));

        tenantResolver.forceTenant(TENANT2);
        JobInfo waitJobInfo2 = new JobInfo(false);
        waitJobInfo2.setPriority(10);
        waitJobInfo2.setClassName(WaiterJob.class.getName());
        waitJobInfo2.setParameters(new JobParameter(WaiterJob.WAIT_PERIOD, 1000L),
                                   new JobParameter(WaiterJob.WAIT_PERIOD_COUNT, 3));

        tenantResolver.forceTenant(TENANT1);
        waitJobInfo1 = jobInfoService.createAsQueued(waitJobInfo1);
        jobInfoService.stopJob(waitJobInfo1.getId());
        LOGGER.info("ASK for " + waitJobInfo1.getId() + " TO BE STOPPED");

        tenantResolver.forceTenant(TENANT2);
        waitJobInfo2 = jobInfoService.createAsQueued(waitJobInfo2);
        jobInfoService.stopJob(waitJobInfo2.getId());
        LOGGER.info("ASK for " + waitJobInfo2.getId() + " TO BE STOPPED");

        Thread.sleep(1_500);
        Assert.assertFalse(succeededs.contains(waitJobInfo1.getId()));
        Assert.assertTrue(aborteds.contains(waitJobInfo1.getId()));
        Assert.assertFalse(succeededs.contains(waitJobInfo2.getId()));
        Assert.assertTrue(aborteds.contains(waitJobInfo2.getId()));
    }

    @Test
    public void testAborted() throws InterruptedException {
        tenantResolver.forceTenant(TENANT1);
        JobInfo waitJobInfo1 = new JobInfo(false);
        waitJobInfo1.setPriority(10);
        waitJobInfo1.setClassName(WaiterJob.class.getName());
        waitJobInfo1.setParameters(new JobParameter(WaiterJob.WAIT_PERIOD, 1000L),
                                   new JobParameter(WaiterJob.WAIT_PERIOD_COUNT, 3));

        tenantResolver.forceTenant(TENANT2);
        JobInfo waitJobInfo2 = new JobInfo(false);
        waitJobInfo2.setPriority(10);
        waitJobInfo2.setClassName(WaiterJob.class.getName());
        waitJobInfo2.setParameters(new JobParameter(WaiterJob.WAIT_PERIOD, 1000L),
                                   new JobParameter(WaiterJob.WAIT_PERIOD_COUNT, 3));

        waitJobInfo2 = jobInfoService.createAsQueued(waitJobInfo2);
        tenantResolver.forceTenant(TENANT1);
        waitJobInfo1 = jobInfoService.createAsQueued(waitJobInfo1);

        Thread.sleep(2_000);
        LOGGER.info("ASK for " + waitJobInfo1.getId() + " TO BE STOPPED");
        jobInfoService.stopJob(waitJobInfo1.getId());

        tenantResolver.forceTenant(TENANT2);
        LOGGER.info("ASK for " + waitJobInfo2.getId() + " TO BE STOPPED");
        jobInfoService.stopJob(waitJobInfo2.getId());

        Thread.sleep(1_500);
        // Do not test runnings, in some case the job can be aborted before its execution
        Assert.assertFalse(succeededs.contains(waitJobInfo2.getId()));
        Assert.assertTrue(aborteds.contains(waitJobInfo2.getId()));
        Assert.assertFalse(succeededs.contains(waitJobInfo1.getId()));
        Assert.assertTrue(aborteds.contains(waitJobInfo1.getId()));
    }

    @Test
    public void testFailed() throws InterruptedException {
        tenantResolver.forceTenant(TENANT1);
        JobInfo failedJobInfo1 = new JobInfo(false);
        failedJobInfo1.setPriority(10);
        failedJobInfo1.setClassName(FailedAfter1sJob.class.getName());
        failedJobInfo1 = jobInfoService.createAsQueued(failedJobInfo1);

        tenantResolver.forceTenant(TENANT2);
        JobInfo failedJobInfo2 = new JobInfo(false);
        failedJobInfo2.setPriority(10);
        failedJobInfo2.setClassName(FailedAfter1sJob.class.getName());
        failedJobInfo2 = jobInfoService.createAsQueued(failedJobInfo2);

        LOGGER.info("Failed job : {}", failedJobInfo1.getId());
        LOGGER.info("Failed job : {}", failedJobInfo2.getId());

        tenantResolver.forceTenant(TENANT1);
        // Wait for job to terminate
        while (jobInfoRepos.findAllByStatusStatus(JobStatus.FAILED).size() < 1) {
            Thread.sleep(1_000);
        }
        tenantResolver.forceTenant(TENANT2);
        // Wait for job to terminate
        while (jobInfoRepos.findAllByStatusStatus(JobStatus.FAILED).size() < 1) {
            Thread.sleep(1_000);
        }

        Assert.assertTrue(runnings.contains(failedJobInfo1.getId()));
        Assert.assertFalse(succeededs.contains(failedJobInfo1.getId()));
        Assert.assertTrue(faileds.contains(failedJobInfo1.getId()));

        Assert.assertTrue(runnings.contains(failedJobInfo2.getId()));
        Assert.assertFalse(succeededs.contains(failedJobInfo2.getId()));
        Assert.assertTrue(faileds.contains(failedJobInfo2.getId()));
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
        tenantResolver.forceTenant(TENANT1);
        for (int i = 0; i < jobInfos.length / 2; i++) {
            jobInfos[i] = jobInfoService.createAsQueued(jobInfos[i]);
        }
        tenantResolver.forceTenant(TENANT2);
        for (int i = jobInfos.length / 2; i < jobInfos.length; i++) {
            jobInfos[i] = jobInfoService.createAsQueued(jobInfos[i]);
        }
        try {
            // Wait to be sure jobs are treated by pool
            Thread.sleep(10_000);
            // Only poolSide jobs should be runnings
            tenantResolver.forceTenant(TENANT1);
            Assert.assertEquals(jobInfos.length / 2, jobInfoRepos.findAllByStatusStatus(JobStatus.SUCCEEDED).size());
            tenantResolver.forceTenant(TENANT2);
            Assert.assertEquals(jobInfos.length / 2, jobInfoRepos.findAllByStatusStatus(JobStatus.SUCCEEDED).size());
        } finally {
            // Wait for all jobs to terminate
            tenantResolver.forceTenant(TENANT1);
            while (jobInfoRepos.findAllByStatusStatus(JobStatus.SUCCEEDED).size() < jobInfos.length / 2) {
                Thread.sleep(1_000);
            }
            tenantResolver.forceTenant(TENANT2);
            while (jobInfoRepos.findAllByStatusStatus(JobStatus.SUCCEEDED).size() < jobInfos.length / 2) {
                Thread.sleep(1_000);
            }
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
}
