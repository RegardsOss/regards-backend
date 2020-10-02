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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.gson.JsonElement;
import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.amqp.domain.IHandler;
import fr.cnes.regards.framework.amqp.event.AbstractRequestEvent;
import fr.cnes.regards.framework.amqp.event.ISubscribable;
import fr.cnes.regards.framework.encryption.exception.EncryptionException;
import fr.cnes.regards.framework.module.rest.exception.EntityInvalidException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.domain.parameter.IPluginParam;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
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
import fr.cnes.regards.modules.notifier.dto.RuleDTO;
import fr.cnes.regards.modules.notifier.dto.in.NotificationRequestEvent;
import fr.cnes.regards.modules.notifier.dto.out.NotificationState;
import fr.cnes.regards.modules.notifier.service.conf.NotificationConfigurationProperties;
import fr.cnes.regards.modules.notifier.service.plugin.DefaultRuleMatcher;

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

    private static final String RECIPIENTR1_1_LABEL = "recipientR1_1";

    private static final String RECIPIENTR1_2_LABEL = "recipientR1_3";

    private static final String RECIPIENTR2_1_LABEL = "recipientR2_1";

    private static final String RULE1_LABEL = "r1";

    private static final String RULE2_LABEL = "r2";

    @Autowired
    private NotificationConfigurationProperties properties;

    @Autowired
    private ISubscriber subscriber;

    @Autowired
    private IPluginService pluginService;

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

    /**
     * In tests we are going to use a rule that will send AMQP messages to multiple queues.
     * In order to be able to clean them at the end of the tests and check the content during the test, we need to subscribe to these events.
     * This allows that queues are created. But we do not care about receiving these messages so we unsubscribe right after that
     */
    private <E extends ISubscribable> void subOrNot(Class<E> eventType, IHandler<E> handler) {
        subscriber.subscribeTo(eventType, handler);
        subscriber.unsubscribeFrom(eventType);
    }

    @Test
    public void testRegisterNotificationRequests()
            throws EncryptionException, EntityNotFoundException, EntityInvalidException {
        // We mainly want to test what happens when we receive events both for retry and a first notification
        // So lets create a few notification requests and events based on them
        PluginConfiguration recipientR1_1 = pluginService.savePluginConfiguration(new PluginConfiguration(
                RECIPIENTR1_1_LABEL,
                new HashSet<>(),
                RecipientSender2.class.getAnnotation(Plugin.class).id()));
        int nbEventForRetry = properties.getMaxBulkSize() / 2;
        List<NotificationRequest> notificationRequestsToRetry = new ArrayList<>(nbEventForRetry);
        Set<NotificationRequestEvent> eventForRetry = new HashSet<>(nbEventForRetry);
        JsonElement payloadMatchR1 = initElement("elementRule1.json");
        for (int i = 0; i < nbEventForRetry; i++) {
            NotificationRequest toRetry = new NotificationRequest(payloadMatchR1,
                                                                  gson.toJsonTree("RETRY"),
                                                                  AbstractRequestEvent.generateRequestId(),
                                                                  OffsetDateTime.now(),
                                                                  NotificationState.ERROR);
            toRetry.getRecipientsInError().add(recipientR1_1);
            notificationRequestsToRetry.add(toRetry);
            eventForRetry.add(new NotificationRequestEvent(toRetry.getPayload(),
                                                           toRetry.getMetadata(),
                                                           toRetry.getRequestId(),
                                                           "tests"));
        }
        notificationRequestsToRetry = notificationRepo.saveAll(notificationRequestsToRetry);
        // Let create some events that are not based on already known requests
        Set<NotificationRequestEvent> firstTime = new HashSet<>(properties.getMaxBulkSize() - nbEventForRetry);
        JsonElement payloadMatchR2 = initElement("elementRule1.json");
        for (int i = 0; i < properties.getMaxBulkSize() - nbEventForRetry; i++) {
            firstTime.add(new NotificationRequestEvent(payloadMatchR2,
                                                       gson.toJsonTree("FIRST_TIME"),
                                                       AbstractRequestEvent.generateRequestId(),
                                                       "tests"));
        }
        // now lets register everything at once
        List<NotificationRequestEvent> toRegister = Lists.newArrayList(firstTime);
        toRegister.addAll(eventForRetry);
        notificationService.registerNotificationRequests(toRegister);
        // lets check that notification to retry have been properly handled
        notificationRequestsToRetry = notificationRepo
                .findAllById(notificationRequestsToRetry.stream().map(NotificationRequest::getId)
                                     .collect(Collectors.toList()));
        Assert.assertTrue("All notification to retry should be in state " + NotificationState.TO_SCHEDULE_BY_RECIPIENT,
                          notificationRequestsToRetry.stream()
                                  .allMatch(r -> r.getState() == NotificationState.TO_SCHEDULE_BY_RECIPIENT));
        for (NotificationRequest toRetry : notificationRequestsToRetry) {
            Assert.assertEquals("There should be only 1 recipient to schedule",
                                1,
                                toRetry.getRecipientsToSchedule().size());
            Assert.assertTrue("recipientR1_1 should be to schedule for requests to retry",
                              toRetry.getRecipientsToSchedule().contains(recipientR1_1));
            Assert.assertTrue("There should be no more errors among retries", toRetry.getRecipientsInError().isEmpty());
            Assert.assertTrue("There should be no recipient already scheduled among retries",
                              toRetry.getRecipientsScheduled().isEmpty());
        }
        // lets check requests that were just created
        Set<NotificationRequest> newRequests = notificationRepo.findAllByStateAndRequestIdIn(NotificationState.GRANTED,
                                                                                             firstTime.stream()
                                                                                                    .map(NotificationRequestEvent::getRequestId)
                                                                                                    .collect(Collectors
                                                                                                                     .toSet()));
        Assert.assertEquals("Not all new requests could be created properly",
                            properties.getMaxBulkSize() - nbEventForRetry,
                            newRequests.size());
        for (NotificationRequest newRequest : newRequests) {
            Assert.assertTrue("There should be no recipient to schedule yet among new requests",
                              newRequest.getRecipientsToSchedule().isEmpty());
            Assert.assertTrue("There should be no recipient in error among new requests",
                              newRequest.getRecipientsInError().isEmpty());
            Assert.assertTrue("There should be no recipient already scheduled among new requests",
                              newRequest.getRecipientsScheduled().isEmpty());
        }
    }

    @Test
    public void testMatchRequestNRecipient() throws ModuleException {
        // Init two rules with multiple recipients
        PluginConfiguration recipientR1_1 = pluginService.savePluginConfiguration(new PluginConfiguration(
                RECIPIENTR1_1_LABEL,
                new HashSet<>(),
                RecipientSender2.class.getAnnotation(Plugin.class).id()));
        PluginConfiguration recipientR1_2 = pluginService.savePluginConfiguration(new PluginConfiguration(
                RECIPIENTR1_2_LABEL,
                new HashSet<>(),
                RecipientSender3.class.getAnnotation(Plugin.class).id()));
        PluginConfiguration recipientR2_1 = pluginService.savePluginConfiguration(new PluginConfiguration(
                RECIPIENTR2_1_LABEL,
                new HashSet<>(),
                RecipientSender4.class.getAnnotation(Plugin.class).id()));
        PluginConfiguration rule1 = new PluginConfiguration(RULE1_LABEL,
                                                            Sets.newHashSet(IPluginParam
                                                                                    .build(DefaultRuleMatcher.ATTRIBUTE_TO_SEEK_FIELD_NAME,
                                                                                           "nature"),
                                                                            IPluginParam
                                                                                    .build(DefaultRuleMatcher.ATTRIBUTE_VALUE_TO_SEEK_FIELD_NAME,
                                                                                           "TM")),
                                                            DefaultRuleMatcher.class.getAnnotation(Plugin.class).id());
        PluginConfiguration rule2 = new PluginConfiguration(RULE2_LABEL,
                                                            Sets.newHashSet(IPluginParam
                                                                                    .build(DefaultRuleMatcher.ATTRIBUTE_TO_SEEK_FIELD_NAME,
                                                                                           "info"),
                                                                            IPluginParam
                                                                                    .build(DefaultRuleMatcher.ATTRIBUTE_VALUE_TO_SEEK_FIELD_NAME,
                                                                                           "toto")),
                                                            DefaultRuleMatcher.class.getAnnotation(Plugin.class).id());
        ruleService.createOrUpdateRule(RuleDTO.build(rule1,
                                                     Sets.newHashSet(recipientR1_1.getBusinessId(),
                                                                     recipientR1_2.getBusinessId())));
        ruleService.createOrUpdateRule(RuleDTO.build(rule2, Sets.newHashSet(recipientR2_1.getBusinessId())));
        // Create notification requests that will be matched by rules
        // Lets add a guide for tests, as we are creating 4 different types of requests, lets divide the maxBulkSize in 4
        // For simplicity of test, it better be a multiple of 4
        Assert.assertEquals(
                "Test are made to rule properly only with a max bulk size which is a multiple of 4. If you want feel free to change tests accordingly",
                0,
                properties.getMaxBulkSize() % 4);
        int nbOfRequestPerType = properties.getMaxBulkSize() / 4;
        // Lets create notification requests that should only be matched by first rule
        JsonElement elementRule1 = initElement("elementRule1.json");
        List<NotificationRequest> first = new ArrayList<>(nbOfRequestPerType);
        for (int i = 0; i < nbOfRequestPerType; i++) {
            first.add(new NotificationRequest(elementRule1,
                                              gson.toJsonTree("SomeMetaWeDontCareAbout"),
                                              AbstractRequestEvent.generateRequestId(),
                                              OffsetDateTime.now(),
                                              NotificationState.GRANTED));
        }
        first = notificationRepo.saveAll(first);
        // Lets create notification requests that should only be matched by second rule
        JsonElement elementRule2 = initElement("elementRule2.json");
        List<NotificationRequest> second = new ArrayList<>(nbOfRequestPerType);
        for (int i = 0; i < nbOfRequestPerType; i++) {
            second.add(new NotificationRequest(elementRule2,
                                               gson.toJsonTree("SomeMetaWeDontCareAbout"),
                                               AbstractRequestEvent.generateRequestId(),
                                               OffsetDateTime.now(),
                                               NotificationState.GRANTED));
        }
        second = notificationRepo.saveAll(second);
        // Lets create notification requests that should be matched by both rules
        JsonElement elementBothRule = initElement("elementBothRule.json");
        List<NotificationRequest> both = new ArrayList<>(nbOfRequestPerType);
        for (int i = 0; i < nbOfRequestPerType; i++) {
            both.add(new NotificationRequest(elementBothRule,
                                             gson.toJsonTree("SomeMetaWeDontCareAbout"),
                                             AbstractRequestEvent.generateRequestId(),
                                             OffsetDateTime.now(),
                                             NotificationState.GRANTED));
        }
        both = notificationRepo.saveAll(both);
        // Lets create notification requests that should not be matched by any rules
        JsonElement elementNoneRule = initElement("elementNoneRule.json");
        List<NotificationRequest> none = new ArrayList<>(nbOfRequestPerType);
        for (int i = 0; i < nbOfRequestPerType; i++) {
            none.add(new NotificationRequest(elementNoneRule,
                                             gson.toJsonTree("SomeMetaWeDontCareAbout"),
                                             AbstractRequestEvent.generateRequestId(),
                                             OffsetDateTime.now(),
                                             NotificationState.GRANTED));
        }
        none = notificationRepo.saveAll(none);
        // lets finally call the method to test
        Pair<Integer, Integer> matchResult = notificationService.matchRequestNRecipient();
        Assert.assertEquals("We should say that only 3 fourth of the requests were matched",
                            nbOfRequestPerType * 3,
                            matchResult.getFirst().intValue());
        Assert.assertEquals("We should say that no errors occurred during matching",
                            0,
                            matchResult.getSecond().intValue());
        // check that only first, second and both requests were placed to NotificationState.TO_SCHEDULE_BY_RECIPIENT
        first = notificationRepo
                .findAllById(first.stream().map(NotificationRequest::getId).collect(Collectors.toList()));
        second = notificationRepo
                .findAllById(second.stream().map(NotificationRequest::getId).collect(Collectors.toList()));
        both = notificationRepo.findAllById(both.stream().map(NotificationRequest::getId).collect(Collectors.toList()));
        Assert.assertTrue(String.format("All requests matching only rule1 should be in state %s",
                                        NotificationState.TO_SCHEDULE_BY_RECIPIENT),
                          first.stream().allMatch(request -> request.getState()
                                  == NotificationState.TO_SCHEDULE_BY_RECIPIENT));
        Assert.assertTrue(String.format("All requests matching only rule2 should be in state %s",
                                        NotificationState.TO_SCHEDULE_BY_RECIPIENT),
                          second.stream().allMatch(request -> request.getState()
                                  == NotificationState.TO_SCHEDULE_BY_RECIPIENT));
        Assert.assertTrue(String.format("All requests matching both rules should be in state %s",
                                        NotificationState.TO_SCHEDULE_BY_RECIPIENT),
                          both.stream().allMatch(request -> request.getState()
                                  == NotificationState.TO_SCHEDULE_BY_RECIPIENT));
        // check that first, second and both requests has the right recipients associated
        for (NotificationRequest matchRule1 : first) {
            Assert.assertTrue("There was no error so why is there a recipient in error?",
                              matchRule1.getRecipientsInError().isEmpty());
            Assert.assertTrue("There should be no recipients scheduled yet",
                              matchRule1.getRecipientsScheduled().isEmpty());
            Assert.assertEquals("Requests matching rule1 should have 2 recipients to schedule",
                                2,
                                matchRule1.getRecipientsToSchedule().size());
            Assert.assertTrue("Requests matching rule1 should have recipientR1_1 to schedule",
                              matchRule1.getRecipientsToSchedule().contains(recipientR1_1));
            Assert.assertTrue("Requests matching rule1 should have recipientR1_2 to schedule",
                              matchRule1.getRecipientsToSchedule().contains(recipientR1_2));
        }
        for (NotificationRequest matchRule2 : second) {
            Assert.assertTrue("There was no error so why is there a recipient in error?",
                              matchRule2.getRecipientsInError().isEmpty());
            Assert.assertTrue("There should be no recipients scheduled yet",
                              matchRule2.getRecipientsScheduled().isEmpty());
            Assert.assertEquals("Requests matching rule2 should have 1 recipients to schedule",
                                1,
                                matchRule2.getRecipientsToSchedule().size());
            Assert.assertTrue("Requests matching rule2 should have recipientR2_1 to schedule",
                              matchRule2.getRecipientsToSchedule().contains(recipientR2_1));
        }
        for (NotificationRequest matchBothRule : both) {
            Assert.assertTrue("There was no error so why is there a recipient in error?",
                              matchBothRule.getRecipientsInError().isEmpty());
            Assert.assertTrue("There should be no recipients scheduled yet",
                              matchBothRule.getRecipientsScheduled().isEmpty());
            Assert.assertEquals("Requests matching both rules should have 3(2+1) recipients to schedule",
                                3,
                                matchBothRule.getRecipientsToSchedule().size());
            Assert.assertTrue("Requests matching both rules should have recipientR1_1 to schedule",
                              matchBothRule.getRecipientsToSchedule().contains(recipientR1_1));
            Assert.assertTrue("Requests matching both rules should have recipientR1_2 to schedule",
                              matchBothRule.getRecipientsToSchedule().contains(recipientR1_2));
            Assert.assertTrue("Requests matching both rules should have recipientR2_1 to schedule",
                              matchBothRule.getRecipientsToSchedule().contains(recipientR2_1));
        }
        // check that none requests were placed to NotificationState.SCHEDULED and contains no recipient
        none = notificationRepo.findAllById(none.stream().map(NotificationRequest::getId).collect(Collectors.toList()));
        Assert.assertTrue(String.format("All requests matching none of the rule should be in state %s",
                                        NotificationState.SCHEDULED),
                          none.stream().allMatch(request -> request.getState() == NotificationState.SCHEDULED));
        Assert.assertTrue("All requests matching none of the rule should not have any recipient associated",
                          none.stream().allMatch(request -> request.getRecipientsScheduled().isEmpty() && request
                                  .getRecipientsInError().isEmpty() && request.getRecipientsToSchedule().isEmpty()));
    }

    @Test
    public void testScheduleJobForOneRecipient()
            throws EncryptionException, EntityNotFoundException, EntityInvalidException {
        // create notification request that should be scheduled for recipientR1_1 and recipientR1_2
        PluginConfiguration recipientR1_1 = pluginService.savePluginConfiguration(new PluginConfiguration(
                RECIPIENTR1_1_LABEL,
                new HashSet<>(),
                RecipientSender2.class.getAnnotation(Plugin.class).id()));
        PluginConfiguration recipientR1_2 = pluginService.savePluginConfiguration(new PluginConfiguration(
                RECIPIENTR1_2_LABEL,
                new HashSet<>(),
                RecipientSender3.class.getAnnotation(Plugin.class).id()));
        JsonElement matchR1 = initElement("elementRule1.json");
        List<NotificationRequest> requestsToSchedule = new ArrayList<>(properties.getMaxBulkSize());
        for (int i = 0; i < properties.getMaxBulkSize(); i++) {
            NotificationRequest toSchedule = new NotificationRequest(matchR1,
                                                                     gson.toJsonTree("DC"),
                                                                     AbstractRequestEvent.generateRequestId(),
                                                                     OffsetDateTime.now(),
                                                                     NotificationState.TO_SCHEDULE_BY_RECIPIENT);
            toSchedule.getRecipientsToSchedule().add(recipientR1_1);
            toSchedule.getRecipientsToSchedule().add(recipientR1_2);
            requestsToSchedule.add(toSchedule);
        }
        requestsToSchedule = notificationRepo.saveAll(requestsToSchedule);
        // then schedule only for recipientR1_1
        Set<Long> idsScheduled = notificationService.scheduleJobForOneRecipient(recipientR1_1);
        // check that requests are still in state NotificationState.TO_SCHEDULE_BY_RECIPIENT
        List<NotificationRequest> scheduledRequests = notificationRepo.findAllById(idsScheduled);
        Assert.assertTrue("Scheduled requests should contains all requests to schedule",
                          scheduledRequests.containsAll(requestsToSchedule));
        Assert.assertTrue("Requests to schedule requests should contains all scheduled requests",
                          requestsToSchedule.containsAll(scheduledRequests));
        Assert.assertTrue(
                "All scheduled requests should still be in state " + NotificationState.TO_SCHEDULE_BY_RECIPIENT,
                scheduledRequests.stream().allMatch(r -> r.getState() == NotificationState.TO_SCHEDULE_BY_RECIPIENT));
        for(NotificationRequest scheduled: scheduledRequests) {
            Assert.assertTrue("There should be no error",scheduled.getRecipientsInError().isEmpty());
            // check that recipientR1_1 has been moved from toSchedule to scheduled
            Assert.assertEquals("There should be only 1 recipient scheduled", 1, scheduled.getRecipientsScheduled().size());
            Assert.assertTrue("Scheduled request should have recipientR1_1 as scheduled recipient", scheduled.getRecipientsScheduled().contains(recipientR1_1));
            // check that recipientR1_2 is still to schedule
            Assert.assertEquals("There should be only 1 recipient to schedule", 1, scheduled.getRecipientsToSchedule().size());
            Assert.assertTrue("Scheduled request should have recipientR1_2 as to schedule recipient", scheduled.getRecipientsToSchedule().contains(recipientR1_2));
        }
    }

    public void testProcessRequest() {
        //TODO
    }

    public void testProcessWithFail() {
        //TODO
    }

    @Test
    public void testCheckSuccess() throws EncryptionException, EntityNotFoundException, EntityInvalidException {
        // we just need to create notification requests in state SCHEDULED and with no more recipients associated to check the success
        int nbSuccess = properties.getMaxBulkSize() / 2;
        JsonElement matchR1 = initElement("elementRule1.json");
        List<NotificationRequest> requestsInSuccess = new ArrayList<>(nbSuccess);
        for (int i = 0; i < nbSuccess; i++) {
            requestsInSuccess.add(new NotificationRequest(matchR1,
                                                          gson.toJsonTree("SomeMetaWeDontCareAbout"),
                                                          AbstractRequestEvent.generateRequestId(),
                                                          OffsetDateTime.now(),
                                                          NotificationState.SCHEDULED));
        }
        requestsInSuccess = notificationRepo.saveAll(requestsInSuccess);
        // we should add some notification with only recipients scheduled
        PluginConfiguration recipientR1_2 = pluginService.savePluginConfiguration(new PluginConfiguration(
                RECIPIENTR1_2_LABEL,
                new HashSet<>(),
                RecipientSender3.class.getAnnotation(Plugin.class).id()));
        List<NotificationRequest> notYetInSuccess = new ArrayList<>(properties.getMaxBulkSize() - nbSuccess);
        for (int i = 0; i < properties.getMaxBulkSize() - nbSuccess; i++) {
            NotificationRequest notYet = new NotificationRequest(matchR1,
                                                                 gson.toJsonTree("DontCare"),
                                                                 AbstractRequestEvent.generateRequestId(),
                                                                 OffsetDateTime.now(),
                                                                 NotificationState.SCHEDULED);
            notYet.getRecipientsScheduled().add(recipientR1_2);
            notYetInSuccess.add(notYet);
        }
        notYetInSuccess = notificationRepo.saveAll(notYetInSuccess);
        int result = notificationService.checkSuccess();
        Assert.assertEquals("Not the right amount of success detected!", nbSuccess, result);
        //lets check that it is really requestsInSuccess that have been identified as it i.e. they do not exists anymore
        Assert.assertEquals("Requests in state " + NotificationState.SCHEDULED
                                    + " with no recipients associated should no longer be in DB",
                            0,
                            notificationRepo.findAllById(requestsInSuccess.stream().map(NotificationRequest::getId)
                                                                 .collect(Collectors.toList())).size());
        // check that requests not yet in success have not been altered
        notYetInSuccess = notificationRepo
                .findAllById(notYetInSuccess.stream().map(NotificationRequest::getId).collect(Collectors.toList()));
        Assert.assertEquals("There should be the same number fo not yet in success requests than before",
                            properties.getMaxBulkSize() - nbSuccess,
                            notYetInSuccess.size());
        for (NotificationRequest notYet : notYetInSuccess) {
            Assert.assertEquals("requests not yet in success should still be in state " + NotificationState.SCHEDULED,
                                NotificationState.SCHEDULED,
                                notYet.getState());
            Assert.assertEquals("There should still be 1 recipient scheduled for requests not yet in success",
                                1,
                                notYet.getRecipientsScheduled().size());
            Assert.assertTrue("recipientR1_2 should still be scheduled for request not yet in success",
                              notYet.getRecipientsScheduled().contains(recipientR1_2));
            Assert.assertTrue("Request not yet in success should have no recipients in error",
                              notYet.getRecipientsInError().isEmpty());
            Assert.assertTrue("Request not yet in success should have no recipients to schedule",
                              notYet.getRecipientsToSchedule().isEmpty());
        }
    }

}
