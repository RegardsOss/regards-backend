/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.jobs.dao;

import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.assertj.core.api.Assertions;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import fr.cnes.regards.framework.jpa.multitenant.test.AbstractDaoTest;
import fr.cnes.regards.modules.jobs.domain.JobConfiguration;
import fr.cnes.regards.modules.jobs.domain.JobInfo;
import fr.cnes.regards.modules.jobs.domain.JobParameters;
import fr.cnes.regards.modules.jobs.domain.JobParametersFactory;
import fr.cnes.regards.modules.jobs.domain.JobStatus;

/**
 * @author Léo Mieulet
 *
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = { JobDaoTestConfiguration.class })
public class JobRepositoryIT extends AbstractDaoTest {

    private static final Logger LOG = LoggerFactory.getLogger(JobRepositoryIT.class);

    @Autowired
    private IJobInfoRepository jobRepository;

    @Autowired
    private IStatusInfoRepository statusRepository;

    /**
     * Do some setup before each test
     */
    @Before
    public void setUp() {
        injectToken("test1");
        jobRepository.deleteAll();
        statusRepository.deleteAll();
    }

    @Test
    public void testSaveJob() {
        final JobParameters parameters = JobParametersFactory.build().addParameter("source", "/path/to/folder")
                .addParameter("answer", 42).getParameters();

        final String description = "This is a simple job";
        final Path workspace = FileSystems.getDefault().getPath("some", "random", "path.xls");
        JobConfiguration jobConfiguration = new JobConfiguration(description, parameters,
                "fr.cnes.regards.modules.MyCustomJob", LocalDateTime.now().plusDays(2),
                LocalDateTime.now().plusDays(15), 1, workspace, "system");
        jobConfiguration.getStatusInfo().setJobStatus(JobStatus.RUNNING);
        JobInfo jobBeforeSave = new JobInfo(jobConfiguration);

        // save the job and the corresponding jobStatusInfo
        JobInfo job = jobRepository.save(jobBeforeSave);

        // test the job is saved
        Assert.assertEquals(1, jobRepository.count());
        Assert.assertEquals(1, statusRepository.count());

        // control the saved job with the initial job
        isEquals(jobBeforeSave, job);

        // find the job with id
        JobInfo aJob = jobRepository.findOne(job.getId());
        isEquals(jobBeforeSave, aJob);

        // test findAllByStatusStatus
        List<JobInfo> jobs = jobRepository.findAllByStatusStatus(JobStatus.QUEUED);
        Assert.assertEquals(0, jobs.size());

        jobs = jobRepository.findAllByStatusStatus(JobStatus.RUNNING);
        Assert.assertEquals(1, jobs.size());

        isEquals(jobBeforeSave, jobs.get(0));

        // a second job
        jobConfiguration = new JobConfiguration(description, parameters, "fr.cnes.regards.modules.MyCustomJob",
                LocalDateTime.now().plusDays(2), LocalDateTime.now().plusDays(15), 1, workspace, "system");
        jobConfiguration.getStatusInfo().setJobStatus(JobStatus.RUNNING);
        jobBeforeSave = new JobInfo(jobConfiguration);

        // save the job and the corresponding jobStatusInfo
        job = jobRepository.save(jobBeforeSave);

        Assert.assertEquals(2, jobRepository.count());
        Assert.assertEquals(2, statusRepository.count());

        jobs = jobRepository.findAllByStatusStatus(JobStatus.RUNNING);
        Assert.assertEquals(2, jobs.size());
        Assert.assertEquals(2, statusRepository.findAllByStatus(JobStatus.RUNNING).size());

        jobRepository.delete(job.getId());
        Assert.assertEquals(1, jobRepository.count());
        Assert.assertEquals(1, statusRepository.count());

        Assert.assertEquals(1, statusRepository.findAllByStatus(JobStatus.RUNNING).size());

        jobRepository.deleteAll();
        statusRepository.deleteAll();
    }

    @Test
    public void testSaveJobWithPojoAsParameter() {
        final String owner = "john.doe@opensource";
        final JobConfiguration jobConfigurationToRunAfter = new JobConfiguration("", null, "", null, null, 5, null,
                owner);
        final JobInfo jobToRunAfter = new JobInfo(jobConfigurationToRunAfter);

        final String keyParam = "thenRun";
        final JobParameters parameters = JobParametersFactory.build().addParameter(keyParam, jobToRunAfter)
                .getParameters();

        final JobConfiguration jobConfiguration = new JobConfiguration("some description", parameters,
                "fr.cnes.regards.modules.MyCustomJob", LocalDateTime.now().plusDays(2),
                LocalDateTime.now().plusDays(15), 1, null, "john.doe@cnes");
        final JobInfo jobBeforeSave = new JobInfo(jobConfiguration);

        // save the job and the corresponding jobStatusInfo
        final JobInfo job = jobRepository.save(jobBeforeSave);
        LOG.info(job.toString());
        final Map<String, Object> parameterAfterSave = job.getParameters().getParameters();
        final JobInfo jobAsParameterToRunAfter = (JobInfo) parameterAfterSave.get(keyParam);

        Assertions.assertThat(jobAsParameterToRunAfter.getOwner().equals(jobConfigurationToRunAfter.getOwner()))
                .isTrue();
        Assertions.assertThat(jobAsParameterToRunAfter.getStatus().getDescription()
                .equals(jobConfigurationToRunAfter.getStatusInfo().getDescription())).isTrue();
    }

    private void isEquals(JobInfo pJobExpected, JobInfo pJobActual) {
        Assertions.assertThat(pJobExpected.getId()).isEqualTo(pJobActual.getId());
        Assertions.assertThat(pJobExpected.getStatus().getExpirationDate()).isEqualTo(pJobActual.getStatus().getExpirationDate());
        Assertions.assertThat(pJobExpected.getStatus().getPercentCompleted()).isEqualTo(pJobActual.getStatus().getPercentCompleted());
        Assertions.assertThat(pJobExpected.getStatus().getJobStatus()).isEqualTo(pJobActual.getStatus().getJobStatus());
        Assertions.assertThat(pJobExpected.getStatus().getStartDate()).isEqualTo(pJobActual.getStatus().getStartDate());
        Assertions.assertThat(pJobExpected.getStatus().getStopDate()).isEqualTo(pJobActual.getStatus().getStopDate());
        Assertions.assertThat(pJobExpected.getStatus().getDescription().equals(pJobActual.getStatus().getDescription()))
                .isTrue();

        Assert.assertEquals(pJobExpected.getOwner(), pJobActual.getOwner());
        Assert.assertEquals(pJobExpected.getPriority(), pJobActual.getPriority());
        Assert.assertEquals(pJobExpected.getParameters().getParameters().size(),
                            pJobActual.getParameters().getParameters().size());
        Assert.assertEquals(pJobExpected.getStatus().getStartDate(), pJobActual.getStatus().getStartDate());
        Assert.assertEquals(pJobExpected.getStatus().getStopDate(), pJobActual.getStatus().getStopDate());
        // TYODO CMZ à remettre
//        Assert.assertEquals(pJobExpected.getWorkspace(), pJobActual.getWorkspace());
    }

}