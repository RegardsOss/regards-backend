package fr.cnes.regards.framework.modules.jobs.service;

import com.google.gson.Gson;
import fr.cnes.regards.framework.jpa.json.GsonUtil;
import fr.cnes.regards.framework.modules.jobs.dao.IJobInfoRepository;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.domain.JobStatus;
import fr.cnes.regards.framework.modules.jobs.test.JobTestConfiguration;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.test.util.JUnitLogRule;
import org.awaitility.Awaitility;
import org.junit.*;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Test of Jobs executions (status, pool, spring autowiring, ...)
 *
 * @author oroussel
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = { JobTestConfiguration.class })
@TestPropertySource(properties = { "regards.jobs.pool.size=1" })
public class JobServicePoolerIT {

    public static final String TENANT = "JOBS";

    private static final Logger LOGGER = LoggerFactory.getLogger(JobServicePoolerIT.class);

    @Autowired
    private IJobInfoRepository jobInfoRepos;

    @Autowired
    private IRuntimeTenantResolver tenantResolver;

    @Autowired
    private JobTestCleaner jobTestCleaner;

    @Autowired
    private JobServiceJobCreator jobServiceJobCreator;

    @Rule
    public JUnitLogRule rule = new JUnitLogRule();

    @Autowired
    private Gson gson;

    @Before
    public void setUp() {
        GsonUtil.setGson(gson);

        tenantResolver.forceTenant(TENANT);
        jobInfoRepos.deleteAll();

        jobTestCleaner.startJobManager();
    }

    @After
    public void after() throws Exception {
        jobTestCleaner.cleanJob();
    }

    @Test
    public void testPool() {
        // Create 6 waitJob
        JobInfo[] jobInfos = jobServiceJobCreator.runWaitJobs();

        // Wait to be sure jobs are treated by pool
        Awaitility.await().atMost(30, TimeUnit.SECONDS).until(() -> {
            tenantResolver.forceTenant(TENANT);
            // Only poolSize jobs should be runnings
            return jobInfoRepos.countByStatusStatusIn(JobStatus.SUCCEEDED) == jobInfos.length;
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

}