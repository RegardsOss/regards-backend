/**
 *
 */
package fr.cnes.regards.modules.jobs.dao;

import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Map;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import fr.cnes.regards.modules.jobs.domain.Job;
import fr.cnes.regards.modules.jobs.domain.JobConfiguration;
import fr.cnes.regards.modules.jobs.domain.JobParameters;
import fr.cnes.regards.security.utils.jwt.JWTService;

/**
 * @author lmieulet
 *
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = { JobDaoTestConfiguration.class })
public class JobRepositoryIT {

    private static final Logger LOG = LoggerFactory.getLogger(JobRepositoryIT.class);

    /**
     * Utility service for handling JWT. Autowired by Spring.
     */
    @Autowired
    private JWTService jwtService;

    @Autowired
    private IJobRepository jobRepository;

    @Autowired
    private IStatusInfoRepository statusInfoRepository;

    /**
     * Do some setup before each test
     */
    @Before
    public void setUp() {
        jwtService.injectToken("test1", "USER");
        jobRepository.deleteAll();
    }

    @Test
    public void testSaveJob() {
        JobParameters parameters = new JobParameters();
        parameters.add("source", "/path/to/folder");
        parameters.add("answer", 42);

        String description = "This is a simple job";
        Path workspace = FileSystems.getDefault().getPath("some", "random", "path.xls");
        JobConfiguration jobConfiguration = new JobConfiguration(description, parameters,
                "fr.cnes.regards.modules.MyCustomJob", LocalDateTime.now().plusDays(2),
                LocalDateTime.now().plusDays(15), 1, workspace, "system");
        Job jobBeforeSave = new Job(jobConfiguration);

        // save the job and the corresponding jobStatusInfo
        Job job = jobRepository.save(jobBeforeSave);

        Assertions.assertThat(job.getStatus().getDescription()).as(description);
        Assertions.assertThat(job.getId()).isEqualTo(jobBeforeSave.getId());

        jobRepository.delete(job.getId());
    }

    @Test
    public void testSaveJobWithPojoAsParameter() {
        String owner_ = "john.doe@opensource";
        JobConfiguration jobConfigurationToRunAfter = new JobConfiguration("", null, "", null, null, 5, null, owner_);
        Job jobToRunAfter = new Job(jobConfigurationToRunAfter);

        JobParameters parameters = new JobParameters();
        String keyParam = "thenRun";
        parameters.add(keyParam, jobToRunAfter);

        JobConfiguration jobConfiguration = new JobConfiguration("some description", parameters,
                "fr.cnes.regards.modules.MyCustomJob", LocalDateTime.now().plusDays(2),
                LocalDateTime.now().plusDays(15), 1, null, "john.doe@cnes");
        Job jobBeforeSave = new Job(jobConfiguration);

        // save the job and the corresponding jobStatusInfo
        Job job = jobRepository.save(jobBeforeSave);
        LOG.info(job.toString());
        Map<String, Object> parameterAfterSave = job.getParameters().getMap();
        Job jobAsParameterToRunAfter = (Job) parameterAfterSave.get(keyParam);
        Assertions.assertThat(jobAsParameterToRunAfter.getOwner()).as(owner_);
        Assertions.assertThat(jobAsParameterToRunAfter.getStatus().getDescription()).as("");

    }

}