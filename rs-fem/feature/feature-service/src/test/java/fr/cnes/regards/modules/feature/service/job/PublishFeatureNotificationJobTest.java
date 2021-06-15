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
import fr.cnes.regards.framework.modules.jobs.service.IJobService;
import fr.cnes.regards.modules.feature.dto.FeaturesSelectionDTO;
import fr.cnes.regards.modules.feature.dto.event.in.FeatureNotificationRequestEvent;
import fr.cnes.regards.modules.feature.service.AbstractFeatureMultitenantServiceTest;
import fr.cnes.regards.modules.feature.service.IFeatureService;

/**
 * Test class to check {@link PublishFeatureNotificationJob}s
 *
 * @author Sébastien Binda
 *
 */
@TestPropertySource(
        properties = { "spring.jpa.properties.hibernate.default_schema=notif_job_test", "regards.amqp.enabled=true" })
@ActiveProfiles(value = { "testAmqp", "noFemHandler", "noscheduler" })
public class PublishFeatureNotificationJobTest extends AbstractFeatureMultitenantServiceTest {

    @Autowired
    private IJobService jobService;

    @Autowired
    private IFeatureService featureService;

    @Autowired
    private ISubscriber subscriber;

    @Test
    public void notifySelection() throws InterruptedException, ExecutionException {
        cleanAMQPQueues(NotificationEventListener.class, ONE_PER_MICROSERVICE_TYPE);
        initData(100);
        NotificationEventListener listener = new NotificationEventListener();
        subscriber.subscribeTo(FeatureNotificationRequestEvent.class, listener);
        // Run Job and wait for end
        JobInfo job = featureService.scheduleNotificationsJob(FeaturesSelectionDTO.build().withSource("unknown"));
        if (job != null) {
            String tenant = runtimeTenantResolver.getTenant();
            jobService.runJob(job, tenant).get();
        }
        Assert.assertEquals(0L, listener.getNumberOfRequests());
        // Run Job and wait for end
        job = featureService.scheduleNotificationsJob(FeaturesSelectionDTO.build());
        if (job != null) {
            String tenant = runtimeTenantResolver.getTenant();
            jobService.runJob(job, tenant).get();
        }
        Thread.sleep(5_000);
        Assert.assertEquals(100L, listener.getNumberOfRequests());
    }

}
