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
        } catch (final JwtException e) {
            LOG.error(e.getMessage(), e);
        }
        jobRepository.deleteAll();
    }

    @Test
    public void testSaveJob() {
        final JobParameters parameters = new JobParameters();
        parameters.add("source", "/path/to/folder");
        parameters.add("answer", 42);

        final String description = "This is a simple job";
        final Path workspace = FileSystems.getDefault().getPath("some", "random", "path.xls");
        final JobConfiguration jobConfiguration = new JobConfiguration(description, parameters,
                "fr.cnes.regards.modules.MyCustomJob", LocalDateTime.now().plusDays(2),
                LocalDateTime.now().plusDays(15), 1, workspace, "system");
        final JobInfo jobBeforeSave = new JobInfo(jobConfiguration);

        // save the job and the corresponding jobStatusInfo
        final JobInfo job = jobRepository.save(jobBeforeSave);

        Assertions.assertThat(job.getStatus().getDescription().equals(description)).isTrue();
        Assertions.assertThat(job.getId()).isEqualTo(jobBeforeSave.getId());

        jobRepository.delete(job.getId());
    }

    @Test
    public void testSaveJobWithPojoAsParameter() {
        final String owner_ = "john.doe@opensource";
        final JobConfiguration jobConfigurationToRunAfter = new JobConfiguration("", null, "", null, null, 5, null,
                owner_);
        final JobInfo jobToRunAfter = new JobInfo(jobConfigurationToRunAfter);

        final JobParameters parameters = new JobParameters();
        final String keyParam = "thenRun";
        parameters.add(keyParam, jobToRunAfter);

        final JobConfiguration jobConfiguration = new JobConfiguration("some description", parameters,
                "fr.cnes.regards.modules.MyCustomJob", LocalDateTime.now().plusDays(2),
                LocalDateTime.now().plusDays(15), 1, null, "john.doe@cnes");
        final JobInfo jobBeforeSave = new JobInfo(jobConfiguration);

        // save the job and the corresponding jobStatusInfo
        final JobInfo job = jobRepository.save(jobBeforeSave);
        LOG.info(job.toString());
        final Map<String, Object> parameterAfterSave = job.getParameters().getParameters();
        final JobInfo jobAsParameterToRunAfter = (JobInfo) parameterAfterSave.get(keyParam);
        Assertions.assertThat(jobAsParameterToRunAfter.getOwner().equals(owner_)).isTrue();
        Assertions.assertThat(jobAsParameterToRunAfter.getStatus().getDescription().equals("")).isTrue();

    }

}