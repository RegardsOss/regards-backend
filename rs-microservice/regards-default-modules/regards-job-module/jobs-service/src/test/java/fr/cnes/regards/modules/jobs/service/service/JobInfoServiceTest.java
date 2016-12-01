/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.jobs.service.service;

import java.time.LocalDateTime;

import org.assertj.core.api.Assertions;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import fr.cnes.regards.framework.amqp.exception.RabbitMQVhostException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.modules.jobs.dao.IJobInfoRepository;
import fr.cnes.regards.modules.jobs.domain.JobConfiguration;
import fr.cnes.regards.modules.jobs.domain.JobInfo;
import fr.cnes.regards.modules.jobs.domain.JobParameters;
import fr.cnes.regards.modules.jobs.domain.JobStatus;
import fr.cnes.regards.modules.jobs.service.communication.INewJobPublisher;
import fr.cnes.regards.modules.jobs.service.communication.IStoppingJobPublisher;

/**
 * @author Léo Mieulet
 */
public class JobInfoServiceTest {

    private IJobInfoRepository jobInfoRepository;

    private JobInfoService jobInfoService;

    private INewJobPublisher newJobPublisher;

    private IStoppingJobPublisher stoppingJobPublisher;

    private JobInfo pJobInfo;

    @Before
    public void setUp() {
        jobInfoRepository = Mockito.mock(IJobInfoRepository.class);
        newJobPublisher = Mockito.mock(INewJobPublisher.class);
        stoppingJobPublisher = Mockito.mock(IStoppingJobPublisher.class);
        jobInfoService = new JobInfoService(jobInfoRepository, newJobPublisher, stoppingJobPublisher);

        // Create a new jobInfo
        final JobParameters pParameters = new JobParameters();
        pParameters.add("follow", "Kepler");
        final String jobClassName = "fr.cnes.regards.modules.jobs.service.manager.AJob";
        final LocalDateTime pEstimatedCompletion = LocalDateTime.now().plusHours(5);
        final LocalDateTime pExpirationDate = LocalDateTime.now().plusDays(15);
        final String description = "some job description";
        final String owner = "IntegrationTest";
        final JobConfiguration pJobConfiguration = new JobConfiguration(description, pParameters, jobClassName,
                pEstimatedCompletion, pExpirationDate, 1, null, owner);

        pJobInfo = new JobInfo(pJobConfiguration);
        pJobInfo.setId(1L);
    }

    // TODO: create a new JobHandlerUT
    //
    @Test
    public void testCreateJobInfo() throws RabbitMQVhostException {
        Mockito.when(jobInfoRepository.save(pJobInfo)).thenReturn(pJobInfo);
        final JobInfo jobInfo = jobInfoService.createJobInfo(pJobInfo);

        Mockito.verify(newJobPublisher).sendJob(pJobInfo.getId());
        Assertions.assertThat(jobInfo.getStatus()).isEqualTo(pJobInfo.getStatus());
    }

    @Test
    public void testCreate() {
        final JobInfo jobInfo = new JobInfo();
        jobInfoService.createJobInfo(jobInfo);
        Mockito.verify(jobInfoRepository).save(jobInfo);
    }

    @Test
    public void testSave() {
        final JobInfo jobInfo = new JobInfo();
        jobInfoService.save(jobInfo);
        Mockito.verify(jobInfoRepository).save(jobInfo);
    }

    @Test
    public void testRetrieveJobInfoById() {
        final Long jobInfoId = 14L;
        final JobInfo jobInfo = new JobInfo();
        jobInfo.setId(jobInfoId);
        Mockito.when(jobInfoRepository.findOne(jobInfoId)).thenReturn(jobInfo);
        try {
            final JobInfo job = jobInfoService.retrieveJobInfoById(jobInfoId);
            Assert.assertNotNull(job);
        } catch (EntityNotFoundException e) {
            Assert.fail();
        }
    }

    @Test
    public void testRetrieveJobInfoListByState() {
        final JobStatus pStatus = JobStatus.QUEUED;
        jobInfoService.retrieveJobInfoListByState(pStatus);
        Mockito.verify(jobInfoRepository).findAllByStatusStatus(pStatus);
    }

    @Test
    public void testRetrieveJobInfoList() {
        jobInfoService.retrieveJobInfoList();
        Mockito.verify(jobInfoRepository).findAll();
    }
}
