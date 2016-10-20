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

import fr.cnes.regards.framework.security.utils.jwt.JWTService;
import fr.cnes.regards.framework.security.utils.jwt.exception.JwtException;
import fr.cnes.regards.modules.jobs.domain.JobConfiguration;
import fr.cnes.regards.modules.jobs.domain.JobInfo;
import fr.cnes.regards.modules.jobs.domain.JobParameters;

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
    private IJobInfoRepository jobRepository;

    @Autowired
    private IStatusInfoRepository statusInfoRepository;

    /**
     * Do some setup before each test
     */
    @Before
    public void setUp() {
        try {
            jwtService.injectToken("test1", "USER");
        } catch (JwtException e) {
            LOG.error(e.getMessage(), e);
        }
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
        JobInfo jobBeforeSave = new JobInfo(jobConfiguration);

        // save the job and the corresponding jobStatusInfo
        JobInfo job = jobRepository.save(jobBeforeSave);

        Assertions.assertThat(job.getStatus().getDescription()).as(description);
        Assertions.assertThat(job.getId()).isEqualTo(jobBeforeSave.getId());

        jobRepository.delete(job.getId());
    }

    @Test
    public void testSaveJobWithPojoAsParameter() {
        String owner_ = "john.doe@opensource";
        JobConfiguration jobConfigurationToRunAfter = new JobConfiguration("", null, "", null, null, 5, null, owner_);
        JobInfo jobToRunAfter = new JobInfo(jobConfigurationToRunAfter);

        JobParameters parameters = new JobParameters();
        String keyParam = "thenRun";
        parameters.add(keyParam, jobToRunAfter);

        JobConfiguration jobConfiguration = new JobConfiguration("some description", parameters,
                "fr.cnes.regards.modules.MyCustomJob", LocalDateTime.now().plusDays(2),
                LocalDateTime.now().plusDays(15), 1, null, "john.doe@cnes");
        JobInfo jobBeforeSave = new JobInfo(jobConfiguration);

        // save the job and the corresponding jobStatusInfo
        JobInfo job = jobRepository.save(jobBeforeSave);
        LOG.info(job.toString());
        Map<String, Object> parameterAfterSave = job.getParameters().getMap();
        JobInfo jobAsParameterToRunAfter = (JobInfo) parameterAfterSave.get(keyParam);
        Assertions.assertThat(jobAsParameterToRunAfter.getOwner()).as(owner_);
        Assertions.assertThat(jobAsParameterToRunAfter.getStatus().getDescription()).as("");

    }

}