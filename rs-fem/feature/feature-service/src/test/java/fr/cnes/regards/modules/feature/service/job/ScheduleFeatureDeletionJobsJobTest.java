/*
 * Copyright 2017-2021 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.feature.service.job;

import java.util.concurrent.ExecutionException;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.domain.JobStatus;
import fr.cnes.regards.framework.modules.jobs.service.IJobInfoService;
import fr.cnes.regards.framework.modules.jobs.service.IJobService;
import fr.cnes.regards.modules.feature.dto.FeaturesSelectionDTO;
import fr.cnes.regards.modules.feature.service.AbstractFeatureMultitenantServiceTest;
import fr.cnes.regards.modules.feature.service.IFeatureService;

/**
 *
 * @author Sébastien Binda
 *
 */
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=deletion_job_test",
        "regards.amqp.enabled=true", "regards.feature.deletion.notification.job.size=30" })
@ActiveProfiles(value = { "testAmqp", "nohandler", "noscheduler" })
public class ScheduleFeatureDeletionJobsJobTest extends AbstractFeatureMultitenantServiceTest {

    @Autowired
    private IJobService jobService;

    @Autowired
    private IJobInfoService jobInfoService;

    @Autowired
    private IFeatureService featureService;

    @Test
    public void test() throws InterruptedException, ExecutionException {
        initData(100);
        JobInfo job = featureService.scheduleDeletionJob(FeaturesSelectionDTO.build().withSource("unknown"));
        if (job != null) {
            String tenant = runtimeTenantResolver.getTenant();
            jobService.runJob(job, tenant).get();
        }
        Assert.assertEquals(new Long(0L),
                            jobInfoService.retrieveJobsCount(PublishFeatureDeletionEventsJob.class.getName()));

        job = featureService.scheduleDeletionJob(FeaturesSelectionDTO.build());
        if (job != null) {
            String tenant = runtimeTenantResolver.getTenant();
            jobService.runJob(job, tenant).get();
        }
        Assert.assertEquals(new Long(4L),
                            jobInfoService.retrieveJobsCount(PublishFeatureDeletionEventsJob.class.getName(),
                                                             JobStatus.QUEUED, JobStatus.RUNNING, JobStatus.SUCCEEDED));
    }

}
