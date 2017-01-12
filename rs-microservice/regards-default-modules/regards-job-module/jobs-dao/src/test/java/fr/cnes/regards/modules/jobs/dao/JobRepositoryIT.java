/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.jobs.dao;

import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

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

    @SuppressWarnings("unused")
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
        final String owner = "john.doe@c-s.fr";
        final JobParameters parameters = JobParametersFactory.build().addParameter("source", "/path/to/folder")
                .addParameter("answer", 42).getParameters();

        final String description = "This is a simple job";
        final Path workspace = FileSystems.getDefault().getPath("some", "random", "path.xls");
        JobConfiguration jobConfiguration = new JobConfiguration(description, parameters,
                "fr.cnes.regards.modules.MyCustomJob", LocalDateTime.now().plusDays(2),
                LocalDateTime.now().plusDays(15), 1, workspace, owner);
        jobConfiguration.getStatusInfo().setJobStatus(JobStatus.RUNNING);
        JobInfo jobBeforeSave = new JobInfo(jobConfiguration);

        // save the job and the corresponding jobStatusInfo
        JobInfo job = jobRepository.save(jobBeforeSave);

        // test the job is saved
        Assert.assertEquals(1, jobRepository.count());
        Assert.assertEquals(1, statusRepository.count());

        // control the saved job with the initial job
        Assert.assertEquals(jobBeforeSave.getWorkspace(), job.getWorkspace());
        isEquals(jobBeforeSave, job);

        // find the job with id
        job = jobRepository.findOne(job.getId());
        Assert.assertEquals(jobBeforeSave.getWorkspace(), job.getWorkspace());
        isEquals(jobBeforeSave, job);

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
        final Path workspace = FileSystems.getDefault().getPath("some", "random", "path.xls");
        final JobParameters parametersConfAfter = JobParametersFactory.build().addParameter("source", "/path/to/folder")
                .addParameter("answer", 42).getParameters();
        final JobConfiguration jobConfigurationToRunAfter = new JobConfiguration("job configuration to run after",
                parametersConfAfter, "fr.cnes.regards.modules.MyCustomJob", LocalDateTime.now().minusDays(10),
                LocalDateTime.now().plusDays(2), 5, workspace, owner);
        final JobInfo jobToRunAfter = new JobInfo(jobConfigurationToRunAfter);

        final String keyParam = "thenRun";
        final JobParameters parameters = JobParametersFactory.build().addParameter(keyParam, jobToRunAfter)
                .getParameters();

        final JobConfiguration jobConfiguration = new JobConfiguration("some description", parameters,
                "fr.cnes.regards.modules.MyCustomJob", LocalDateTime.now().plusDays(2),
                LocalDateTime.now().plusDays(15), 1, workspace, owner);
        jobConfiguration.getStatusInfo().setJobStatus(JobStatus.RUNNING);
        final JobInfo jobBeforeSave = new JobInfo(jobConfiguration);

        // save the job and the corresponding jobStatusInfo
        final JobInfo jobSaved = jobRepository.save(jobBeforeSave);

        final Map<String, Object> parameterAfterSave = jobSaved.getParameters().getParameters();
        final JobInfo jobAsParameterToRunAfter = (JobInfo) parameterAfterSave.get(keyParam);

        Assert.assertEquals(jobConfigurationToRunAfter.getClassName(), jobAsParameterToRunAfter.getClassName());
        Assert.assertEquals(jobConfigurationToRunAfter.getOwner(), jobAsParameterToRunAfter.getOwner());
        Assert.assertEquals(jobConfigurationToRunAfter.getStatusInfo().getDescription(),
                            jobAsParameterToRunAfter.getStatus().getDescription());
        Assert.assertEquals(jobConfigurationToRunAfter.getStatusInfo().getStartDate(),
                            jobAsParameterToRunAfter.getStatus().getStartDate());
        Assert.assertEquals(jobConfigurationToRunAfter.getStatusInfo().getStopDate(),
                            jobAsParameterToRunAfter.getStatus().getStopDate());
        Assert.assertEquals(jobConfigurationToRunAfter.getStatusInfo().getJobStatus(),
                            jobAsParameterToRunAfter.getStatus().getJobStatus());
    }

    /**
     * Test if 2 {@link JobInfo} are equals (ie the all the attributes).
     *
     * @param pJobExpected
     * @param pJobActual
     */
    private void isEquals(JobInfo pJobExpected, JobInfo pJobActual) {
        Assert.assertEquals(pJobExpected.getId(), pJobActual.getId());
        Assert.assertEquals(pJobExpected.getStatus().getExpirationDate(), pJobActual.getStatus().getExpirationDate());
        Assert.assertEquals(pJobExpected.getStatus().getPercentCompleted(),
                            pJobActual.getStatus().getPercentCompleted());
        Assert.assertEquals(pJobExpected.getStatus().getJobStatus(), pJobActual.getStatus().getJobStatus());
        Assert.assertEquals(pJobExpected.getStatus().getStartDate(), pJobActual.getStatus().getStartDate());
        Assert.assertEquals(pJobExpected.getStatus().getStopDate(), pJobActual.getStatus().getStopDate());
        Assert.assertEquals(pJobExpected.getStatus().getDescription(), (pJobActual.getStatus().getDescription()));

        Assert.assertEquals(pJobExpected.getOwner(), pJobActual.getOwner());
        Assert.assertEquals(pJobExpected.getPriority(), pJobActual.getPriority());
        Assert.assertEquals(pJobExpected.getParameters().getParameters().size(),
                            pJobActual.getParameters().getParameters().size());
        Assert.assertEquals(pJobExpected.getStatus().getStartDate(), pJobActual.getStatus().getStartDate());
        Assert.assertEquals(pJobExpected.getStatus().getStopDate(), pJobActual.getStatus().getStopDate());

        Assert.assertEquals(pJobExpected.getWorkspace(), pJobActual.getWorkspace());
    }

}