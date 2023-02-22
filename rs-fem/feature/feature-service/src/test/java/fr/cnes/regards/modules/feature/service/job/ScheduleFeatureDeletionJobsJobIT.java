/*
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.domain.JobStatus;
import fr.cnes.regards.framework.modules.jobs.service.IJobInfoService;
import fr.cnes.regards.framework.modules.jobs.service.IJobService;
import fr.cnes.regards.modules.feature.domain.SearchFeatureSimpleEntityParameters;
import fr.cnes.regards.modules.feature.dto.event.in.FeatureDeletionRequestEvent;
import fr.cnes.regards.modules.feature.service.AbstractFeatureMultitenantServiceIT;
import fr.cnes.regards.modules.feature.service.IFeatureService;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.concurrent.ExecutionException;

/**
 * Test class to check {@link PublishFeatureDeletionEventsJob}s and  {@link ScheduleFeatureDeletionJobsJob}s
 *
 * @author Sébastien Binda
 */
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=deletion_job_test",
                                   "regards.amqp.enabled=true",
                                   "regards.feature.deletion.notification.job.size=30" })
@ActiveProfiles(value = { "testAmqp", "noFemHandler", "noscheduler" })
public class ScheduleFeatureDeletionJobsJobIT extends AbstractFeatureMultitenantServiceIT {

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
        DeletionEventListener listener = new DeletionEventListener();
        subscriber.subscribeTo(FeatureDeletionRequestEvent.class, listener);

        // Initialize some feature
        initData(100);

        // Schedule global deletion job with feature filters that results to no feature
        JobInfo job = featureService.scheduleDeletionJob(new SearchFeatureSimpleEntityParameters().withSource("unknown"));
        if (job != null) {
            String tenant = runtimeTenantResolver.getTenant();
            jobService.runJob(job, tenant).get();
        }
        // No job should be scheduled
        Assert.assertEquals(
            "No PublishFeatureDeletionEventsJob should be scheduled as the feature selection should be empty",
            Long.valueOf(0L),
            jobInfoService.retrieveJobsCount(PublishFeatureDeletionEventsJob.class.getName()));
        Thread.sleep(1_000);
        Assert.assertEquals("No deletion request event should be sent", 0, listener.getNumberOfRequests());

        // Rerun schedule of deletion jobs for all features this time
        job = featureService.scheduleDeletionJob(new SearchFeatureSimpleEntityParameters());
        if (job != null) {
            String tenant = runtimeTenantResolver.getTenant();
            jobService.runJob(job, tenant).get();
        }
        // As the number of features to handle is 100 and each job should handle 30 features, there should be 4 jobs scheduled
        Assert.assertEquals("There should be 100/regards.feature.deletion.notification.job.size jobs scheduled ",
                            Long.valueOf(4L),
                            jobInfoService.retrieveJobsCount(PublishFeatureDeletionEventsJob.class.getName(),
                                                             JobStatus.TO_BE_RUN,
                                                             JobStatus.QUEUED,
                                                             JobStatus.RUNNING,
                                                             JobStatus.SUCCEEDED));
        int loop = 0;
        while ((listener.getNumberOfRequests() < 100) && (loop < 100)) {
            Thread.sleep(100);
            loop++;
        }
        Assert.assertEquals("All 100 feature should be notified", 100, listener.getNumberOfRequests());
    }

}
