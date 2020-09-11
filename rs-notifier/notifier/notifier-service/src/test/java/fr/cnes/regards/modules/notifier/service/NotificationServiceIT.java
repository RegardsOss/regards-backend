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

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import com.google.gson.JsonElement;
import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.amqp.domain.IHandler;
import fr.cnes.regards.framework.amqp.event.AbstractRequestEvent;
import fr.cnes.regards.framework.amqp.event.ISubscribable;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.domain.JobStatus;
import fr.cnes.regards.modules.notifier.domain.NotificationRequest;
import fr.cnes.regards.modules.notifier.domain.plugin.RecipientSender10;
import fr.cnes.regards.modules.notifier.domain.plugin.RecipientSender2;
import fr.cnes.regards.modules.notifier.domain.plugin.RecipientSender3;
import fr.cnes.regards.modules.notifier.domain.plugin.RecipientSender4;
import fr.cnes.regards.modules.notifier.domain.plugin.RecipientSender5;
import fr.cnes.regards.modules.notifier.domain.plugin.RecipientSender6;
import fr.cnes.regards.modules.notifier.domain.plugin.RecipientSender7;
import fr.cnes.regards.modules.notifier.domain.plugin.RecipientSender8;
import fr.cnes.regards.modules.notifier.domain.plugin.RecipientSender9;
import fr.cnes.regards.modules.notifier.dto.NotificationEvent10;
import fr.cnes.regards.modules.notifier.dto.NotificationEvent2;
import fr.cnes.regards.modules.notifier.dto.NotificationEvent3;
import fr.cnes.regards.modules.notifier.dto.NotificationEvent4;
import fr.cnes.regards.modules.notifier.dto.NotificationEvent5;
import fr.cnes.regards.modules.notifier.dto.NotificationEvent6;
import fr.cnes.regards.modules.notifier.dto.NotificationEvent7;
import fr.cnes.regards.modules.notifier.dto.NotificationEvent8;
import fr.cnes.regards.modules.notifier.dto.NotificationEvent9;
import fr.cnes.regards.modules.notifier.dto.out.NotificationState;
import static org.junit.Assert.assertEquals;

/**
 * Test class for service {@link NotificationRuleService}
 * @author kevin
 *
 */
@TestPropertySource(
        properties = { "spring.jpa.properties.hibernate.default_schema=notification", "regards.amqp.enabled=true",
                "spring.jpa.properties.hibernate.jdbc.batch_size=1024",
                "spring.jpa.properties.hibernate.order_inserts=true" })
@ActiveProfiles(value = { "testAmqp", "noscheduler" })
public class NotificationServiceIT extends AbstractNotificationMultitenantServiceTest {

    @Autowired
    private ISubscriber subscriber;

    @Override
    @Before
    public void before() throws InterruptedException {
        super.before();

        subOrNot(NotificationEvent2.class, new RecipientSender2());
        subOrNot(NotificationEvent3.class, new RecipientSender3());
        subOrNot(NotificationEvent4.class, new RecipientSender4());
        subOrNot(NotificationEvent5.class, new RecipientSender5());
        subOrNot(NotificationEvent6.class, new RecipientSender6());
        subOrNot(NotificationEvent7.class, new RecipientSender7());
        subOrNot(NotificationEvent8.class, new RecipientSender8());
        subOrNot(NotificationEvent9.class, new RecipientSender9());
        subOrNot(NotificationEvent10.class, new RecipientSender10());
    }

    private <E extends ISubscribable> void subOrNot(Class<E> eventType, IHandler<E> handler) {
        subscriber.subscribeTo(eventType, handler);
        subscriber.unsubscribeFrom(eventType);
    }

    /**
     * test process method it should work
     * @throws InterruptedException
     * @throws ModuleException
     */
    @Test
    public void testProcess() throws InterruptedException, ModuleException {

        // JobInfo created for test only we don't need a job start in this test
        JobInfo job = new JobInfo(false);
        job.setId(UUID.randomUUID());
        job.updateStatus(JobStatus.ABORTED);
        job = this.jobInforepo.save(job);

        JsonElement element = initElement("element.json");

        initPlugins(false);

        List<NotificationRequest> events = new ArrayList<>();
        int bulk = 0;
        for (int i = 0; i < EVENT_TO_RECEIVE; i++) {
            bulk++;
            events.add(new NotificationRequest(element,
                                               gson.toJsonTree("CREATE"),
                                               AbstractRequestEvent.generateRequestId(),
                                               OffsetDateTime.now(),
                                               NotificationState.GRANTED));
            if (bulk == EVENT_BULK) {
                bulk = 0;
                assertEquals(EVENT_BULK * RECIPIENTS_PER_RULE,
                             this.notificationService.processRequest(events, job.getId()).getFirst().intValue());
                events.clear();
            }
        }

        if (bulk > 0) {
            assertEquals(bulk * RECIPIENTS_PER_RULE,
                         this.notificationService.processRequest(events, job.getId()).getFirst().intValue());
        }
    }

    /**
     * In that test one the the fake RecipientSender will fail
     * @throws ModuleException
     */
    @Test
    public void testProcessWithFail() throws ModuleException {
        // JobInfo created for test only we don't need a job start in this test
        JobInfo job = new JobInfo(false);
        job.setId(UUID.randomUUID());
        job.updateStatus(JobStatus.ABORTED);
        job = this.jobInforepo.save(job);

        JsonElement modifiedElement = initElement("element.json");

        initPlugins(true);

        List<NotificationRequest> events = new ArrayList<>();
        for (int i = 0; i < EVENT_TO_RECEIVE; i++) {
            events.add(new NotificationRequest(modifiedElement,
                                               gson.toJsonTree("CREATE"),
                                               AbstractRequestEvent.generateRequestId(),
                                               OffsetDateTime.now(),
                                               NotificationState.GRANTED));
        }

        Pair<Integer, Integer> results = this.notificationService.processRequest(events, job.getId());
        assertEquals(EVENT_TO_RECEIVE * (RECIPIENTS_PER_RULE - 1), results.getFirst().intValue());
        assertEquals(EVENT_TO_RECEIVE, results.getSecond().intValue());
        assertEquals(EVENT_TO_RECEIVE, this.recipientErrorRepo.count());
    }

}
