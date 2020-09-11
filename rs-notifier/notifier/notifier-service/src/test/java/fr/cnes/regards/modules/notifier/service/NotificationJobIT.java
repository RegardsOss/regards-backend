/*
 * Copyright 2017-2020 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.notifier.service;

import fr.cnes.regards.framework.amqp.event.AbstractRequestEvent;
import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import com.google.gson.JsonElement;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.domain.JobStatus;
import fr.cnes.regards.modules.notifier.domain.NotificationRequest;
import fr.cnes.regards.modules.notifier.domain.RecipientError;
import fr.cnes.regards.modules.notifier.dto.in.NotificationActionEvent;

/**
 * Test notification job restart after recipient failure
 * @author Kevin Marchois
 *
 */
@TestPropertySource(
        properties = { "spring.jpa.properties.hibernate.default_schema=notification_job", "regards.amqp.enabled=true" },
        locations = { "classpath:regards_perf.properties", "classpath:batch.properties" })
@ActiveProfiles(value = { "testAmqp", "noscheduler" })
public class NotificationJobIT extends AbstractNotificationMultitenantServiceTest {

    /**
     * Test notification job restart after {@link Recipient} fail
     * After the job fail we will restart ir then the {@link Recipient} should work
     * All {@link RecipientError} and {@link NotificationRequest} must be deleted
     * @throws InterruptedException
     * @throws ModuleException
     */
    @Test
    public void testRestartAfterFailure() throws InterruptedException, ModuleException {

        JsonElement element = initElement("element.json");

        initPlugins(true);

        List<NotificationActionEvent> events = new ArrayList<NotificationActionEvent>();
        for (int i = 0; i < configuration.getMaxBulkSize(); i++) {
            events.add(new NotificationActionEvent(element, gson.toJsonTree("CREATE"),
                                                     AbstractRequestEvent.generateRequestId(),
                                                     "NotificationPerfIT"));
        }
        this.publisher.publish(events);
        // we should have  configuration.getMaxBulkSize() NotificationAction in database
        waitDatabaseCreation(this.notificationRepo, configuration.getMaxBulkSize(), 60);
        this.notificationService.scheduleRequests();
        // we will wait until configuration.getMaxBulkSize() recipient errors are stored in database
        // cause one of the RECIPIENTS_PER_RULE will fail so we will get 1 error per NotificationAction to send
        waitDatabaseCreation(this.recipientErrorRepo, configuration.getMaxBulkSize(), 60);
        // waiting for job end
        Thread.sleep(5000);
        JobInfo failJob = this.jobInforepo.findAll().iterator().next();
        failJob.updateStatus(JobStatus.QUEUED);
        // this static variable will make recipient plugin succeed/fail according it value
        RECIPIENT_FAIL = false;
        this.jobInforepo.save(failJob);
        waitDatabaseCreation(this.recipientErrorRepo, 0, 60);
        waitDatabaseCreation(this.notificationRepo, 0, 60);
        assertEquals(JobStatus.SUCCEEDED, this.jobInforepo.findAll().iterator().next().getStatus().getStatus());
    }
}
