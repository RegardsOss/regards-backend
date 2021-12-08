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
package fr.cnes.regards.modules.notifier.service;

import com.google.gson.JsonObject;
import fr.cnes.regards.framework.amqp.event.AbstractRequestEvent;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.utils.plugins.exception.NotAvailablePluginConfigurationException;
import fr.cnes.regards.modules.notifier.dto.in.NotificationRequestEvent;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.assertEquals;

/**
 * Performance tests on Notification manager.
 * Notifier should be able to send 10_000 notifications in 2 min
 *
 * @author Kevin Marchois
 */
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=notification_perf",
        "regards.amqp.enabled=true" },
        locations = { "classpath:regards_perf.properties", "classpath:batch.properties" })
@ActiveProfiles(value = { "testAmqp", "noscheduler" })
@Ignore("Perf test in local")
public class NotificationPerfIT extends AbstractNotificationMultitenantServiceTest {

    @Test
    public void testRegistrationAndProcessing()
            throws ExecutionException, NotAvailablePluginConfigurationException, ModuleException, InterruptedException {

        JsonObject element = initElement("elementRule1.json");

        initPlugins(false);

        List<NotificationRequestEvent> events = new ArrayList<>();
        for (int i = 0; i < configuration.getMaxBulkSize(); i++) {
            events.add(new NotificationRequestEvent(element,
                                                    gson.toJsonTree("{action: CREATE}").getAsJsonObject(),
                                                    AbstractRequestEvent.generateRequestId(),
                                                    "NotificationPerfIT"));
        }
        this.publisher.publish(events);
        // we should have  configuration.getMaxBulkSize() NotificationAction in database
        waitDatabaseCreation(this.notificationRequestRepository, configuration.getMaxBulkSize(), 60);
        //        this.notificationService.scheduleRequests();
        //TODO
        // all send should work so we should have 0 NotificationAction left in database
        waitDatabaseCreation(this.notificationRequestRepository, 0, 60);
        assertEquals(0, this.recipientErrorRepo.count());

    }

    @Test
    public void testRegistrationAndProcessingWith1RecipientFail()
            throws ExecutionException, NotAvailablePluginConfigurationException, ModuleException, InterruptedException {
        JsonObject element = initElement("elementRule1.json");
        initPlugins(true);

        List<NotificationRequestEvent> events = new ArrayList<NotificationRequestEvent>();
        for (int i = 0; i < configuration.getMaxBulkSize(); i++) {
            events.add(new NotificationRequestEvent(element,
                                                    gson.toJsonTree("{action: CREATE}").getAsJsonObject(),
                                                    AbstractRequestEvent.generateRequestId(),
                                                    "NotificationPerfIT"));
        }
        this.publisher.publish(events);
        // we should have  configuration.getMaxBulkSize() NotificationAction in database
        waitDatabaseCreation(this.notificationRequestRepository, configuration.getMaxBulkSize(), 60);
        //        this.notificationService.scheduleRequests();
        //TODO
        // we will wait util configuration.getMaxBulkSize() recipient errors are stored in database
        // cause one of the RECIPIENTS_PER_RULE will fail so we will get 1 error per NotificationAction to send
        waitDatabaseCreation(this.recipientErrorRepo, configuration.getMaxBulkSize(), 60);
        assertEquals(this.notificationRequestRepository.count(), configuration.getMaxBulkSize().intValue());

    }

    //    @Test
    //    public void testNotify() throws ModuleException, InterruptedException {
    //        //NotificationActionEvent element = getEvent("2352-template.json");
    //        //  initPlugins(true);
    //
    //        // publisher.publish(element);
    //        waitDatabaseCreation(this.notificationRepo, 1, 60);
    //    }
}
