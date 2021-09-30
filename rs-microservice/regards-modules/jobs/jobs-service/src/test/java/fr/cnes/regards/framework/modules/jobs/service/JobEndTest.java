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
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import com.google.gson.Gson;

import fr.cnes.regards.framework.jpa.json.GsonUtil;
import fr.cnes.regards.framework.modules.jobs.dao.IJobInfoRepository;
import fr.cnes.regards.framework.modules.jobs.domain.RandomFloatJob;
import fr.cnes.regards.framework.modules.jobs.domain.TotoJob;
import fr.cnes.regards.framework.modules.jobs.domain.DoubleJob;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.domain.JobStatus;
import fr.cnes.regards.framework.modules.jobs.domain.LongJob;
import fr.cnes.regards.framework.modules.jobs.domain.Titi;
import fr.cnes.regards.framework.modules.jobs.domain.Toto;
import fr.cnes.regards.framework.modules.jobs.test.JobTestConfiguration;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;

/**
 * Test completion compute and update, result transmission, etc...
 * @author oroussel
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, classes = TestApplication.class)
@ContextConfiguration(classes = { JobTestConfiguration.class })
public class JobEndTest {

    public static final String TENANT = "JOBS";

    @SuppressWarnings("unused")
    private static final Logger LOGGER = LoggerFactory.getLogger(JobEndTest.class);

    @Autowired
    private IRuntimeTenantResolver tenantResolver;

    @Autowired
    private IJobInfoRepository jobInfoRepos;

    @Autowired
    private IJobInfoService jobInfoService;

    @Autowired
    private Gson gson;

    @Before
    public void setUp() {
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
        JobInfo longJob = new JobInfo(false);
        longJob.setClassName(LongJob.class.getName());
        longJob.setPriority(100);
        longJob = jobInfoService.createAsQueued(longJob);

        // wait a bisto denas half job time (which is 10 s)
        Thread.sleep(5_000);
        // Look at jobInfo from database
        JobInfo jobInfoFromDb = jobInfoRepos.findById(longJob.getId()).get();
        // percentComplete should be > 0 (around 50 %)
        int percent = jobInfoFromDb.getStatus().getPercentCompleted();
        Assert.assertTrue(percent > 0);
        // Estimation completion should have been computed
        Assert.assertNotNull(jobInfoFromDb.getStatus().getEstimatedCompletion());
        // Wait One second and a half : percent should be modified (upper)
        Thread.sleep(1_500);
        jobInfoFromDb = jobInfoRepos.findById(longJob.getId()).get();
        Assert.assertTrue(jobInfoFromDb.getStatus().getPercentCompleted() > percent);

        Thread.sleep(5_000);
    }

    @Test
    public void testWithResults1() throws InterruptedException {
        JobInfo randomFloatJob = new JobInfo(false);
        randomFloatJob.setClassName(RandomFloatJob.class.getName());
        randomFloatJob = jobInfoService.createAsQueued(randomFloatJob);

        Thread.sleep(1_000);
        // Look at jobInfo from database
        do {
            randomFloatJob = jobInfoRepos.findById(randomFloatJob.getId()).get();
        } while (randomFloatJob.getStatus().getStatus() != JobStatus.SUCCEEDED
                && randomFloatJob.getStatus().getStatus() != JobStatus.FAILED);
        Assert.assertEquals(JobStatus.SUCCEEDED, randomFloatJob.getStatus().getStatus());
        Assert.assertNotNull(randomFloatJob.getResult());
        Assert.assertTrue(randomFloatJob.getResult() instanceof Float);

        Thread.sleep(5_000);
    }

    @SuppressWarnings("rawtypes")
    @Test
    public void testWithResults2() throws InterruptedException {
        JobInfo doubleJob = new JobInfo(false);
        doubleJob.setClassName(DoubleJob.class.getName());
        doubleJob = jobInfoService.createAsQueued(doubleJob);

        Thread.sleep(1_000);
        // Look at jobInfo from database
        do {
            doubleJob = jobInfoRepos.findById(doubleJob.getId()).get();
        } while (doubleJob.getStatus().getStatus() != JobStatus.SUCCEEDED
                && doubleJob.getStatus().getStatus() != JobStatus.FAILED);
        Assert.assertEquals(JobStatus.SUCCEEDED, doubleJob.getStatus().getStatus());
        Assert.assertNotNull(doubleJob.getResult());
        Assert.assertTrue(doubleJob.getResult() instanceof Map);
        Assert.assertTrue(((Map) doubleJob.getResult()).containsKey("tutu"));
        Assert.assertTrue(((Map) doubleJob.getResult()).containsKey("toto"));
        Assert.assertTrue(((Map) doubleJob.getResult()).containsKey("titi"));
        Assert.assertEquals(1.0, ((Map) doubleJob.getResult()).get("toto"));
        Assert.assertEquals(3.0, ((Map) doubleJob.getResult()).get("titi"));
        Assert.assertEquals(2.0, ((Map) doubleJob.getResult()).get("tutu"));

        Thread.sleep(5_000);
    }

    @Test
    public void testWithResults3() throws InterruptedException {
        JobInfo totoJob = new JobInfo(false);
        totoJob.setClassName(TotoJob.class.getName());
        totoJob = jobInfoService.createAsQueued(totoJob);

        Thread.sleep(1_000);
        // Look at jobInfo from database
        do {
            totoJob = jobInfoRepos.findById(totoJob.getId()).get();
        } while (totoJob.getStatus().getStatus() != JobStatus.SUCCEEDED
                && totoJob.getStatus().getStatus() != JobStatus.FAILED);
        Assert.assertEquals(JobStatus.SUCCEEDED, totoJob.getStatus().getStatus());
        Assert.assertNotNull(totoJob.getResult());
        Assert.assertTrue(totoJob.getResult() instanceof Toto);
        Toto toto = new Toto();
        toto.setI(15);
        Assert.assertEquals(toto, totoJob.getResult());
        Titi titi = new Titi();
        titi.setJ(150);
        Assert.assertNotNull(((Toto) totoJob.getResult()).getList());
        Assert.assertEquals(1, ((Toto) totoJob.getResult()).getList().size());
        Assert.assertEquals(titi, ((Toto) totoJob.getResult()).getList().get(0));

        Thread.sleep(5_000);
    }

    @Test
    public void testExpirationDate() throws InterruptedException {
        JobInfo jobSnow = new JobInfo(false);
        jobSnow.setExpirationDate(OffsetDateTime.now());
        jobSnow = jobInfoService.createAsQueued(jobSnow);
        Thread.sleep(1_000);
        // Look at jobInfo from database
        do {
            jobSnow = jobInfoRepos.findById(jobSnow.getId()).get();
        } while (jobSnow.getStatus().getStatus() != JobStatus.SUCCEEDED
                && jobSnow.getStatus().getStatus() != JobStatus.FAILED);
        Assert.assertEquals(JobStatus.FAILED, jobSnow.getStatus().getStatus());
        Assert.assertEquals("Expiration date reached", jobSnow.getStatus().getStackTrace());
    }
}
