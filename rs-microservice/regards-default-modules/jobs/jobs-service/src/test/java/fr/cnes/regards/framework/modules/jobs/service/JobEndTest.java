package fr.cnes.regards.framework.modules.jobs.service;

import java.time.OffsetDateTime;
import java.util.Map;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import com.google.gson.Gson;
import fr.cnes.regards.framework.jpa.json.GsonUtil;
import fr.cnes.regards.framework.modules.jobs.dao.IJobInfoRepository;
import fr.cnes.regards.framework.modules.jobs.domain.BlowJob;
import fr.cnes.regards.framework.modules.jobs.domain.FootJob;
import fr.cnes.regards.framework.modules.jobs.domain.HandJob;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.domain.JobStatus;
import fr.cnes.regards.framework.modules.jobs.domain.LongJob;
import fr.cnes.regards.framework.modules.jobs.domain.Titi;
import fr.cnes.regards.framework.modules.jobs.domain.Toto;
import fr.cnes.regards.framework.modules.jobs.test.JobConfiguration;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;

/**
 * Test completion compute and update, result transmission, etc...
 * @author oroussel
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = { JobConfiguration.class })
public class JobEndTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(JobEndTest.class);

    public static final String TENANT = "JOBS";

    @Autowired
    private IRuntimeTenantResolver tenantResolver;

    @Autowired
    private IJobInfoRepository jobInfoRepos;

    @Autowired
    private IJobInfoService jobInfoService;

    @Autowired
    private Gson gson;

    @Before
    public void setUp() throws Exception {
        GsonUtil.setGson(gson);

        tenantResolver.forceTenant(TENANT);
    }

    @After
    public void tearDown() {
        tenantResolver.forceTenant(TENANT);
        jobInfoRepos.deleteAll();
    }

    @Test
    public void test() throws InterruptedException {
        JobInfo longJob = new JobInfo();
        longJob.setDescription("A long job updating its completion");
        longJob.setClassName(LongJob.class.getName());
        longJob.setPriority(100);
        longJob = jobInfoService.createAsQueued(longJob);

        // wait a bisto denas half job time (which is 10 s)
        Thread.sleep(5_000);
        // Look at jobInfo from database
        JobInfo jobInfoFromDb = jobInfoRepos.findOne(longJob.getId());
        // percentComplete should be > 0 (around 50 %)
        int percent = jobInfoFromDb.getStatus().getPercentCompleted();
        Assert.assertTrue(percent > 0);
        // Estimation completion should have been computed
        Assert.assertNotNull(jobInfoFromDb.getStatus().getEstimatedCompletion());
        // Wait One second and a half : percent should be modified (upper)
        Thread.sleep(1_500);
        jobInfoFromDb = jobInfoRepos.findOne(longJob.getId());
        Assert.assertTrue(jobInfoFromDb.getStatus().getPercentCompleted() > percent);

        Thread.sleep(5_000);
    }

    @Test
    public void testWithResults1() throws InterruptedException {
        JobInfo blowJob = new JobInfo();
        blowJob.setDescription("A job that set a random float as result");
        blowJob.setClassName(BlowJob.class.getName());
        blowJob = jobInfoService.createAsQueued(blowJob);

        Thread.sleep(1_000);
        // Look at jobInfo from database
        do {
            blowJob = jobInfoRepos.findOne(blowJob.getId());
        } while ((blowJob.getStatus().getStatus() != JobStatus.SUCCEEDED) && (blowJob.getStatus().getStatus()
                != JobStatus.FAILED));
        Assert.assertEquals(JobStatus.SUCCEEDED, blowJob.getStatus().getStatus());
        Assert.assertNotNull(blowJob.getResult());
        Assert.assertTrue(blowJob.getResult() instanceof Float);

        Thread.sleep(5_000);
    }

    @Test
    public void testWithResults2() throws InterruptedException {
        JobInfo handJob = new JobInfo();
        handJob.setDescription("A job that set a complex object result");
        handJob.setClassName(HandJob.class.getName());
        handJob = jobInfoService.createAsQueued(handJob);

        Thread.sleep(1_000);
        // Look at jobInfo from database
        do {
            handJob = jobInfoRepos.findOne(handJob.getId());
        } while ((handJob.getStatus().getStatus() != JobStatus.SUCCEEDED) && (handJob.getStatus().getStatus()
                != JobStatus.FAILED));
        Assert.assertEquals(JobStatus.SUCCEEDED, handJob.getStatus().getStatus());
        Assert.assertNotNull(handJob.getResult());
        Assert.assertTrue(handJob.getResult() instanceof Map);
        Assert.assertTrue(((Map) handJob.getResult()).containsKey("tutu"));
        Assert.assertTrue(((Map) handJob.getResult()).containsKey("toto"));
        Assert.assertTrue(((Map) handJob.getResult()).containsKey("titi"));
        Assert.assertEquals(1.0, ((Map) handJob.getResult()).get("toto"));
        Assert.assertEquals(3.0, ((Map) handJob.getResult()).get("titi"));
        Assert.assertEquals(2.0, ((Map) handJob.getResult()).get("tutu"));

        Thread.sleep(5_000);
    }

    @Test
    public void testWithResults3() throws InterruptedException {
        JobInfo footJob = new JobInfo();
        footJob.setDescription("A job that set a complex object result");
        footJob.setClassName(FootJob.class.getName());
        footJob = jobInfoService.createAsQueued(footJob);

        Thread.sleep(1_000);
        // Look at jobInfo from database
        do {
            footJob = jobInfoRepos.findOne(footJob.getId());
        } while ((footJob.getStatus().getStatus() != JobStatus.SUCCEEDED) && (footJob.getStatus().getStatus()
                != JobStatus.FAILED));
        Assert.assertEquals(JobStatus.SUCCEEDED, footJob.getStatus().getStatus());
        Assert.assertNotNull(footJob.getResult());
        Assert.assertTrue(footJob.getResult() instanceof Toto);
        Toto toto = new Toto();
        toto.setI(15);
        Assert.assertEquals(toto, footJob.getResult());
        Titi titi = new Titi();
        titi.setJ(150);
        Assert.assertNotNull(((Toto)footJob.getResult()).getList());
        Assert.assertEquals(1, ((Toto)footJob.getResult()).getList().size());
        Assert.assertEquals(titi, ((Toto)footJob.getResult()).getList().get(0));

        Thread.sleep(5_000);
    }

    @Test
    public void testExpirationDate() throws InterruptedException {
        JobInfo jobSnow = new JobInfo();
        jobSnow.setExpirationDate(OffsetDateTime.now());
        jobSnow = jobInfoService.createAsQueued(jobSnow);
        Thread.sleep(1_000);
        // Look at jobInfo from database
        do {
            jobSnow = jobInfoRepos.findOne(jobSnow.getId());
        } while ((jobSnow.getStatus().getStatus() != JobStatus.SUCCEEDED) && (jobSnow.getStatus().getStatus()
                != JobStatus.FAILED));
        Assert.assertEquals(JobStatus.FAILED, jobSnow.getStatus().getStatus());
        Assert.assertEquals("Expiration date reached", jobSnow.getStatus().getStackTrace());
    }
}
