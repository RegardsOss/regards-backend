package fr.cnes.regards.framework.modules.jobs.service;

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

import fr.cnes.regards.framework.modules.jobs.dao.IJobInfoRepository;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.domain.LongJob;
import fr.cnes.regards.framework.modules.jobs.service.service.IJobInfoService;
import fr.cnes.regards.framework.modules.jobs.test.JobConfiguration;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;

/**
 * Test completion compute and update
 * @author oroussel
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = { JobConfiguration.class })
public class JobCompletionTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(JobCompletionTest.class);

    public static final String TENANT = "JOBS";

    @Autowired
    private IRuntimeTenantResolver tenantResolver;

    @Autowired
    private IJobInfoRepository jobInfoRepos;

    @Autowired
    private IJobInfoService jobInfoService;

    @Before
    public void setUp() throws Exception {
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
        longJob = jobInfoService.create(longJob);

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
}
