/*
 * Copyright 2017 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
 *
 * This file is part of REGARDS.
 *
 * REGARDS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * REGARDS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with REGARDS. If not, see <http://www.gnu.org/licenses/>.
 */
package fr.cnes.regards.framework.modules.jobs.service.service;

import java.time.OffsetDateTime;

import org.assertj.core.api.Assertions;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import fr.cnes.regards.framework.amqp.exception.RabbitMQVhostException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.modules.jobs.dao.IJobInfoRepository;
import fr.cnes.regards.framework.modules.jobs.domain.JobConfiguration;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.domain.JobParameters;
import fr.cnes.regards.framework.modules.jobs.domain.JobStatus;
import fr.cnes.regards.framework.modules.jobs.service.communication.INewJobPublisher;
import fr.cnes.regards.framework.modules.jobs.service.communication.IStoppingJobPublisher;

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
        final String jobClassName = "fr.cnes.regards.framework.modules.jobs.service.manager.AJob";
        final OffsetDateTime pEstimatedCompletion = OffsetDateTime.now().plusHours(5);
        final OffsetDateTime pExpirationDate = OffsetDateTime.now().plusDays(15);
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
