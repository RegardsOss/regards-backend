/*
 * Copyright 2017-2024 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.framework.modules.jobs.service;

import fr.cnes.regards.framework.modules.jobs.dao.IJobInfoRepository;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.domain.JobStatus;
import fr.cnes.regards.framework.modules.jobs.domain.TotoJob;
import fr.cnes.regards.framework.modules.jobs.test.JobTestConfiguration;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.HashSet;

/**
 * Test of Jobs priority
 *
 * @author Thibaud Michaudel
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = { JobTestConfiguration.class })
@ActiveProfiles({ "test", "nojobs", "noscheduler" })
public class JobPriorityIT {

    @Autowired
    private IJobInfoService jobInfoService;

    @Autowired
    private IJobInfoRepository jobInfoRepos;

    @Before
    public void setUp() {
        jobInfoRepos.deleteAll();
    }

    /**
     * Test that the job with the highest priority is played first
     */
    @Test
    public void test_job_priority() {
        // Given
        JobInfo highPriority = new JobInfo(false, 100, new HashSet<>(), "owner", TotoJob.class.getName());
        highPriority.updateStatus(JobStatus.QUEUED);
        highPriority = jobInfoRepos.save(highPriority);

        JobInfo lowPriority = new JobInfo(false, 10, new HashSet<>(), "owner", TotoJob.class.getName());
        lowPriority.updateStatus(JobStatus.QUEUED);
        lowPriority = jobInfoRepos.save(lowPriority);

        JobInfo mediumPriority = new JobInfo(false, 50, new HashSet<>(), "owner", TotoJob.class.getName());
        mediumPriority.updateStatus(JobStatus.QUEUED);
        mediumPriority = jobInfoRepos.save(mediumPriority);

        // When
        JobInfo job = jobInfoService.findHighestPriorityQueuedJobAndSetAsToBeRun();

        // Then
        Assertions.assertEquals(highPriority.getId(), job.getId());

        // When
        job = jobInfoService.findHighestPriorityQueuedJobAndSetAsToBeRun();

        // Then
        Assertions.assertEquals(mediumPriority.getId(), job.getId());

        // When
        job = jobInfoService.findHighestPriorityQueuedJobAndSetAsToBeRun();

        // Then
        Assertions.assertEquals(lowPriority.getId(), job.getId());
    }

}
