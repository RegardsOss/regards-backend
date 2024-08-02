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
package fr.cnes.regards.modules.feature.service.job;

import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.service.IJobService;
import fr.cnes.regards.modules.feature.domain.RecipientsSearchFeatureSimpleEntityParameters;
import fr.cnes.regards.modules.feature.domain.SearchFeatureSimpleEntityParameters;
import fr.cnes.regards.modules.feature.dto.event.in.FeatureNotificationRequestEvent;
import fr.cnes.regards.modules.feature.service.AbstractFeatureMultitenantServiceIT;
import fr.cnes.regards.modules.feature.service.IFeatureService;
import org.awaitility.Awaitility;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * Test class to check {@link PublishFeatureNotificationJob}s
 *
 * @author Sébastien Binda
 */
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=notif_job_test",
                                   "regards.amqp.enabled=true" })
@ActiveProfiles(value = { "testAmqp", "noFemHandler", "noscheduler" })
public class PublishFeatureNotificationJobIT extends AbstractFeatureMultitenantServiceIT {

    private static final String RECIPIENT_BUSINNES_ID0 = "recipient_business_id0";

    private static final String RECIPIENT_BUSINNES_ID1 = "recipient_business_id1";

    @Autowired
    private IJobService jobService;

    @Autowired
    private IFeatureService featureService;

    @Autowired
    private ISubscriber subscriber;

    @Test
    public void test_notifySelection() throws InterruptedException, ExecutionException {
        // Given
        initData(10);
        NotificationEventListener listener = new NotificationEventListener();
        subscriber.subscribeTo(FeatureNotificationRequestEvent.class, listener);

        RecipientsSearchFeatureSimpleEntityParameters recipientsSearchFeatureSimpleEntityParameters = new RecipientsSearchFeatureSimpleEntityParameters();
        recipientsSearchFeatureSimpleEntityParameters.setSearchParameters(new SearchFeatureSimpleEntityParameters().withSource(
            "unknown"));
        // When : run Job and wait for end
        JobInfo job = featureService.scheduleNotificationsJob(recipientsSearchFeatureSimpleEntityParameters);
        if (job != null) {
            String tenant = runtimeTenantResolver.getTenant();
            jobService.runJob(job, tenant).get();
        }
        // Then
        Assert.assertEquals(0L, listener.getNumberOfRequests());

        // Given
        recipientsSearchFeatureSimpleEntityParameters.setSearchParameters(new SearchFeatureSimpleEntityParameters());
        // When : run Job and wait for end
        job = featureService.scheduleNotificationsJob(recipientsSearchFeatureSimpleEntityParameters);
        if (job != null) {
            String tenant = runtimeTenantResolver.getTenant();
            jobService.runJob(job, tenant).get();
        }

        // Then
        Awaitility.await().atMost(5, TimeUnit.SECONDS).until(() -> {
            LOGGER.info("Listener count {}/{}", listener.getNumberOfRequests(), 10);
            return listener.getNumberOfRequests() == 10L;
        });
    }

    @Test
    public void test_notifySelection_with_recipients() throws InterruptedException, ExecutionException {
        // Given
        initData(2);
        NotificationEventListener listener = new NotificationEventListener();
        subscriber.subscribeTo(FeatureNotificationRequestEvent.class, listener);

        RecipientsSearchFeatureSimpleEntityParameters recipientsSearchFeatureSimpleEntityParameters = new RecipientsSearchFeatureSimpleEntityParameters();
        recipientsSearchFeatureSimpleEntityParameters.setSearchParameters(new SearchFeatureSimpleEntityParameters());
        recipientsSearchFeatureSimpleEntityParameters.setRecipientIds(Set.of(RECIPIENT_BUSINNES_ID0,
                                                                             RECIPIENT_BUSINNES_ID1));
        // When : run Job and wait for end
        JobInfo job = featureService.scheduleNotificationsJob(recipientsSearchFeatureSimpleEntityParameters);
        if (job != null) {
            String tenant = runtimeTenantResolver.getTenant();
            jobService.runJob(job, tenant).get();
        }
        Awaitility.await().atMost(5, TimeUnit.SECONDS).until(() -> {
            LOGGER.info("listener {}/{}", listener.getNumberOfRequests(), 2);
            return listener.getNumberOfRequests() == 2;
        });
        // Then
        Assert.assertEquals(2L, listener.getNumberOfRequests());
        Assert.assertEquals(2L, listener.getMessages().size());
        // Check the FeatureNotificationRequestEvent contains the list of recipients
        checkFeatureNotificationRequestEvent(listener.getMessages().get(0));
        checkFeatureNotificationRequestEvent(listener.getMessages().get(1));
    }

    private void checkFeatureNotificationRequestEvent(FeatureNotificationRequestEvent featureNotificationRequestEvent) {
        Assert.assertNotNull(featureNotificationRequestEvent);
        Assert.assertEquals(2L, featureNotificationRequestEvent.getRecipients().size());
        Assert.assertTrue(featureNotificationRequestEvent.getRecipients().contains(RECIPIENT_BUSINNES_ID0));
        Assert.assertTrue(featureNotificationRequestEvent.getRecipients().contains(RECIPIENT_BUSINNES_ID1));
    }

}
