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

import static fr.cnes.regards.framework.amqp.event.Target.ONE_PER_MICROSERVICE_TYPE;

import java.util.concurrent.ExecutionException;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.domain.JobStatus;
import fr.cnes.regards.framework.modules.jobs.service.IJobInfoService;
import fr.cnes.regards.framework.modules.jobs.service.IJobService;
import fr.cnes.regards.modules.feature.dto.FeaturesSelectionDTO;
import fr.cnes.regards.modules.feature.dto.event.in.FeatureDeletionRequestEvent;
import fr.cnes.regards.modules.feature.service.AbstractFeatureMultitenantServiceTest;
import fr.cnes.regards.modules.feature.service.IFeatureService;

/**
 * Test class to check {@link PublishFeatureDeletionEventsJob}s and  {@link ScheduleFeatureDeletionJobsJob}s
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

    @Autowired
    private ISubscriber subscriber;

    @Test
    public void test() throws InterruptedException, ExecutionException {
        // Initialize AMQP for test
        cleanAMQPQueues(DeletionEventListener.class, ONE_PER_MICROSERVICE_TYPE);
        DeletionEventListener listener = new DeletionEventListener();
        subscriber.subscribeTo(FeatureDeletionRequestEvent.class, listener);

        // Initialize some feature
        initData(100);

        // Schedule global deletion job with feature filters that results to no feature
        JobInfo job = featureService.scheduleDeletionJob(FeaturesSelectionDTO.build().withSource("unknown"));
        if (job != null) {
            String tenant = runtimeTenantResolver.getTenant();
            jobService.runJob(job, tenant).get();
        }
        // No job should be scheduled
        Assert.assertEquals("No PublishFeatureDeletionEventsJob should be scheduled as the feature selection should be empty",
                            new Long(0L),
                            jobInfoService.retrieveJobsCount(PublishFeatureDeletionEventsJob.class.getName()));
        Thread.sleep(1_000);
        Assert.assertEquals("No deletion request event should be sent", 0, listener.getNumberOfRequests());

        // Rerun schedule of deletion jobs for all features this time
        job = featureService.scheduleDeletionJob(FeaturesSelectionDTO.build());
        if (job != null) {
            String tenant = runtimeTenantResolver.getTenant();
            jobService.runJob(job, tenant).get();
        }
        // As the number of features to handle is 100 and each job should handle 30 features, there should be 4 jobs scheduled
        Assert.assertEquals("There should be 100/regards.feature.deletion.notification.job.size jobs scheduled ",
                            new Long(4L),
                            jobInfoService.retrieveJobsCount(PublishFeatureDeletionEventsJob.class.getName(),
                                                             JobStatus.TO_BE_RUN, JobStatus.QUEUED, JobStatus.RUNNING,
                                                             JobStatus.SUCCEEDED));
        Thread.sleep(2_000);
        Assert.assertEquals("All 100 feature should be notified", 100, listener.getNumberOfRequests());
    }

}
