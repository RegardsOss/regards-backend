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

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.amqp.domain.IHandler;
import fr.cnes.regards.framework.amqp.event.AbstractRequestEvent;
import fr.cnes.regards.framework.amqp.event.ISubscribable;
import fr.cnes.regards.framework.encryption.exception.EncryptionException;
import fr.cnes.regards.framework.module.rest.exception.EntityInvalidException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.service.IJobInfoService;
import fr.cnes.regards.framework.modules.jobs.service.IJobService;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.domain.parameter.IPluginParam;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.modules.notifier.dao.INotificationRequestRepository;
import fr.cnes.regards.modules.notifier.domain.NotificationRequest;
import fr.cnes.regards.modules.notifier.domain.Rule;
import fr.cnes.regards.modules.notifier.domain.plugin.*;
import fr.cnes.regards.modules.notifier.dto.*;
import fr.cnes.regards.modules.notifier.dto.in.NotificationRequestEvent;
import fr.cnes.regards.modules.notifier.dto.out.NotificationState;
import fr.cnes.regards.modules.notifier.dto.out.NotifierEvent;
import fr.cnes.regards.modules.notifier.dto.out.Recipient;
import fr.cnes.regards.modules.notifier.dto.out.RecipientStatus;
import fr.cnes.regards.modules.notifier.service.conf.NotificationConfigurationProperties;
import fr.cnes.regards.modules.notifier.service.job.NotificationJob;
import fr.cnes.regards.modules.notifier.service.plugin.DefaultRuleMatcher;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.AdditionalAnswers;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockReset;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.data.util.Pair;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test class for service {@link NotificationRegistrationService}
 * @author Sylvain Vissiere-Guerinet
 *
 */
@TestPropertySource(
        properties = {
                "spring.jpa.properties.hibernate.default_schema=notification_service_it",
                "regards.amqp.enabled=true",
                "spring.jpa.properties.hibernate.jdbc.batch_size=1024",
                "spring.jpa.properties.hibernate.order_inserts=true",
                "regards.notifier.max.bulk.size=20"
        })
@ActiveProfiles(value = { "testAmqp", "noscheduler" })
public class NotificationServiceIT extends AbstractNotificationMultitenantServiceTest {

    private static final String RECIPIENT_R1_1_LABEL = "recipientR1_1";

    private static final String RECIPIENT_R1_2_LABEL = "recipientR1_2";

    private static final String RECIPIENT_R2_1_LABEL = "recipientR2_1";

    private static final String RULE1_LABEL = "r1";

    private static final String RULE2_LABEL = "r2";

    @Autowired
    private NotificationConfigurationProperties properties;

    @Autowired
    private ISubscriber subscriber;

    @SpyBean
    private IPluginService pluginService;

    @Autowired
    private IJobService jobService;

    @Autowired
    private TestService testService;

    @SpyBean
    private IJobInfoService jobInfoService;

    @Captor
    private ArgumentCaptor<List<NotifierEvent>> notifierEventCaptor;

    private final TestNotificationMetadata globalMetadata = TestNotificationMetadata.build("value");

    @Override
    @Before
    public void before() throws Exception {
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

        // stop job daemon
        ReflectionTestUtils.setField(jobService, "canManage", false);
    }

    /**
     * In tests we are going to use a rule that will send AMQP messages to multiple queues.
     * In order to be able to clean them at the end of the tests and check the content during the test, we need to subscribe to these events.
     * This allows that queues are created. But we do not care about receiving these messages so we unsubscribe right after that
     */
    private <E extends ISubscribable> void subOrNot(Class<E> eventType, IHandler<E> handler) {
        subscriber.subscribeTo(eventType, handler);
        subscriber.unsubscribeFrom(eventType, true);
    }

    @Test
    public void testRegisterNotificationRequests() throws Exception {
        // Init two rules with multiple recipients
        Init2Rule3Recipient twoRules3Recipients = new Init2Rule3Recipient(false);
        PluginConfiguration recipientR1_1 = twoRules3Recipients.getRecipientR1_1();
        Rule rule1 = twoRules3Recipients.getRule1();
        Rule rule2 = twoRules3Recipients.getRule2();
        int nbEventForRetry = 10;
        List<NotificationRequest> notificationRequestsToRetry_AllRuleMatchOneRecipientError = new ArrayList<>(
                nbEventForRetry / 3);
        Set<NotificationRequestEvent> eventForRetry = new HashSet<>(nbEventForRetry);
        JsonObject payloadMatchR1 = initElement("elementRule1.json");
        // we want to simulate that some requests could be matched by all rules but one recipient was in error
        for (int i = 0; i < nbEventForRetry / 3; i++) {
            NotificationRequest toRetry = new NotificationRequest(payloadMatchR1,
                                                                  gson.toJsonTree(globalMetadata).getAsJsonObject(),
                                                                  AbstractRequestEvent.generateRequestId(),
                                                                  this.getClass().getSimpleName(),
                                                                  OffsetDateTime.now(),
                                                                  NotificationState.ERROR,
                                                                  new HashSet<>());
            toRetry.getRecipientsInError().add(recipientR1_1);
            notificationRequestsToRetry_AllRuleMatchOneRecipientError.add(toRetry);
            eventForRetry.add(new NotificationRequestEvent(toRetry.getPayload(),
                                                           toRetry.getMetadata(),
                                                           toRetry.getRequestId(),
                                                           REQUEST_OWNER));
        }
        notificationRequestsToRetry_AllRuleMatchOneRecipientError = notificationRequestRepository
                .saveAll(notificationRequestsToRetry_AllRuleMatchOneRecipientError);
        // we want to simulate that some requests are still pending and cannot be retried
        List<NotificationRequest> notRetriableRequests = new ArrayList<>(
                nbEventForRetry / 3);
        for (int i = 0; i < nbEventForRetry / 3; i++) {
            NotificationRequest toRetry = new NotificationRequest(payloadMatchR1,
                                                                  gson.toJsonTree(globalMetadata).getAsJsonObject(),
                                                                  AbstractRequestEvent.generateRequestId(),
                                                                  this.getClass().getSimpleName(),
                                                                  OffsetDateTime.now(),
                                                                  NotificationState.SCHEDULED,
                                                                  Sets.newHashSet(rule2));
            notRetriableRequests.add(toRetry);
            eventForRetry.add(new NotificationRequestEvent(toRetry.getPayload(),
                                                           toRetry.getMetadata(),
                                                           toRetry.getRequestId(),
                                                           REQUEST_OWNER));
        }
        notRetriableRequests = notificationRequestRepository
                .saveAll(notRetriableRequests);
        // we want to simulate that some requests could not be matched by all rules and one recipient was in error
        List<NotificationRequest> notificationRequestsToRetry_RuleNotMatchedOneRecipientError = new ArrayList<>(
                nbEventForRetry / 3 + nbEventForRetry % 3);
        for (int i = 0; i < nbEventForRetry / 3 + nbEventForRetry % 3; i++) {
            NotificationRequest toRetry = new NotificationRequest(payloadMatchR1,
                                                                  gson.toJsonTree(
                                                                          globalMetadata).getAsJsonObject(),
                                                                  AbstractRequestEvent.generateRequestId(),
                                                                  this.getClass().getSimpleName(),
                                                                  OffsetDateTime.now(),
                                                                  NotificationState.ERROR,
                                                                  Sets.newHashSet(rule2));
            toRetry.getRecipientsInError().add(recipientR1_1);
            notificationRequestsToRetry_RuleNotMatchedOneRecipientError.add(toRetry);
            eventForRetry.add(new NotificationRequestEvent(toRetry.getPayload(),
                                                           toRetry.getMetadata(),
                                                           toRetry.getRequestId(),
                                                           REQUEST_OWNER));
        }
        notificationRequestsToRetry_RuleNotMatchedOneRecipientError = notificationRequestRepository
                .saveAll(notificationRequestsToRetry_RuleNotMatchedOneRecipientError);

        // Let create some events that are not based on already known requests
        Set<NotificationRequestEvent> firstTime = new HashSet<>(properties.getMaxBulkSize() - nbEventForRetry);
        JsonObject payloadMatchR2 = initElement("elementRule2.json");
        for (int i = 0; i < properties.getMaxBulkSize() - nbEventForRetry; i++) {
            firstTime.add(new NotificationRequestEvent(payloadMatchR2,
                                                       gson.toJsonTree(globalMetadata).getAsJsonObject(),
                                                       AbstractRequestEvent.generateRequestId(),
                                                       REQUEST_OWNER));
        }
        // now lets register everything at once
        List<NotificationRequestEvent> toRegister = Lists.newArrayList(firstTime);
        toRegister.addAll(eventForRetry);
        notificationRegistrationService.registerNotificationRequests(toRegister);

        // lets check that notification to retry have been properly handled
        // requests that could be matched by all rules but one recipient was in error
        notificationRequestsToRetry_AllRuleMatchOneRecipientError = notificationRequestRepository
                .findAllById(notificationRequestsToRetry_AllRuleMatchOneRecipientError.stream()
                        .map(NotificationRequest::getId).collect(Collectors.toList()));
        Assert.assertTrue("All notification to retry should be in state " + NotificationState.TO_SCHEDULE_BY_RECIPIENT,
                notificationRequestsToRetry_AllRuleMatchOneRecipientError.stream()
                        .allMatch(r -> r.getState() == NotificationState.TO_SCHEDULE_BY_RECIPIENT));
        for (NotificationRequest toRetry : notificationRequestsToRetry_AllRuleMatchOneRecipientError) {
            Assert.assertEquals("There should be only 1 recipient to schedule",
                                1,
                                toRetry.getRecipientsToSchedule().size());
            Assert.assertTrue("recipientR1_1 should be to schedule for requests to retry",
                              toRetry.getRecipientsToSchedule().contains(recipientR1_1));
            Assert.assertTrue("There should be no more errors among retries", toRetry.getRecipientsInError().isEmpty());
            Assert.assertTrue("There should be no recipient already scheduled among retries",
                              toRetry.getRecipientsScheduled().isEmpty());
            Assert.assertTrue("There should be no rule to match", toRetry.getRulesToMatch().isEmpty());
        }
        // requests that could not be matched by all rules but no recipients in error
        notRetriableRequests = notificationRequestRepository
                .findAllById(notRetriableRequests.stream()
                        .map(NotificationRequest::getId).collect(Collectors.toList()));
        Assert.assertTrue("All notification to retry should be in state " + NotificationState.GRANTED,
                          notRetriableRequests.stream()
                        .allMatch(r -> r.getState() == NotificationState.GRANTED));
        for (NotificationRequest toRetry : notRetriableRequests) {
            Assert.assertTrue("There should be no recipient to schedule", toRetry.getRecipientsToSchedule().isEmpty());
            Assert.assertTrue("There should be no more errors among retries", toRetry.getRecipientsInError().isEmpty());
            Assert.assertTrue("There should be no recipient already scheduled among retries",
                              toRetry.getRecipientsScheduled().isEmpty());
            Assert.assertEquals("There should be only 1 rule to match", 1, toRetry.getRulesToMatch().size());
            Assert.assertTrue("rule2 should be to match for requests to retry",
                              toRetry.getRulesToMatch().contains(rule2));
        }
        // requests that could not be matched by all rules and one recipient was in error
        notificationRequestsToRetry_RuleNotMatchedOneRecipientError = notificationRequestRepository
                .findAllById(notificationRequestsToRetry_RuleNotMatchedOneRecipientError.stream()
                        .map(NotificationRequest::getId).collect(Collectors.toList()));
        Assert.assertTrue("All notification to retry should be in state " + NotificationState.GRANTED,
                notificationRequestsToRetry_RuleNotMatchedOneRecipientError.stream()
                        .allMatch(r -> r.getState() == NotificationState.GRANTED));
        for (NotificationRequest toRetry : notificationRequestsToRetry_RuleNotMatchedOneRecipientError) {
            Assert.assertEquals("There should be only 1 recipient to schedule",
                                1,
                                toRetry.getRecipientsToSchedule().size());
            Assert.assertTrue("recipientR1_1 should be to schedule for requests to retry",
                              toRetry.getRecipientsToSchedule().contains(recipientR1_1));
            Assert.assertTrue("There should be no more errors among retries", toRetry.getRecipientsInError().isEmpty());
            Assert.assertTrue("There should be no recipient already scheduled among retries",
                              toRetry.getRecipientsScheduled().isEmpty());
            Assert.assertEquals("There should be only 1 rule to match", 1, toRetry.getRulesToMatch().size());
            Assert.assertTrue("rule2 should be to match for requests to retry",
                              toRetry.getRulesToMatch().contains(rule2));
        }

        // lets check requests that were just created
        Set<NotificationRequest> newRequests = notificationRequestRepository
                .findAllByRequestIdIn(firstTime.stream().map(NotificationRequestEvent::getRequestId)
                        .collect(Collectors.toSet()));
        Assert.assertEquals("Not all new requests could be created properly",
                            properties.getMaxBulkSize() - nbEventForRetry,
                            newRequests.size());
        Assert.assertTrue("All new notification requests should be in state " + NotificationState.GRANTED,
                          newRequests.stream().allMatch(r -> r.getState() == NotificationState.GRANTED));
        for (NotificationRequest newRequest : newRequests) {
            Assert.assertEquals("New request should have 2 rules to match", 2, newRequest.getRulesToMatch().size());
            Assert.assertTrue("New request should have rule1 to match", newRequest.getRulesToMatch().contains(rule1));
            Assert.assertTrue("New request should have rule2 to match", newRequest.getRulesToMatch().contains(rule2));
            Assert.assertTrue("There should be no recipient to schedule yet among new requests",
                              newRequest.getRecipientsToSchedule().isEmpty());
            Assert.assertTrue("There should be no recipient in error among new requests",
                              newRequest.getRecipientsInError().isEmpty());
            Assert.assertTrue("There should be no recipient already scheduled among new requests",
                              newRequest.getRecipientsScheduled().isEmpty());
        }
    }

    @Test
    public void testMatchRequestNRecipient() throws Exception {
        // Init two rules with multiple recipients
        Init2Rule3Recipient twoRules3Recipients = new Init2Rule3Recipient(false);
        PluginConfiguration recipientR1_1 = twoRules3Recipients.getRecipientR1_1();
        PluginConfiguration recipientR1_2 = twoRules3Recipients.getRecipientR1_2();
        PluginConfiguration recipientR2_1 = twoRules3Recipients.getRecipientR2_1();
        Set<Rule> rules = ruleCache.getRules();
        // Create notification requests that will be matched by rules
        // Lets add a guide for tests, as we are creating 4 different types of requests, lets divide the maxBulkSize in 4
        // For simplicity of test, it better be a multiple of 4
        Assert.assertEquals(
                "Test are made to rule properly only with a max bulk size which is a multiple of 4. If you want feel free to change tests accordingly",
                0,
                properties.getMaxBulkSize() % 4);
        int nbOfRequestPerType = properties.getMaxBulkSize() / 4;
        // Lets create notification requests that should only be matched by first rule
        JsonObject elementRule1 = initElement("elementRule1.json");
        List<NotificationRequest> first = new ArrayList<>(nbOfRequestPerType);
        for (int i = 0; i < nbOfRequestPerType; i++) {
            first.add(new NotificationRequest(elementRule1,
                                              gson.toJsonTree(globalMetadata).getAsJsonObject(),
                                              AbstractRequestEvent.generateRequestId(),
                                              REQUEST_OWNER,
                                              OffsetDateTime.now(),
                                              NotificationState.GRANTED,
                                              rules));
        }
        first = notificationRequestRepository.saveAll(first);
        // Lets create notification requests that should only be matched by second rule
        JsonObject elementRule2 = initElement("elementRule2.json");
        List<NotificationRequest> second = new ArrayList<>(nbOfRequestPerType);
        for (int i = 0; i < nbOfRequestPerType; i++) {
            second.add(new NotificationRequest(elementRule2,
                                               gson.toJsonTree(globalMetadata).getAsJsonObject(),
                                               AbstractRequestEvent.generateRequestId(),
                                               REQUEST_OWNER,
                                               OffsetDateTime.now(),
                                               NotificationState.GRANTED,
                                               rules));
        }
        second = notificationRequestRepository.saveAll(second);
        // Lets create notification requests that should be matched by both rules
        JsonObject elementBothRule = initElement("elementBothRule.json");
        List<NotificationRequest> both = new ArrayList<>(nbOfRequestPerType);
        for (int i = 0; i < nbOfRequestPerType; i++) {
            both.add(new NotificationRequest(elementBothRule,
                                             gson.toJsonTree(globalMetadata).getAsJsonObject(),
                                             AbstractRequestEvent.generateRequestId(),
                                             REQUEST_OWNER,
                                             OffsetDateTime.now(),
                                             NotificationState.GRANTED,
                                             rules));
        }
        both = notificationRequestRepository.saveAll(both);
        // Lets create notification requests that should not be matched by any rules
        JsonObject elementNoneRule = initElement("elementNoneRule.json");
        List<NotificationRequest> none = new ArrayList<>(nbOfRequestPerType);
        for (int i = 0; i < nbOfRequestPerType; i++) {
            none.add(new NotificationRequest(elementNoneRule,
                                             gson.toJsonTree(globalMetadata).getAsJsonObject(),
                                             AbstractRequestEvent.generateRequestId(),
                                             REQUEST_OWNER,
                                             OffsetDateTime.now(),
                                             NotificationState.GRANTED,
                                             rules));
        }
        none = notificationRequestRepository.saveAll(none);
        // lets finally call the method to test
        Pair<Integer, Integer> matchResult = notificationMatchingService.matchRequestNRecipient();
        Assert.assertEquals("We should say that only 3 fourth of the requests were matched",
                            nbOfRequestPerType * 3,
                            matchResult.getFirst().intValue());
        Assert.assertEquals("We should have successfully matched to 3 recipients",
                            3,
                            matchResult.getSecond().intValue());
        // check that only first, second and both requests were placed to NotificationState.TO_SCHEDULE_BY_RECIPIENT
        first = notificationRequestRepository
                .findAllById(first.stream().map(NotificationRequest::getId).collect(Collectors.toList()));
        second = notificationRequestRepository
                .findAllById(second.stream().map(NotificationRequest::getId).collect(Collectors.toList()));
        both = notificationRequestRepository.findAllById(both.stream().map(NotificationRequest::getId).collect(Collectors.toList()));
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
            Assert.assertTrue("Requests matching rule1 should have no rules to match left",
                              matchRule1.getRulesToMatch().isEmpty());
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
            Assert.assertTrue("Requests matching rule2 should have no rules to match left",
                              matchRule2.getRulesToMatch().isEmpty());
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
            Assert.assertTrue("Requests matching both rules should have no rules to match left",
                              matchBothRule.getRulesToMatch().isEmpty());
        }
        // check that none requests were placed to NotificationState.SCHEDULED and contains no recipient
        none = notificationRequestRepository.findAllById(none.stream().map(NotificationRequest::getId).collect(Collectors.toList()));
        Assert.assertTrue(String.format("All requests matching none of the rule should be in state %s",
                        NotificationState.SCHEDULED),
                none.stream().allMatch(request -> request.getState() == NotificationState.SCHEDULED));
        Assert.assertTrue(
                "All requests matching none of the rule should not have any recipient associated and no more rules to match",
                none.stream().allMatch(request -> request.getRecipientsScheduled().isEmpty() && request
                        .getRecipientsInError().isEmpty() && request.getRecipientsToSchedule().isEmpty() && request
                        .getRulesToMatch().isEmpty()));
    }

    @Test
    public void testScheduleJobForOneRecipient()
            throws EncryptionException, EntityNotFoundException, EntityInvalidException {
        // create notification request that should be scheduled for recipientR1_1 and recipientR1_2
        PluginConfiguration recipientR1_1 = pluginService.savePluginConfiguration(new PluginConfiguration(
                RECIPIENT_R1_1_LABEL,
                new HashSet<>(),
                RecipientSender2.PLUGIN_ID));
        PluginConfiguration recipientR1_2 = pluginService.savePluginConfiguration(new PluginConfiguration(
                RECIPIENT_R1_2_LABEL,
                new HashSet<>(),
                RecipientSender3.PLUGIN_ID));
        JsonObject matchR1 = initElement("elementRule1.json");
        List<NotificationRequest> requestsToSchedule = new ArrayList<>(properties.getMaxBulkSize());
        for (int i = 0; i < properties.getMaxBulkSize(); i++) {
            NotificationRequest toSchedule = new NotificationRequest(matchR1,
                                                                     gson.toJsonTree(globalMetadata).getAsJsonObject(),
                                                                     AbstractRequestEvent.generateRequestId(),
                                                                     REQUEST_OWNER,
                                                                     OffsetDateTime.now(),
                                                                     NotificationState.TO_SCHEDULE_BY_RECIPIENT,
                                                                     new HashSet<>());
            toSchedule.getRecipientsToSchedule().add(recipientR1_1);
            toSchedule.getRecipientsToSchedule().add(recipientR1_2);
            requestsToSchedule.add(toSchedule);
        }
        requestsToSchedule = notificationRequestRepository.saveAll(requestsToSchedule);
        // then schedule only for recipientR1_1
        Set<Long> idsScheduled = notificationProcessingService.scheduleJobForOneRecipient(recipientR1_1);
        // check that requests are still in state NotificationState.TO_SCHEDULE_BY_RECIPIENT
        List<NotificationRequest> scheduledRequests = notificationRequestRepository.findAllById(idsScheduled);
        Assert.assertTrue("Scheduled requests should contains all requests to schedule",
                          scheduledRequests.containsAll(requestsToSchedule));
        Assert.assertTrue("Requests to schedule requests should contains all scheduled requests",
                          requestsToSchedule.containsAll(scheduledRequests));
        Assert.assertTrue(
                "All scheduled requests should still be in state " + NotificationState.TO_SCHEDULE_BY_RECIPIENT,
                scheduledRequests.stream().allMatch(r -> r.getState() == NotificationState.TO_SCHEDULE_BY_RECIPIENT));
        for (NotificationRequest scheduled : scheduledRequests) {
            Assert.assertTrue("There should be no error", scheduled.getRecipientsInError().isEmpty());
            // check that recipientR1_1 has been moved from toSchedule to scheduled
            Assert.assertEquals("There should be only 1 recipient scheduled",
                                1,
                                scheduled.getRecipientsScheduled().size());
            Assert.assertTrue("Scheduled request should have recipientR1_1 as scheduled recipient",
                              scheduled.getRecipientsScheduled().contains(recipientR1_1));
            // check that recipientR1_2 is still to schedule
            Assert.assertEquals("There should be only 1 recipient to schedule",
                                1,
                                scheduled.getRecipientsToSchedule().size());
            Assert.assertTrue("Scheduled request should have recipientR1_2 as to schedule recipient",
                              scheduled.getRecipientsToSchedule().contains(recipientR1_2));
            Assert.assertTrue("There should be no rules to match", scheduled.getRulesToMatch().isEmpty());
        }
    }

    @Test
    public void testScheduleJobForTwoRecipientConcurrent()
            throws EncryptionException, EntityNotFoundException, EntityInvalidException, InterruptedException {
        // create notification request that should be scheduled for recipientR1_1 and recipientR1_2
        PluginConfiguration recipientR1_1 = pluginService.savePluginConfiguration(new PluginConfiguration(
                RECIPIENT_R1_1_LABEL,
                new HashSet<>(),
                RecipientSender2.PLUGIN_ID));
        PluginConfiguration recipientR1_2 = pluginService.savePluginConfiguration(new PluginConfiguration(
                RECIPIENT_R1_2_LABEL,
                new HashSet<>(),
                RecipientSender3.PLUGIN_ID));
        JsonObject matchR1 = initElement("elementRule1.json");
        List<NotificationRequest> requestsToSchedule = new ArrayList<>(properties.getMaxBulkSize());
        for (int i = 0; i < properties.getMaxBulkSize(); i++) {
            NotificationRequest toSchedule = new NotificationRequest(matchR1,
                                                                     gson.toJsonTree(globalMetadata).getAsJsonObject(),
                                                                     AbstractRequestEvent.generateRequestId(),
                                                                     REQUEST_OWNER,
                                                                     OffsetDateTime.now(),
                                                                     NotificationState.TO_SCHEDULE_BY_RECIPIENT,
                                                                     new HashSet<>());
            toSchedule.getRecipientsToSchedule().add(recipientR1_1);
            toSchedule.getRecipientsToSchedule().add(recipientR1_2);
            requestsToSchedule.add(toSchedule);
        }
        requestsToSchedule = notificationRequestRepository.saveAll(requestsToSchedule);
        // then schedule for both recipientR1_1 and recipientR1_2 in parallel
        ExecutorService executor = Executors.newFixedThreadPool(2);
        executor.execute(() -> {
            runtimeTenantResolver.forceTenant(getDefaultTenant());
            notificationProcessingService.scheduleJobForOneRecipient(recipientR1_1);
        });
        executor.execute(() -> {
            runtimeTenantResolver.forceTenant(getDefaultTenant());
            notificationProcessingService.scheduleJobForOneRecipient(recipientR1_2);
        });
        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.MINUTES);
        // check method was called 3 times (2 times according to before and one more time because of concurrency)
        Mockito.verify(notificationProcessingService, Mockito.times(3))
                .scheduleJobForOneRecipientConcurrent(Mockito.any(), Mockito.anyList());
        // check that requests are still in state NotificationState.TO_SCHEDULE_BY_RECIPIENT
        List<NotificationRequest> scheduledRequests = notificationRequestRepository
                .findAllById(requestsToSchedule.stream().map(NotificationRequest::getId).collect(Collectors.toSet()));
        Assert.assertEquals("Scheduled requests should contains all requests to schedule",
                            requestsToSchedule.size(),
                            scheduledRequests.size());
        Assert.assertTrue(
                "All scheduled requests should still be in state " + NotificationState.SCHEDULED,
                scheduledRequests.stream().allMatch(r -> r.getState() == NotificationState.SCHEDULED));
        for (NotificationRequest scheduled : scheduledRequests) {
            Assert.assertTrue("There should be no error", scheduled.getRecipientsInError().isEmpty());
            // check that recipientR1_1 & recipientR1_2 has been moved from toSchedule to scheduled
            Assert.assertEquals("There should be 2 recipients scheduled", 2, scheduled.getRecipientsScheduled().size());
            Assert.assertTrue("Scheduled request should have recipientR1_1 scheduled",
                              scheduled.getRecipientsScheduled().contains(recipientR1_1));
            Assert.assertTrue("Scheduled request should have recipientR1_2 scheduled",
                              scheduled.getRecipientsScheduled().contains(recipientR1_2));
            // check that nothing more is to be scheduled
            Assert.assertEquals("There should be no more recipients to schedule",
                                0,
                                scheduled.getRecipientsToSchedule().size());
            Assert.assertTrue("There should be no rules to match", scheduled.getRulesToMatch().isEmpty());
        }
    }

    @Test
    public void testProcessRequest() throws EncryptionException, EntityNotFoundException, EntityInvalidException {
        // create notification request that will be processed with recipientR1_1 and recipientR1_2 scheduled
        PluginConfiguration recipientR1_1 = pluginService.savePluginConfiguration(new PluginConfiguration(
                RECIPIENT_R1_1_LABEL,
                new HashSet<>(),
                RecipientSender2.PLUGIN_ID));
        PluginConfiguration recipientR1_2 = pluginService.savePluginConfiguration(new PluginConfiguration(
                RECIPIENT_R1_2_LABEL,
                new HashSet<>(),
                RecipientSender3.PLUGIN_ID));
        JsonObject matchR1 = initElement("elementRule1.json");
        List<NotificationRequest> toProcess = new ArrayList<>(properties.getMaxBulkSize());
        for (int i = 0; i < properties.getMaxBulkSize(); i++) {
            NotificationRequest toSchedule = new NotificationRequest(matchR1,
                                                                     gson.toJsonTree(globalMetadata).getAsJsonObject(),
                                                                     AbstractRequestEvent.generateRequestId(),
                                                                     REQUEST_OWNER,
                                                                     OffsetDateTime.now(),
                                                                     NotificationState.TO_SCHEDULE_BY_RECIPIENT,
                                                                     new HashSet<>());
            toSchedule.getRecipientsScheduled().add(recipientR1_1);
            toSchedule.getRecipientsScheduled().add(recipientR1_2);
            toProcess.add(toSchedule);
        }
        toProcess = notificationRequestRepository.saveAll(toProcess);
        // then process only for recipientR1_1
        notificationProcessingService.processRequests(toProcess, recipientR1_1);
        // check that requests are still in state NotificationState.TO_SCHEDULE_BY_RECIPIENT
        List<NotificationRequest> requestsProcessed = notificationRequestRepository
                .findAllById(toProcess.stream().map(NotificationRequest::getId).collect(Collectors.toSet()));
        Assert.assertTrue("Scheduled requests should contains all requests to schedule",
                          requestsProcessed.containsAll(toProcess));
        Assert.assertTrue("Requests to schedule requests should contains all scheduled requests",
                          toProcess.containsAll(requestsProcessed));
        Assert.assertTrue(
                "All scheduled requests should still be in state " + NotificationState.TO_SCHEDULE_BY_RECIPIENT,
                requestsProcessed.stream().allMatch(r -> r.getState() == NotificationState.TO_SCHEDULE_BY_RECIPIENT));
        for (NotificationRequest processed : requestsProcessed) {
            Assert.assertTrue("There should be no error", processed.getRecipientsInError().isEmpty());
            // check that recipientR1_1 has been processed i.e. no more in scheduled
            Assert.assertEquals("There should be only 1 recipient scheduled",
                                1,
                                processed.getRecipientsScheduled().size());
            Assert.assertTrue("Scheduled request should have recipientR1_2 as scheduled recipient",
                              processed.getRecipientsScheduled().contains(recipientR1_2));
            // check that nothing is to be scheduled (as previously)
            Assert.assertTrue("There should be no recipient to schedule",
                              processed.getRecipientsToSchedule().isEmpty());
            Assert.assertTrue("There should be no rules to match", processed.getRulesToMatch().isEmpty());
        }
    }

    @Test
    public void testProcessRequestFail() throws EncryptionException, EntityNotFoundException, EntityInvalidException {
        // create notification request that will be processed with recipientR1_1(which will fail) and recipientR1_2 scheduled
        PluginConfiguration recipientR1_1 = pluginService.savePluginConfiguration(new PluginConfiguration(
                RECIPIENT_R1_1_LABEL,
                new HashSet<>(),
                RecipientSenderFail.PLUGIN_ID));
        PluginConfiguration recipientR1_2 = pluginService.savePluginConfiguration(new PluginConfiguration(
                RECIPIENT_R1_2_LABEL,
                new HashSet<>(),
                RecipientSender3.PLUGIN_ID));
        JsonObject matchR1 = initElement("elementRule1.json");
        Integer nbRequests = 10; //properties.getMaxBulkSize();
        List<NotificationRequest> toProcess = new ArrayList<>(properties.getMaxBulkSize());
        for (int i = 0; i < nbRequests ; i++) {
            NotificationRequest toSchedule = new NotificationRequest(matchR1,
                                                                     gson.toJsonTree(globalMetadata).getAsJsonObject(),
                                                                     AbstractRequestEvent.generateRequestId(),
                                                                     REQUEST_OWNER,
                                                                     OffsetDateTime.now(),
                                                                     NotificationState.SCHEDULED,
                                                                     new HashSet<>());
            toSchedule.getRecipientsScheduled().add(recipientR1_1);
            toSchedule.getRecipientsScheduled().add(recipientR1_2);
            toProcess.add(toSchedule);
        }
        toProcess = notificationRequestRepository.saveAll(toProcess);
        // then process only for recipientR1_1
        Pair<Integer, Integer> result = notificationProcessingService.processRequests(toProcess, recipientR1_1);
        Assert.assertEquals("Invalid number of successes recipients",0 , result.getFirst().intValue());
        Assert.assertEquals("Invalid number of errors recipients", nbRequests , result.getSecond());
        notificationProcessingService.checkCompletedRequests();
        // check that requests are still in state NotificationState.TO_SCHEDULE_BY_RECIPIENT
        List<NotificationRequest> requestsProcessed = notificationRequestRepository
                .findAllById(toProcess.stream().map(NotificationRequest::getId).collect(Collectors.toSet()));
        Assert.assertTrue("Scheduled requests should contains all requests to schedule",
                          requestsProcessed.containsAll(toProcess));
        Assert.assertTrue("Requests to schedule requests should contains all scheduled requests",
                          toProcess.containsAll(requestsProcessed));
        Assert.assertTrue("All scheduled requests should still be in state " + NotificationState.SCHEDULED,
                          requestsProcessed.stream().allMatch(r -> r.getState() == NotificationState.SCHEDULED));
        for (NotificationRequest processed : requestsProcessed) {
            Assert.assertEquals("There should be one error", 1, processed.getRecipientsInError().size());
            Assert.assertTrue("The error should be recipientR1_1",
                              processed.getRecipientsInError().contains(recipientR1_1));
            // check that recipientR1_1 has been processed i.e. no more in scheduled
            Assert.assertEquals("There should be only 1 recipient scheduled",
                                1,
                                processed.getRecipientsScheduled().size());
            Assert.assertTrue("Scheduled request should have recipientR1_2 as scheduled recipient",
                              processed.getRecipientsScheduled().contains(recipientR1_2));
            // check that nothing is to be scheduled (as previously)
            Assert.assertTrue("There should be no recipient to schedule",
                              processed.getRecipientsToSchedule().isEmpty());
            Assert.assertTrue("There should be no rules to match", processed.getRulesToMatch().isEmpty());
        }
    }

    @Test
    public void testProcessRequestConcurrent() throws EncryptionException, EntityNotFoundException, EntityInvalidException, InterruptedException {

        // create notification request that will be processed with recipientR1_1(which will fail) and recipientR1_2 scheduled
        PluginConfiguration recipientR1_1 = pluginService.savePluginConfiguration(new PluginConfiguration(
                RECIPIENT_R1_1_LABEL,
                new HashSet<>(),
                RecipientSenderFail.PLUGIN_ID));
        PluginConfiguration recipientR1_2 = pluginService.savePluginConfiguration(new PluginConfiguration(
                RECIPIENT_R1_2_LABEL,
                new HashSet<>(),
                RecipientSender3.PLUGIN_ID));
        JsonObject matchR1 = initElement("elementRule1.json");
        List<NotificationRequest> toProcess = new ArrayList<>(properties.getMaxBulkSize());
        for (int i = 0; i < configuration.getMaxBulkSize() / 2; i++) {
            NotificationRequest toSchedule = new NotificationRequest(matchR1,
                                                                     gson.toJsonTree(globalMetadata).getAsJsonObject(),
                                                                     AbstractRequestEvent.generateRequestId(),
                                                                     REQUEST_OWNER,
                                                                     OffsetDateTime.now(),
                                                                     NotificationState.SCHEDULED,
                                                                     new HashSet<>());
            toSchedule.getRecipientsScheduled().add(recipientR1_1);
            toSchedule.getRecipientsScheduled().add(recipientR1_2);
            toProcess.add(toSchedule);
        }
        toProcess = notificationRequestRepository.saveAll(toProcess);
        final List<NotificationRequest> finalToProcess = toProcess;
        // then process for both recipientR1_1(will fail) and recipientR1_2 in parallel
        ExecutorService executor = Executors.newFixedThreadPool(2);
        executor.execute(() -> {
            runtimeTenantResolver.forceTenant(getDefaultTenant());
            notificationProcessingService.processRequests(finalToProcess, recipientR1_1);
        });
        executor.execute(() -> {
            runtimeTenantResolver.forceTenant(getDefaultTenant());
            notificationProcessingService.processRequests(finalToProcess, recipientR1_2);
        });
        executor.shutdown();
        Assert.assertTrue("Time out waiting for requests processed", executor.awaitTermination(1, TimeUnit.MINUTES));
        notificationProcessingService.checkCompletedRequests();
        Mockito.verify(notificationProcessingService, Mockito.times(3))
                .handleRecipientResults(Mockito.any(), Mockito.any(), Mockito.any());
        // check that requests are still in state NotificationState.TO_SCHEDULE_BY_RECIPIENT
        List<NotificationRequest> requestsProcessed = notificationRequestRepository
                .findAllById(toProcess.stream().map(NotificationRequest::getId).collect(Collectors.toSet()));
        Assert.assertTrue("Scheduled requests should contains all requests to schedule",
                          requestsProcessed.containsAll(toProcess));
        Assert.assertTrue("Requests to schedule requests should contains all scheduled requests",
                          toProcess.containsAll(requestsProcessed));
        Assert.assertTrue("All scheduled requests should still be in state " + NotificationState.ERROR,
                          requestsProcessed.stream().allMatch(r -> r.getState() == NotificationState.ERROR));
        for (NotificationRequest processed : requestsProcessed) {
            Assert.assertEquals("There should be one error", 1, processed.getRecipientsInError().size());
            Assert.assertTrue("The error should be recipientR1_1",
                              processed.getRecipientsInError().contains(recipientR1_1));
            // check that recipientR1_1 and recipient R1_2 has been processed i.e. no more in scheduled
            Assert.assertEquals("There should be no more recipients scheduled",
                                0,
                                processed.getRecipientsScheduled().size());
            // check that nothing is to be scheduled (as previously)
            Assert.assertTrue("There should be no recipient to schedule",
                              processed.getRecipientsToSchedule().isEmpty());
            Assert.assertTrue("There should be no rules to match", processed.getRulesToMatch().isEmpty());
        }
    }

    @Test
    public void testCheckCompleted() throws EncryptionException, EntityNotFoundException, EntityInvalidException {
        // we just need to create notification requests in state SCHEDULED and with no more recipients associated to check the success
        int nbCompleted = properties.getMaxBulkSize() / 2;
        JsonObject matchR1 = initElement("elementRule1.json");
        PluginConfiguration recipientR1_2 = pluginService.savePluginConfiguration(new PluginConfiguration(
                RECIPIENT_R1_2_LABEL,
                new HashSet<>(),
                RecipientSender3.PLUGIN_ID));
        List<NotificationRequest> completed = new ArrayList<>(nbCompleted);
        JsonElement metadata = gson.toJsonTree(globalMetadata);
        int nbError = 0;
        for (int i = 0; i < nbCompleted; i++) {
            switch (i%3) {
                case 0:
                    completed.add(
                        new NotificationRequest(matchR1,metadata.getAsJsonObject() , AbstractRequestEvent.generateRequestId(), REQUEST_OWNER,
                                                OffsetDateTime.now(), NotificationState.SCHEDULED, new HashSet<>()));
                    break;
                case 1:
                    NotificationRequest inSuccess = new NotificationRequest(matchR1,metadata.getAsJsonObject(), AbstractRequestEvent.generateRequestId(), REQUEST_OWNER,
                                            OffsetDateTime.now(), NotificationState.SCHEDULED, new HashSet<>());
                    inSuccess.getSuccessRecipients().add(recipientR1_2);
                    completed.add(inSuccess);
                    break;
                case 2:
                default:
                    NotificationRequest inError = new NotificationRequest(matchR1, metadata.getAsJsonObject(), AbstractRequestEvent.generateRequestId(), REQUEST_OWNER,
                                                                            OffsetDateTime.now(), NotificationState.SCHEDULED, new HashSet<>());
                    inError.getRecipientsInError().add(recipientR1_2);
                    completed.add(inError);
                    nbError++;
                    break;
            }
        }
        completed = notificationRequestRepository.saveAll(completed);
        List<NotificationRequest> notYetCompleted = new ArrayList<>(properties.getMaxBulkSize() - nbCompleted);
        for (int i = 0; i < properties.getMaxBulkSize() - nbCompleted; i++) {
            NotificationRequest notYet = new NotificationRequest(matchR1,
                                                                 gson.toJsonTree(globalMetadata).getAsJsonObject(),
                                                                 AbstractRequestEvent.generateRequestId(),
                                                                 REQUEST_OWNER,
                                                                 OffsetDateTime.now(),
                                                                 NotificationState.SCHEDULED,
                                                                 new HashSet<>());
            notYet.getRecipientsScheduled().add(recipientR1_2);
            notYetCompleted.add(notYet);
        }
        notYetCompleted = notificationRequestRepository.saveAll(notYetCompleted);
        Pair<Integer, Integer> result = notificationProcessingService.checkCompletedRequests();
        Assert.assertEquals("Not the right amount of completed detected!", nbCompleted, result.getFirst() + result.getSecond());
        // let's check that it is really requestsInSuccess that have been identified as it i.e. they do not exist anymore
        Assert.assertEquals("Requests in state " + NotificationState.SCHEDULED
                        + " with no recipients associated should no longer be in DB",
                nbError,
                notificationRequestRepository.findAllById(completed.stream().map(NotificationRequest::getId)
                        .collect(Collectors.toList())).size());
        // check that requests not yet in success have not been altered
        notYetCompleted = notificationRequestRepository
                .findAllById(notYetCompleted.stream().map(NotificationRequest::getId).collect(Collectors.toList()));
        Assert.assertEquals("There should be the same number fo not yet in success requests than before",
                            properties.getMaxBulkSize() - nbCompleted,
                            notYetCompleted.size());
        for (NotificationRequest notYet : notYetCompleted) {
            Assert.assertEquals("requests not yet completed should still be in state " + NotificationState.SCHEDULED,
                                NotificationState.SCHEDULED,
                                notYet.getState());
            Assert.assertEquals("There should still be 1 recipient scheduled for requests not yet completed",
                                1,
                                notYet.getRecipientsScheduled().size());
            Assert.assertTrue("recipientR1_2 should still be scheduled for request not yet completed",
                              notYet.getRecipientsScheduled().contains(recipientR1_2));
            Assert.assertTrue("Request not yet completed should have no recipients in error",
                              notYet.getRecipientsInError().isEmpty());
            Assert.assertTrue("Request not yet completed should have no recipients to schedule",
                              notYet.getRecipientsToSchedule().isEmpty());
        }
    }

    @Test
    public void testMatchWhileScheduleConcurrent() throws Exception {
        // possible because we could be in state TO_SCHEDULE and pass in state GRANTED because of a retry
        // ( first match with rules that could not be matched (rule2) -> schedule recipients identified (recipientR1_1 & recipientR1_2)
        //                                                    -> retry and match                 )
        // Init two rules with multiple recipients
        Init2Rule3Recipient twoRules3Recipients = new Init2Rule3Recipient(false);
        PluginConfiguration recipientR1_1 = twoRules3Recipients.getRecipientR1_1();
        PluginConfiguration recipientR1_2 = twoRules3Recipients.getRecipientR1_2();
        PluginConfiguration recipientR2_1 = twoRules3Recipients.getRecipientR2_1();
        Rule rule2 = twoRules3Recipients.getRule2();
        //lets prepare some element that are being retried while matched
        // we consider that rule2 could not be matched initially while rule1 could be match
        JsonObject elementBothRules = initElement("elementBothRule.json");
        JsonElement metadata = gson.toJsonTree(globalMetadata);
        List<NotificationRequest> beingScheduled = new ArrayList<>();
        for (int i = 0; i < properties.getMaxBulkSize() / 2; i++) {
            NotificationRequest request = new NotificationRequest(elementBothRules,
                                                                  metadata.getAsJsonObject(),
                                                                  AbstractRequestEvent.generateRequestId(),
                                                                  REQUEST_OWNER,
                                                                  OffsetDateTime.now(),
                                                                  NotificationState.TO_SCHEDULE_BY_RECIPIENT,
                                                                  Sets.newHashSet(rule2));
            request.getRecipientsToSchedule().add(recipientR1_1);
            request.getRecipientsToSchedule().add(recipientR1_2);
            beingScheduled.add(request);
        }
        beingScheduled = notificationRequestRepository.saveAll(beingScheduled);
        //prepare mockito to control concurrency
        CountDownLatch latch = new CountDownLatch(1);
        // we want to call match while we scheduleNotificationJobs is calling the scheduleJobForOneRecipient for recipientR1_1.
        // in reality we want any recipient but recipientR2_1 that has no requests to send because rule2 could not be matched
        List<NotificationRequest> finalBeingScheduled = beingScheduled;
        Mockito.when(notificationProcessingService.scheduleJobForOneRecipientConcurrent(Mockito.eq(recipientR1_1), Mockito.anyList()))
                .thenAnswer(invocation -> {
                    // simulate actions that allow to have a match: retry because of rule2 that is still to be matched
                    logger.debug("Start simulate");
                    // we cannot update based on what we had initialized because scheduling might have been done for
                    // another request so we have to ask DB what is the current state
                    testService.updateDatabaseToSimulateRetryOnlyRulesToMatch(
                            notificationRequestRepository.findAllById(finalBeingScheduled.stream().map(NotificationRequest::getId).collect(Collectors.toSet())));
                    logger.debug("End simulate");
                    CompletableFuture.runAsync(() -> {
                        runtimeTenantResolver.forceTenant(getDefaultTenant());
                        notificationMatchingService.matchRequestNRecipient();
                        latch.countDown();
                    });
                    return invocation.callRealMethod();
                })
                .thenAnswer((InvocationOnMock::callRealMethod));


        // jobInfoService was a good candidate to wait for matchRequestNRecipient to be finished so we used it
        Mockito.doAnswer(invocation -> {
                    if (((JobInfo) invocation.getArgument(0)).getParametersAsMap().get(NotificationJob.RECIPIENT_BUSINESS_ID).getValue().equals(recipientR1_1.getBusinessId())) {
                        Assert.assertTrue(
                                "Latch could not be released in less then 1 minutes! matchRequestNRecipient was too long.",
                                latch.await(3, TimeUnit.MINUTES));
                    }
                    return invocation.callRealMethod();
                })
                .when(jobInfoService).createAsQueued(Mockito.any());

        recipientService.scheduleNotificationJobs();
        // As we are simulating actions so that rule2 is matched while recipientR1_1 is being scheduled, requests might be in state SCHEDULED or TO_SCHEDULE
        // So that means: if we schedule recipientR2_1 before recipientR1_1, the request cannot be set in state SCHEDULED. Otherwise, recipientR2_1 would never be processed.
        // if we schedule recipientR2_1 after recipientR1_1, the request should be in state SCHEDULED. Because when we will request DB for request containing recipientR2_1, we
        // will see them
        List<NotificationRequest> scheduledNMatchedRequests = notificationRequestRepository
                .findAllById(beingScheduled.stream().map(NotificationRequest::getId).collect(Collectors.toSet()));
        for (NotificationRequest scheduledNMatched : scheduledNMatchedRequests) {
            Assert.assertTrue("Request scheduled and matched at the same time should not have any rules to match left",
                    scheduledNMatched.getRulesToMatch().isEmpty());
            if (scheduledNMatched.getState() == NotificationState.TO_SCHEDULE_BY_RECIPIENT) {
                Assert.assertEquals("Request scheduled and matched at the same time in state "
                                + NotificationState.TO_SCHEDULE_BY_RECIPIENT
                                + " should have only one recipient to schedule",
                        1,
                        scheduledNMatched.getRecipientsToSchedule().size());
                Assert.assertTrue("Request scheduled and matched at the same time in state "
                                + NotificationState.TO_SCHEDULE_BY_RECIPIENT
                                + " should have recipientR2_1 to schedule",
                        scheduledNMatched.getRecipientsToSchedule().contains(recipientR2_1));

                Assert.assertEquals("Request scheduled and matched at the same time in state "
                                            + NotificationState.TO_SCHEDULE_BY_RECIPIENT
                                            + " should have 2 recipients scheduled",
                                    2,
                                    scheduledNMatched.getRecipientsScheduled().size());
                Assert.assertTrue("Request scheduled and matched at the same time in state "
                                          + NotificationState.TO_SCHEDULE_BY_RECIPIENT
                                          + " should have recipientR1_1 scheduled",
                                  scheduledNMatched.getRecipientsScheduled().contains(recipientR1_1));
                Assert.assertTrue("Request scheduled and matched at the same time in state "
                                          + NotificationState.TO_SCHEDULE_BY_RECIPIENT
                                          + " should have recipientR1_2 scheduled",
                                  scheduledNMatched.getRecipientsScheduled().contains(recipientR1_2));
            } else if (scheduledNMatched.getState() == NotificationState.SCHEDULED) {
                Assert.assertEquals(
                        "Request scheduled and matched at the same time in state " + NotificationState.SCHEDULED
                                + " should have no more recipient to schedule",
                        0,
                        scheduledNMatched.getRecipientsToSchedule().size());
                Assert.assertEquals(
                        "Request scheduled and matched at the same time in state " + NotificationState.SCHEDULED
                                + " should have 3 recipients scheduled",
                        3,
                        scheduledNMatched.getRecipientsScheduled().size());
                Assert.assertTrue(
                        "Request scheduled and matched at the same time in state " + NotificationState.SCHEDULED
                                + " should have recipientR1_1 scheduled",
                        scheduledNMatched.getRecipientsScheduled().contains(recipientR1_1));
                Assert.assertTrue(
                        "Request scheduled and matched at the same time in state " + NotificationState.SCHEDULED
                                + " should have recipientR1_2 scheduled",
                        scheduledNMatched.getRecipientsScheduled().contains(recipientR1_2));
                Assert.assertTrue(
                        "Request scheduled and matched at the same time in state " + NotificationState.SCHEDULED
                                + " should have recipientR2_1 scheduled",
                        scheduledNMatched.getRecipientsScheduled().contains(recipientR2_1));
            } else {
                Assert.fail("Either concurrency test is not complete or the code is bugged, but a breakpoint to know!");
            }
            Assert.assertTrue("Request scheduled and matched at the same time should not have any recipient in error",
                              scheduledNMatched.getRecipientsInError().isEmpty());
        }
    }

    @Test
    public void testProcessFailWhileMatchConcurrent() throws Exception {
        // possible because previous recipient is failing while matching process has started, again, because of a retry
        // in case of rule that could not be matched has already been done
        Init2Rule3Recipient twoRules3Recipients = new Init2Rule3Recipient(true);
        PluginConfiguration recipientR1_1 = twoRules3Recipients.getRecipientR1_1();
        PluginConfiguration recipientR1_2 = twoRules3Recipients.getRecipientR1_2();
        PluginConfiguration recipientR2_1 = twoRules3Recipients.getRecipientR2_1();
        Rule rule2 = twoRules3Recipients.getRule2();
        //lets prepare some element that are being retried while matched
        // we consider that rule2 could not be matched initially while rule1 could be match
        JsonObject elementBothRules = initElement("elementBothRule.json");
        JsonElement metadata = gson.toJsonTree(globalMetadata);
        List<NotificationRequest> beingMatchForRuleError = new ArrayList<>();
        for (int i = 0; i < properties.getMaxBulkSize() / 2; i++) {
            NotificationRequest request = new NotificationRequest(elementBothRules,
                                                                  metadata.getAsJsonObject(),
                                                                  AbstractRequestEvent.generateRequestId(),
                                                                  REQUEST_OWNER,
                                                                  OffsetDateTime.now(),
                                                                  // was in state SCHEDULED because job have been scheduled but retry has set requests in state GRANTED
                                                                  NotificationState.GRANTED,
                                                                  Sets.newHashSet(rule2));
            request.getRecipientsScheduled().add(recipientR1_1);
            request.getRecipientsScheduled().add(recipientR1_2);
            beingMatchForRuleError.add(request);
        }
        beingMatchForRuleError = notificationRequestRepository.saveAll(beingMatchForRuleError);
        // we want to be sure that processRequest is being started while we are already inside
        // matchRequestNRecipientConcurrent so we add some logic before the real method is called
        // moreover, we want to be sure that processRequest has ended before
        // the end of the first matchRequestNRecipientConcurrent call so optimistic lock will fail and we can properly check what happens
        List<NotificationRequest> finalBeingMatched = beingMatchForRuleError;
        CountDownLatch latch = new CountDownLatch(1);
        Mockito.doAnswer(invocation -> {
                    CompletableFuture.runAsync(() -> {
                        runtimeTenantResolver.forceTenant(getDefaultTenant());
                        notificationProcessingService.processRequests(finalBeingMatched, recipientR1_1);
                        latch.countDown();
                    });
                    return invocation.callRealMethod();
                }).doAnswer((InvocationOnMock::callRealMethod)).when(notificationMatchingService)
                .matchRequestNRecipientConcurrent(Mockito.anyList());
        // pluginService was a good candidate to wait for processRequest to be finished so we used it
        Mockito.doAnswer(invocation -> {
                    Assert.assertTrue(
                            "Latch could not be released in less then 1 minutes! registerNotificationRequests was too long.",
                            latch.await(1, TimeUnit.MINUTES));
                    return invocation.callRealMethod();
                }).doAnswer(InvocationOnMock::callRealMethod).when(pluginService)
                .getPlugin(rule2.getRulePlugin().getBusinessId());
        notificationMatchingService.matchRequestNRecipient();
        // Requests should end up in state TO_SCHEDULE and not ERROR so the recipientR2_1(that has been match during this call to match) could be scheduled
        // Moreover, recipientR1_1 has failed and so an event has been send to API caller so they can handle it(retry most likely) and recipientR1_1 should be kept has
        // recipientInError
        List<NotificationRequest> processFailedWhileMatchRequests = notificationRequestRepository
                .findAllById(beingMatchForRuleError.stream().map(NotificationRequest::getId)
                        .collect(Collectors.toList()));
        Assert.assertTrue(
                "Requests which one recipient has failed while they were being matched because of a rule that could not be matched earlier should all be in state "
                        + NotificationState.TO_SCHEDULE_BY_RECIPIENT + " and not " + processFailedWhileMatchRequests
                        .get(0).getState(),
                processFailedWhileMatchRequests.stream()
                        .allMatch(request -> request.getState() == NotificationState.TO_SCHEDULE_BY_RECIPIENT));
        for (NotificationRequest processFailedWhileMatch : processFailedWhileMatchRequests) {
            // no more rules to match
            Assert.assertTrue(
                    "Requests which one recipient has failed while they were being matched because of a rule that could not be matched earlier should no longer have any rules to match",
                    processFailedWhileMatch.getRulesToMatch().isEmpty());
            // recipientR2_1 is to schedule
            Assert.assertEquals(
                    "Requests which one recipient has failed while they were being matched because of a rule that could not be matched earlier should have 1 recipient to schedule",
                    1,
                    processFailedWhileMatch.getRecipientsToSchedule().size());
            Assert.assertTrue(
                    "Requests which one recipient has failed while they were being matched because of a rule that could not be matched earlier should have recipientR2_1 to schedule",
                    processFailedWhileMatch.getRecipientsToSchedule().contains(recipientR2_1));
            // recipientR1_1 is in error
            Assert.assertEquals(
                    "Requests which one recipient has failed while they were being matched because of a rule that could not be matched earlier should have 1 recipient in error",
                    1,
                    processFailedWhileMatch.getRecipientsInError().size());
            Assert.assertTrue(
                    "Requests which one recipient has failed while they were being matched because of a rule that could not be matched earlier should have recipientR1_1 in error",
                    processFailedWhileMatch.getRecipientsInError().contains(recipientR1_1));
            // recipientR1_2 is still scheduled
            Assert.assertEquals(
                    "Requests which one recipient has failed while they were being matched because of a rule that could not be matched earlier should still have 1 recipient scheduled",
                    1,
                    processFailedWhileMatch.getRecipientsScheduled().size());
            Assert.assertTrue(
                    "Requests which one recipient has failed while they were being matched because of a rule that could not be matched earlier should still have recipientR1_2 scheduled",
                    processFailedWhileMatch.getRecipientsScheduled().contains(recipientR1_2));
        }
    }

    @Test
    public void testMatchWhileProcessFailConcurrent() throws Exception {
        // we cannot directly spy on jpa repository so we need to do this by hand: (don't ask me why but it cannot be done later...)
        INotificationRequestRepository spiedRepo = Mockito.mock(INotificationRequestRepository.class,
                MockReset.withSettings(MockReset.AFTER)
                        .defaultAnswer(AdditionalAnswers.delegatesTo(
                                notificationRequestRepository)));
        // now we need to inject it into the service thanks to ReflectionTestUtils
        ReflectionTestUtils.setField(notificationRegistrationService, "notificationRequestRepository", spiedRepo);
        ReflectionTestUtils.setField(notificationMatchingService, "notificationRequestRepository", spiedRepo);
        ReflectionTestUtils.setField(notificationProcessingService, "notificationRequestRepository", spiedRepo);
        // possible because of retry in case of rule that could not be matched and previous recipient is failing
        Init2Rule3Recipient twoRules3Recipients = new Init2Rule3Recipient(true);
        PluginConfiguration recipientR1_1 = twoRules3Recipients.getRecipientR1_1();
        PluginConfiguration recipientR1_2 = twoRules3Recipients.getRecipientR1_2();
        PluginConfiguration recipientR2_1 = twoRules3Recipients.getRecipientR2_1();
        Rule rule2 = twoRules3Recipients.getRule2();
        // we consider that rule2 could not be matched initially while rule1 could be match
        JsonObject elementBothRules = initElement("elementBothRule.json");
        JsonElement metadata = gson.toJsonTree(globalMetadata);
        List<NotificationRequest> beingProcessed = new ArrayList<>();
        for (int i = 0; i < properties.getMaxBulkSize() / 2; i++) {
            NotificationRequest request = new NotificationRequest(elementBothRules,
                    metadata.getAsJsonObject(),
                    AbstractRequestEvent.generateRequestId(),
                    REQUEST_OWNER,
                    OffsetDateTime.now(),
                    // was in state SCHEDULED because job have been scheduled but retry has set requests in state GRANTED
                    NotificationState.GRANTED,
                    Sets.newHashSet(rule2));
            request.getRecipientsScheduled().add(recipientR1_1);
            request.getRecipientsScheduled().add(recipientR1_2);
            beingProcessed.add(request);
        }
        beingProcessed = notificationRequestRepository.saveAll(beingProcessed);
        CountDownLatch latch = new CountDownLatch(1);
        //small trick so we are sure the same of the method is really the one needed and we don't care about canonical name & co
        final List<String> finalHandleRecipientResultsMethodName = new ArrayList<>(1);
        Mockito.doAnswer(invocation -> {
                    finalHandleRecipientResultsMethodName.add(invocation.getMethod().getName());
                    CompletableFuture.runAsync(() -> {
                        runtimeTenantResolver.forceTenant(getDefaultTenant());
                        notificationMatchingService.matchRequestNRecipient();
                        latch.countDown();
                    });
                    return invocation.callRealMethod();
                })
                .doAnswer((InvocationOnMock::callRealMethod))
                .when(notificationProcessingService).handleRecipientResultsConcurrent(Mockito.anyList(), Mockito.any(), Mockito.anyCollection());

        // There is no good candidate to wait for matchRequestNRecipient in handleRecipientResultsConcurrent to end its execution so we can only use notificationRepo.saveAll
        // as we cannot call abstract method with invocation::callRealMethod, we bypass the spy by calling the real method from the real bean
        // !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!           WARNING           !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
        // This only works here because we are not relying on verification feature of the spy and only want to add some logic before calling the real method
        Mockito.doAnswer(invocation -> {
            // lets check just the caller is handleRecipientResultsConcurrent so as not to block anything else
            try {
                throw new Exception();
            } catch (Exception e) {
                // lets see if saveAll was called by handleRecipientResultsConcurrent i.e mock has been called so list has 1 element
                if (finalHandleRecipientResultsMethodName.size() == 1) {
                    if (Arrays.stream(e.getStackTrace()).anyMatch(ste -> ste.getMethodName()
                            .equals(finalHandleRecipientResultsMethodName.get(0)))) {
                        Assert.assertTrue(
                                "Latch could not be released in less then 1 minutes! matchRequestNRecipient was too long.",
                                latch.await(5, TimeUnit.MINUTES));
                    }
                }
            }
            return notificationRequestRepository.saveAll(invocation.getArgument(0));
        }).when(spiedRepo).saveAll(Mockito.anyCollection());
        notificationProcessingService.processRequests(beingProcessed, recipientR1_1);
        // Requests should be in state TO_SCHEDULE so that recipientR2_1 could be scheduled ASAP. But error event should
        // be sent anyway so that it can be handled by API callers
        List<NotificationRequest> matchWhileProcessFailedRequests = notificationRequestRepository
                .findAllById(beingProcessed.stream().map(NotificationRequest::getId).collect(Collectors.toList()));
        Assert.assertTrue(
                "Requests that are matched because of a rule that should not be matched earlier while one recipient fails should all be in state "
                        + NotificationState.TO_SCHEDULE_BY_RECIPIENT + " and not " + matchWhileProcessFailedRequests
                        .get(0).getState(),
                matchWhileProcessFailedRequests.stream()
                        .allMatch(request -> request.getState() == NotificationState.TO_SCHEDULE_BY_RECIPIENT));
        for (NotificationRequest matchWhileProcessFailed : matchWhileProcessFailedRequests) {
            // no more rules to match
            Assert.assertTrue(
                    "Requests that are matched because of a rule that should not be matched earlier while one recipient fails should no longer have any rules to match",
                    matchWhileProcessFailed.getRulesToMatch().isEmpty());
            // recipientR2_1 is to schedule
            Assert.assertEquals(
                    "Requests that are matched because of a rule that should not be matched earlier while one recipient fails should have 1 recipient to schedule",
                    1,
                    matchWhileProcessFailed.getRecipientsToSchedule().size());
            Assert.assertTrue(
                    "Requests that are matched because of a rule that should not be matched earlier while one recipient fails should have recipientR2_1 to schedule",
                    matchWhileProcessFailed.getRecipientsToSchedule().contains(recipientR2_1));
            // recipientR1_1 is in error
            Assert.assertEquals(
                    "Requests that are matched because of a rule that should not be matched earlier while one recipient fails should have 1 recipient in error",
                    1,
                    matchWhileProcessFailed.getRecipientsInError().size());
            Assert.assertTrue(
                    "Requests that are matched because of a rule that should not be matched earlier while one recipient fails should have recipientR1_1 in error",
                    matchWhileProcessFailed.getRecipientsInError().contains(recipientR1_1));
            // recipientR1_2 is still scheduled
            Assert.assertEquals(
                    "Requests that are matched because of a rule that should not be matched earlier while one recipient fails should still have 1 recipient scheduled",
                    1,
                    matchWhileProcessFailed.getRecipientsScheduled().size());
            Assert.assertTrue(
                    "Requests that are matched because of a rule that should not be matched earlier while one recipient fails should still have recipientR1_2 scheduled",
                    matchWhileProcessFailed.getRecipientsScheduled().contains(recipientR1_2));
        }
    }

    @Test
    public void testRetryWhileMatchConcurrent() throws Exception {
        // possible because of a retry just before a recipient fails that is now entering matching process (not because of a recipient in error but a rule that could not be matched)
        // and a second retry because the recipient has failed (not because of rules to match but because of recipient in error)
        // In fact, matching process being executed, it means we launch matching process on an entity with some rulesToMatch and no recipient in error
        // while the process is failing on a request with the following content: rulesToMatch not empty and recipientsError not empty and state error
        // Init two rules with multiple recipients
        Init2Rule3Recipient twoRules3Recipients = new Init2Rule3Recipient(false);
        PluginConfiguration recipientR1_1 = twoRules3Recipients.getRecipientR1_1();
        Rule rule1 = twoRules3Recipients.getRule1();
        Rule rule2 = twoRules3Recipients.getRule2();
        //lets prepare some element that are being retried while matched
        JsonObject elementR1 = initElement("elementRule1.json");
        JsonElement metadata = gson.toJsonTree(globalMetadata);
        List<NotificationRequest> beingMatched = new ArrayList<>();
        List<NotificationRequestEvent> retryEvents = new ArrayList<>();
        for (int i = 0; i < properties.getMaxBulkSize() / 2; i++) {
            NotificationRequest request = new NotificationRequest(elementR1,
                                                                  metadata.getAsJsonObject(),
                                                                  AbstractRequestEvent.generateRequestId(),
                                                                  REQUEST_OWNER,
                                                                  OffsetDateTime.now(),
                                                                  NotificationState.GRANTED,
                                                                  Sets.newHashSet(rule2));
            // we consider that recipientR1_1 has not yet been processed and will fail
            request.getRecipientsScheduled().add(recipientR1_1);
            retryEvents.add(new NotificationRequestEvent(request.getPayload(),
                                                         request.getMetadata(),
                                                         request.getRequestId(),
                                                         request.getRequestOwner()));
            beingMatched.add(request);
        }
        beingMatched = notificationRequestRepository.saveAll(beingMatched);
        JsonObject elementR2 = initElement("elementRule2.json");
        //lets prepare some element that are completely new and are not being matched
        List<NotificationRequestEvent> newEvents = new ArrayList<>();
        for (int i = 0; i < properties.getMaxBulkSize() - properties.getMaxBulkSize() / 2; i++) {
            newEvents.add(new NotificationRequestEvent(elementR2,
                                                       gson.toJsonTree(
                                                               globalMetadata).getAsJsonObject(),
                                                       AbstractRequestEvent.generateRequestId(),
                                                       REQUEST_OWNER));
        }
        ArrayList<NotificationRequestEvent> eventToRegister = new ArrayList<>();
        eventToRegister.addAll(newEvents);
        eventToRegister.addAll(retryEvents);

        // we want to be sure that registerNotificationRequest is being started while we are already inside
        // matchRequestNRecipientConcurrent so we add some logic before the real method is called
        // moreover, we want to be sure that registerNotificationRequest (that contains retry logic) has ended before
        // the end of the first matchRequestNRecipientConcurrent call so optimistic lock will fail and we can properly check what happens
        List<NotificationRequest> finalBeingMatched = beingMatched;
        CountDownLatch latch = new CountDownLatch(1);
        Mockito.doAnswer(invocation -> {
                    //simulate actions that allow to have a retry
                    testService.updateDatabaseToSimulateProcessFailForRecipient(finalBeingMatched, recipientR1_1);
                    CompletableFuture.runAsync(() -> {
                        runtimeTenantResolver.forceTenant(getDefaultTenant());
                        notificationRegistrationService.registerNotificationRequests(eventToRegister);
                        latch.countDown();
                    });
                    return invocation.callRealMethod();
                }).doAnswer((InvocationOnMock::callRealMethod)).when(notificationMatchingService)
                .matchRequestNRecipientConcurrent(Mockito.anyList());
        // pluginService was a good candidate to wait for registerNotificationRequests to be finished so we used it
        Mockito.doAnswer(invocation -> {
                    Assert.assertTrue(
                            "Latch could not be released in less then 1 minutes! registerNotificationRequests was too long.",
                            latch.await(1, TimeUnit.MINUTES));
                    return invocation.callRealMethod();
                }).doAnswer(InvocationOnMock::callRealMethod).when(pluginService)
                .getPlugin(rule2.getRulePlugin().getBusinessId());
        notificationMatchingService.matchRequestNRecipient();
        // final result should be:
        //  - Concerning requests that were being matched and retried: no more rules to match, no more recipient in error,
        //     no more recipient scheduled, only recipientR1_1 to be scheduled (payload matched only rule1 and not rule2
        //     so matching should not have added recipientR2_1) and requests should be in state TO_SCHEDULE_BY_RECIPIENT
        //  - Concerning new events: request should be in state GRANTED with rule1 and rule2 to be matched
        List<NotificationRequest> matchedNRetriedRequests = notificationRequestRepository
                .findAllById(beingMatched.stream().map(NotificationRequest::getId).collect(Collectors.toSet()));
        Assert.assertTrue("All requests that were being matched and retried should be in state "
                                  + NotificationState.TO_SCHEDULE_BY_RECIPIENT,
                          matchedNRetriedRequests.stream().allMatch(request -> request.getState()
                                  == NotificationState.TO_SCHEDULE_BY_RECIPIENT));
        for (NotificationRequest matchedNRetried : matchedNRetriedRequests) {
            Assert.assertTrue(
                    "Request that have been matched and retried at the same time should not have rulesToMatch anymore",
                    matchedNRetried.getRulesToMatch().isEmpty());
            Assert.assertTrue(
                    "Request that have been matched and retried at the same time should not have recipientsInError anymore",
                    matchedNRetried.getRecipientsInError().isEmpty());
            Assert.assertTrue(
                    "Request that have been matched and retried at the same time should not have recipientsScheduled anymore",
                    matchedNRetried.getRecipientsScheduled().isEmpty());
            Assert.assertEquals(
                    "Request that have been matched and retried at the same time should have only one recipientsToSchedule",
                    1,
                    matchedNRetried.getRecipientsToSchedule().size());
            Assert.assertTrue(
                    "Request that have been matched and retried at the same time should have only recipientR1_1 to schedule",
                    matchedNRetried.getRecipientsToSchedule().contains(recipientR1_1));
        }
        Set<NotificationRequest> newRequests = notificationRequestRepository
                .findAllByRequestIdIn(newEvents.stream().map(NotificationRequestEvent::getRequestId)
                        .collect(Collectors.toSet()));
        Assert.assertEquals("Not all new requests could be created properly",
                            properties.getMaxBulkSize() - properties.getMaxBulkSize() / 2,
                            newRequests.size());
        Assert.assertTrue("All new notification requests should be in state " + NotificationState.GRANTED,
                          newRequests.stream().allMatch(r -> r.getState() == NotificationState.GRANTED));
        for (NotificationRequest newRequest : newRequests) {
            Assert.assertEquals("New request should have 2 rules to match", 2, newRequest.getRulesToMatch().size());
            Assert.assertTrue("New request should have rule1 to match", newRequest.getRulesToMatch().contains(rule1));
            Assert.assertTrue("New request should have rule2 to match", newRequest.getRulesToMatch().contains(rule2));
            Assert.assertTrue("There should be no recipient to schedule yet among new requests",
                              newRequest.getRecipientsToSchedule().isEmpty());
            Assert.assertTrue("There should be no recipient in error among new requests",
                              newRequest.getRecipientsInError().isEmpty());
            Assert.assertTrue("There should be no recipient already scheduled among new requests",
                              newRequest.getRecipientsScheduled().isEmpty());
        }
    }

    @Test
    public void testRetryWhileScheduleConcurrent() throws Exception {
        // possible because of rules that could not be matched
        // test that everything is well when scheduling schedule and event are received at the same time
        // Init two rules with multiple recipients
        Init2Rule3Recipient twoRules3Recipients = new Init2Rule3Recipient(false);
        PluginConfiguration recipientR1_1 = twoRules3Recipients.getRecipientR1_1();
        PluginConfiguration recipientR1_2 = twoRules3Recipients.getRecipientR1_2();
        Rule rule2 = twoRules3Recipients.getRule2();
        // lets init some requests that have been matched and will be scheduled
        // we consider that rule2 could not be matched initially while rule1 could be match
        JsonObject elementBothRules = initElement("elementBothRule.json");
        JsonElement metadata = gson.toJsonTree(globalMetadata);
        List<NotificationRequest> beingScheduled = new ArrayList<>();
        List<NotificationRequestEvent> beingRetriedEvents = new ArrayList<>();
        for (int i = 0; i < 10 / 2; i++) {
            NotificationRequest request = new NotificationRequest(elementBothRules,
                                                                  metadata.getAsJsonObject(),
                                                                  AbstractRequestEvent.generateRequestId(),
                                                                  REQUEST_OWNER,
                                                                  OffsetDateTime.now(),
                                                                  NotificationState.TO_SCHEDULE_BY_RECIPIENT,
                                                                  Sets.newHashSet(rule2));
            request.getRecipientsToSchedule().add(recipientR1_1);
            request.getRecipientsToSchedule().add(recipientR1_2);
            beingScheduled.add(request);
            beingRetriedEvents.add(new NotificationRequestEvent(request.getPayload(),
                                                                request.getMetadata(),
                                                                request.getRequestId(),
                                                                request.getRequestOwner()));
        }
        beingScheduled = notificationRequestRepository.saveAll(beingScheduled);
        //prepare mockito to control concurrency
        CountDownLatch latch = new CountDownLatch(1);
        // we want to call registerNotificationRequests while scheduleNotificationJobs is calling the scheduleJobForOneRecipient for recipientR1_1.
        // in reality we want any recipient but recipientR2_1 that has no requests to send because rule2 could not be matched
        Mockito.when(notificationProcessingService
                             .scheduleJobForOneRecipientConcurrent(Mockito.eq(recipientR1_1), Mockito.anyList()))
                .thenAnswer(invocation -> {
                    // There is no action to simulate
                    CompletableFuture.runAsync(() -> {
                        runtimeTenantResolver.forceTenant(getDefaultTenant());
                        notificationRegistrationService.registerNotificationRequests(beingRetriedEvents);
                        latch.countDown();
                    });
                    return invocation.callRealMethod();
                }).thenAnswer((InvocationOnMock::callRealMethod));
        // jobInfoService was a good candidate to wait for registerNotificationRequests to be finished so we used it
        Mockito.doAnswer(invocation -> {
            if (((JobInfo) invocation.getArgument(0)).getParametersAsMap().get(NotificationJob.RECIPIENT_BUSINESS_ID)
                    .getValue().equals(recipientR1_1.getBusinessId())) {
                Assert.assertTrue(
                        "Latch could not be released in less then 1 minutes! matchRequestNRecipient was too long.",
                        latch.await(3, TimeUnit.MINUTES));
            }
            return invocation.callRealMethod();
        }).when(jobInfoService).createAsQueued(Mockito.any());
        recipientService.scheduleNotificationJobs();
        // As we are simulating actions so that requests are being retried while recipientR1_1 is being scheduled, requests should be in state GRANTED(retry on rule to match)
        List<NotificationRequest> scheduledNRetriedRequests = notificationRequestRepository
                .findAllById(beingScheduled.stream().map(NotificationRequest::getId).collect(Collectors.toSet()));
        Assert.assertTrue(
                "Requests scheduled and retried at the same time should be in state " + NotificationState.GRANTED,
                scheduledNRetriedRequests.stream().allMatch(r -> r.getState() == NotificationState.GRANTED));
        for (NotificationRequest scheduledNRetried : scheduledNRetriedRequests) {
            Assert.assertEquals("Request scheduled and retried at the same time should have 1 rule to match left",
                                1,
                                scheduledNRetried.getRulesToMatch().size());
            Assert.assertTrue("Request scheduled and retried at the same time should have rule2 to match left",
                              scheduledNRetried.getRulesToMatch().contains(rule2));
            // we might have not executed scheduleJobForOneRecipientConcurrent for recipientR1_2 before executing it for recipientR1_1 for these request
            // because there state has been updated to GRANTED during scheduleJobForOneRecipientConcurrent for recipientR1_1
            if (scheduledNRetried.getRecipientsToSchedule().size() == 1) {
                Assert.assertTrue("Request scheduled and retried at the same time should have no recipients to schedule",
                                  scheduledNRetried.getRecipientsToSchedule().contains(recipientR1_2));
                Assert.assertEquals("Request scheduled and retried at the same time should have 1 recipients scheduled",
                                    1,
                                    scheduledNRetried.getRecipientsScheduled().size());
                Assert.assertTrue(
                        "Request scheduled and retried at the same time in state should have recipientR1_1 scheduled",
                        scheduledNRetried.getRecipientsScheduled().contains(recipientR1_1));
            } else {
                Assert.assertTrue("Request scheduled and retried at the same time should have no recipients to schedule",
                                  scheduledNRetried.getRecipientsToSchedule().isEmpty());
                Assert.assertEquals("Request scheduled and retried at the same time should have 2 recipients scheduled",
                                    2,
                                    scheduledNRetried.getRecipientsScheduled().size());
                Assert.assertTrue(
                        "Request scheduled and retried at the same time in state should have recipientR1_1 scheduled",
                        scheduledNRetried.getRecipientsScheduled().contains(recipientR1_1));
                Assert.assertTrue(
                        "Request scheduled and retried at the same time in state should have recipientR1_2 scheduled",
                        scheduledNRetried.getRecipientsScheduled().contains(recipientR1_2));
            }
            Assert.assertTrue("Request scheduled and retried at the same time should not have any recipient in error",
                              scheduledNRetried.getRecipientsInError().isEmpty());
        }
    }

    @Test
    public void testScheduleWhileRetryConcurrent() throws Exception {
        // we cannot directly spy on jpa repository so we need to do this by hand: (don't ask me why but it cannot be done later...)
        INotificationRequestRepository spiedRepo = Mockito.mock(INotificationRequestRepository.class,
                MockReset.withSettings(MockReset.AFTER)
                        .defaultAnswer(AdditionalAnswers.delegatesTo(
                                notificationRequestRepository)));
        // now we need to inject it into the service thanks to ReflectionTestUtils
        ReflectionTestUtils.setField(notificationRegistrationService, "notificationRequestRepository", spiedRepo);
        ReflectionTestUtils.setField(notificationMatchingService, "notificationRequestRepository", spiedRepo);
        ReflectionTestUtils.setField(notificationProcessingService, "notificationRequestRepository", spiedRepo);
        // possible because of rules that could not be matched
        // Init two rules with multiple recipients
        Init2Rule3Recipient twoRules3Recipients = new Init2Rule3Recipient(false);
        PluginConfiguration recipientR1_1 = twoRules3Recipients.getRecipientR1_1();
        PluginConfiguration recipientR1_2 = twoRules3Recipients.getRecipientR1_2();
        Rule rule2 = twoRules3Recipients.getRule2();
        // lets init some requests that have been matched and will be scheduled
        // we consider that rule2 could not be matched initially while rule1 could be match
        JsonObject elementBothRules = initElement("elementBothRule.json");
        JsonElement metadata = gson.toJsonTree(globalMetadata);
        List<NotificationRequest> beingRetried = new ArrayList<>();
        List<NotificationRequestEvent> beingRetriedEvents = new ArrayList<>();
        for (int i = 0; i < properties.getMaxBulkSize() / 2; i++) {
            NotificationRequest request = new NotificationRequest(elementBothRules,
                                                                  metadata.getAsJsonObject(),
                                                                  AbstractRequestEvent.generateRequestId(),
                                                                  REQUEST_OWNER,
                                                                  OffsetDateTime.now(),
                                                                  NotificationState.TO_SCHEDULE_BY_RECIPIENT,
                                                                  Sets.newHashSet(rule2));
            request.getRecipientsToSchedule().add(recipientR1_1);
            request.getRecipientsToSchedule().add(recipientR1_2);
            beingRetried.add(request);
            beingRetriedEvents.add(new NotificationRequestEvent(request.getPayload(),
                                                                request.getMetadata(),
                                                                request.getRequestId(),
                                                                request.getRequestOwner()));
        }
        beingRetried = notificationRequestRepository.saveAll(beingRetried);
        //prepare mockito to control concurrency
        CountDownLatch latch = new CountDownLatch(1);
        // we want to call scheduleNotificationJobs while registerNotificationRequests is calling the scheduleJobForOneRecipient for recipientR1_1.
        // in reality we want any recipient but recipientR2_1 that has no requests to send because rule2 could not be matched
        final List<String> finalHandleRetryMethodName = new ArrayList<>(1);
        Mockito.doAnswer(invocation -> {
            finalHandleRetryMethodName.add(invocation.getMethod().getName());
            // There is no action to simulate
            CompletableFuture.runAsync(() -> {
                runtimeTenantResolver.forceTenant(getDefaultTenant());
                recipientService.scheduleNotificationJobs();
                latch.countDown();
            });
            return invocation.callRealMethod();
        }).doAnswer((InvocationOnMock::callRealMethod)).when(notificationRegistrationService)
                .handleRetryRequestsConcurrent(beingRetriedEvents);
        // There is no good candidate to wait for scheduleNotificationJobs, in handleRetryRequestsConcurrent, to end its execution so we can only use notificationRepo.saveAll
        // as we cannot call abstract method with invocation::callRealMethod, we bypass the spy by calling the real method from the real bean
        // !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!           WARNING           !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
        // This only works here because we are not relying on verification feature of the spy and only want to add some logic before calling the real method
        Mockito.doAnswer(invocation -> {
            // lets check just the caller is handleRetryRequestsConcurrent so as not to block anything else
            try {
                throw new Exception();
            } catch (Exception e) {
                // lets see if saveAll was called by handleRetryRequestsConcurrent i.e mock has been called so list has 1 element
                if (finalHandleRetryMethodName.size() == 1) {
                    if (Arrays.stream(e.getStackTrace())
                            .anyMatch(ste -> ste.getMethodName().equals(finalHandleRetryMethodName.get(0)))) {
                        Assert.assertTrue(
                                "Latch could not be released in less then 1 minutes! scheduleNotificationJobs was too long.",
                                latch.await(5, TimeUnit.MINUTES));
                    }
                }
            }
            return notificationRequestRepository.saveAll(invocation.getArgument(0));
        }).when(spiedRepo).saveAll(Mockito.anyCollection());
        notificationRegistrationService.registerNotificationRequests(beingRetriedEvents);
        // As we are simulating actions so that requests are being retried while recipientR1_1 is being scheduled, requests should be in state GRANTED(retry on rule to match)
        // recipientR2_1 could not have been scheduled as it is marked as to schedule after the schedule method has ended
        List<NotificationRequest> scheduledNRetriedRequests = notificationRequestRepository
                .findAllById(beingRetried.stream().map(NotificationRequest::getId).collect(Collectors.toSet()));
        Assert.assertTrue(
                "Requests scheduled and retried at the same time should be in state " + NotificationState.GRANTED,
                scheduledNRetriedRequests.stream().allMatch(r -> r.getState() == NotificationState.GRANTED));
        for (NotificationRequest scheduledNRetried : scheduledNRetriedRequests) {
            Assert.assertEquals("Request scheduled and retried at the same time should have 1 rule to match left",
                                1,
                                scheduledNRetried.getRulesToMatch().size());
            Assert.assertTrue("Request scheduled and retried at the same time should have rule2 to match left",
                              scheduledNRetried.getRulesToMatch().contains(rule2));
            // we might have not executed scheduleJobForOneRecipientConcurrent for recipientR1_2 before executing it for recipientR1_1 for these request
            Assert.assertTrue("Request scheduled and retried at the same time should have no recipients to schedule",
                              scheduledNRetried.getRecipientsToSchedule().isEmpty());
            Assert.assertEquals("Request scheduled and retried at the same time should have 2 recipients scheduled",
                                2,
                                scheduledNRetried.getRecipientsScheduled().size());
            Assert.assertTrue(
                    "Request scheduled and retried at the same time in state should have recipientR1_1 scheduled",
                    scheduledNRetried.getRecipientsScheduled().contains(recipientR1_1));
            Assert.assertTrue(
                    "Request scheduled and retried at the same time in state should have recipientR1_2 scheduled",
                    scheduledNRetried.getRecipientsScheduled().contains(recipientR1_2));
            Assert.assertTrue("Request scheduled and retried at the same time should not have any recipient in error",
                              scheduledNRetried.getRecipientsInError().isEmpty());
        }
    }

    @Test
    public void testProcessFailWhileScheduleConcurrent() throws Exception {
        // possible because of rule that could not be matched that has been retried and then matched while first recipients have not yet been handled
        // Init two rules with multiple recipients
        Init2Rule3Recipient twoRules3Recipients = new Init2Rule3Recipient(true);
        PluginConfiguration recipientR1_1 = twoRules3Recipients.getRecipientR1_1();
        PluginConfiguration recipientR1_2 = twoRules3Recipients.getRecipientR1_2();
        PluginConfiguration recipientR2_1 = twoRules3Recipients.getRecipientR2_1();
        //lets prepare some element that are being retried while matched
        // we consider that rule2 could not be matched initially while rule1 could be match
        JsonObject elementBothRules = initElement("elementBothRule.json");
        JsonElement metadata = gson.toJsonTree(globalMetadata);
        List<NotificationRequest> beingScheduled = new ArrayList<>();
        for (int i = 0; i < properties.getMaxBulkSize() / 2; i++) {
            NotificationRequest request = new NotificationRequest(elementBothRules,
                                                                  metadata.getAsJsonObject(),
                                                                  AbstractRequestEvent.generateRequestId(),
                                                                  REQUEST_OWNER,
                                                                  OffsetDateTime.now(),
                                                                  NotificationState.TO_SCHEDULE_BY_RECIPIENT,
                                                                  new HashSet<>());
            //recipientR1_1 & recipientR1_2 has already been scheduled previously
            request.getRecipientsScheduled().add(recipientR1_1);
            request.getRecipientsScheduled().add(recipientR1_2);
            // recipientR1_2 has been matched after rule2 retry and match => it is to schedule
            request.getRecipientsToSchedule().add(recipientR2_1);
            beingScheduled.add(request);
        }
        beingScheduled = notificationRequestRepository.saveAll(beingScheduled);
        //prepare mockito to control concurrency
        CountDownLatch latch = new CountDownLatch(1);
        // we want to call process on recipientR1_1 while scheduleNotificationJobs is calling
        // scheduleJobForOneRecipient for recipientR2_1 (only recipient to schedule right now).
        List<NotificationRequest> finalBeingScheduled = beingScheduled;
        Mockito.when(notificationProcessingService
                             .scheduleJobForOneRecipientConcurrent(Mockito.eq(recipientR2_1), Mockito.anyList()))
                .thenAnswer(invocation -> {
                    CompletableFuture.runAsync(() -> {
                        runtimeTenantResolver.forceTenant(getDefaultTenant());
                        notificationProcessingService.processRequests(finalBeingScheduled, recipientR1_1);
                        latch.countDown();
                    });
                    return invocation.callRealMethod();
                }).thenAnswer((InvocationOnMock::callRealMethod));
        // jobInfoService was a good candidate to wait for processRequest to be finished so we used it
        Mockito.doAnswer(invocation -> {
            if (((JobInfo) invocation.getArgument(0)).getParametersAsMap().get(NotificationJob.RECIPIENT_BUSINESS_ID)
                    .getValue().equals(recipientR2_1.getBusinessId())) {
                Assert.assertTrue(
                        "Latch could not be released in less then 1 minutes! matchRequestNRecipient was too long.",
                        latch.await(3, TimeUnit.MINUTES));
            }
            return invocation.callRealMethod();
        }).when(jobInfoService).createAsQueued(Mockito.any());
        recipientService.scheduleNotificationJobs();
        // requests should be in state SCHEDULED, recipientR1_1 should be in error, event to API callers have been send to say that there was an error
        // recipientR1_2 is still scheduled, recipientR2_1 is scheduled, rulesToMatch is empty, no more recipients to schedule
        List<NotificationRequest> failedWhileScheduledRequests = notificationRequestRepository
                .findAllById(beingScheduled.stream().map(NotificationRequest::getId).collect(Collectors.toSet()));
        Assert.assertTrue("Requests for which one recipient ended up in error while scheduled should all be in state "
                                  + NotificationState.SCHEDULED + " and not " + failedWhileScheduledRequests.get(0)
                                  .getState(),
                          failedWhileScheduledRequests.stream()
                                  .allMatch(request -> request.getState() == NotificationState.SCHEDULED));
        for (NotificationRequest failedWhileScheduled : failedWhileScheduledRequests) {
            //no rules to match
            Assert.assertTrue(
                    "Requests for which one recipient ended up in error while scheduled should have no rules to match",
                    failedWhileScheduled.getRulesToMatch().isEmpty());
            //recipientR1_1 in error
            Assert.assertEquals(
                    "Requests for which one recipient ended up in error while scheduled should have only 1 recipient in error",
                    1,
                    failedWhileScheduled.getRecipientsInError().size());
            Assert.assertTrue(
                    "Requests for which one recipient ended up in error while scheduled should have recipientR1_1 in error",
                    failedWhileScheduled.getRecipientsInError().contains(recipientR1_1));
            //recipientR1_2 & recipientR2_1 scheduled
            Assert.assertEquals(
                    "Requests for which one recipient ended up in error while scheduled should have two recipient scheduled",
                    2,
                    failedWhileScheduled.getRecipientsScheduled().size());
            Assert.assertTrue(
                    "Requests for which one recipient ended up in error while scheduled should have recipientR1_2 scheduled (not yet processed)",
                    failedWhileScheduled.getRecipientsScheduled().contains(recipientR1_2));
            Assert.assertTrue(
                    "Requests for which one recipient ended up in error while scheduled should have recipientR2_1 scheduled (just scheduled and so not yet processed)",
                    failedWhileScheduled.getRecipientsScheduled().contains(recipientR2_1));
            // no more recipient to schedule
            Assert.assertTrue(
                    "Requests for which one recipient ended up in error while scheduled should have no more recipient to schedule",
                    failedWhileScheduled.getRecipientsToSchedule().isEmpty());
        }
    }

    @Test
    public void testScheduleWhileProcessFailConcurrent() throws Exception {
        // possible because of rule that could not be matched that has been retried and then matched while first recipients are being handled
        // we cannot directly spy on jpa repository so we need to do this by hand: (don't ask me why but it cannot be done later...)
        INotificationRequestRepository spiedRepo = Mockito.mock(INotificationRequestRepository.class,
                MockReset.withSettings(MockReset.AFTER)
                        .defaultAnswer(AdditionalAnswers.delegatesTo(
                                notificationRequestRepository)));
        // now we need to inject it into the service thanks to ReflectionTestUtils
        ReflectionTestUtils.setField(notificationRegistrationService, "notificationRequestRepository", spiedRepo);
        ReflectionTestUtils.setField(notificationMatchingService, "notificationRequestRepository", spiedRepo);
        ReflectionTestUtils.setField(notificationProcessingService, "notificationRequestRepository", spiedRepo);
        // possible because of retry in case of rule that could not be matched and previous recipient is failing
        Init2Rule3Recipient twoRules3Recipients = new Init2Rule3Recipient(true);
        PluginConfiguration recipientR1_1 = twoRules3Recipients.getRecipientR1_1();
        PluginConfiguration recipientR1_2 = twoRules3Recipients.getRecipientR1_2();
        PluginConfiguration recipientR2_1 = twoRules3Recipients.getRecipientR2_1();
        // we consider that rule2 could not be matched initially while rule1 could be match
        JsonObject elementBothRules = initElement("elementBothRule.json");
        JsonElement metadata = gson.toJsonTree(globalMetadata);
        List<NotificationRequest> beingProcessed = new ArrayList<>();
        for (int i = 0; i < properties.getMaxBulkSize() / 2; i++) {
            NotificationRequest request = new NotificationRequest(elementBothRules,
                                                                  metadata.getAsJsonObject(),
                                                                  AbstractRequestEvent.generateRequestId(),
                                                                  REQUEST_OWNER,
                                                                  OffsetDateTime.now(),
                                                                  // was in state SCHEDULED because job have been scheduled but match has set requests in state TO_SCHEDULE
                                                                  NotificationState.TO_SCHEDULE_BY_RECIPIENT,
                                                                  new HashSet<>());
            request.getRecipientsToSchedule().add(recipientR2_1);
            request.getRecipientsScheduled().add(recipientR1_1);
            request.getRecipientsScheduled().add(recipientR1_2);
            beingProcessed.add(request);
        }
        beingProcessed = notificationRequestRepository.saveAll(beingProcessed);
        CountDownLatch latch = new CountDownLatch(1);
        //small trick so we are sure the same of the method is really the one needed and we don't care about canonical name & co
        final List<String> finalHandleRecipientResultsMethodName = new ArrayList<>(1);
        Mockito.doAnswer(invocation -> {
            finalHandleRecipientResultsMethodName.add(invocation.getMethod().getName());
            CompletableFuture.runAsync(() -> {
                runtimeTenantResolver.forceTenant(getDefaultTenant());
                recipientService.scheduleNotificationJobs();
                latch.countDown();
            });
            return invocation.callRealMethod();
        }).doAnswer((InvocationOnMock::callRealMethod)).when(notificationProcessingService)
                .handleRecipientResultsConcurrent(Mockito.anyList(), Mockito.any(), Mockito.anyCollection());
        // There is no good candidate to wait for scheduleNotificationJobs in handleRecipientResultsConcurrent to end its execution so we can only use notificationRepo.saveAll
        // as we cannot call abstract method with invocation::callRealMethod, we bypass the spy by calling the real method from the real bean
        // !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!           WARNING           !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
        // This only works here because we are not relying on verification feature of the spy and only want to add some logic before calling the real method
        Mockito.doAnswer(invocation -> {
            // lets check just the caller is handleRecipientResultsConcurrent so as not to block anything else
            try {
                throw new Exception();
            } catch (Exception e) {
                // lets see if saveAll was called by handleRecipientResultsConcurrent i.e mock has been called so list has 1 element
                if (finalHandleRecipientResultsMethodName.size() == 1) {
                    if (Arrays.stream(e.getStackTrace()).anyMatch(ste -> ste.getMethodName()
                            .equals(finalHandleRecipientResultsMethodName.get(0)))) {
                        Assert.assertTrue(
                                "Latch could not be released in less then 1 minutes! matchRequestNRecipient was too long.",
                                latch.await(1, TimeUnit.MINUTES));
                    }
                }
            }
            return notificationRequestRepository.saveAll(invocation.getArgument(0));
        }).when(spiedRepo).saveAll(Mockito.anyCollection());
        notificationProcessingService.processRequests(beingProcessed, recipientR1_1);
        // requests should be in state ERROR, recipientR1_1 should be in error, event to API callers have been send to say that there was an error
        // recipientR1_2 is still scheduled, recipientR2_1 is scheduled, rulesToMatch is empty, no more recipients to schedule
        List<NotificationRequest> scheduledWhileFailedRequests = notificationRequestRepository
                .findAllById(beingProcessed.stream().map(NotificationRequest::getId).collect(Collectors.toSet()));
        Assert.assertTrue(
                "Requests scheduled while one recipient fails should all be in state " + NotificationState.SCHEDULED
                        + " and not " + scheduledWhileFailedRequests.get(0).getState(),
                scheduledWhileFailedRequests.stream()
                        .allMatch(request -> request.getState() == NotificationState.SCHEDULED));
        for (NotificationRequest scheduledWhileFailed : scheduledWhileFailedRequests) {
            //no rules to match
            Assert.assertTrue("Requests scheduled while one recipient fails should have no rules to match",
                              scheduledWhileFailed.getRulesToMatch().isEmpty());
            //recipientR1_1 in error
            Assert.assertEquals("Requests scheduled while one recipient fails should have only 1 recipient in error",
                                1,
                                scheduledWhileFailed.getRecipientsInError().size());
            Assert.assertTrue("Requests scheduled while one recipient fails should have recipientR1_1 in error",
                              scheduledWhileFailed.getRecipientsInError().contains(recipientR1_1));
            //recipientR1_2 & recipientR2_1 scheduled
            Assert.assertEquals("Requests scheduled while one recipient fails should have two recipient scheduled",
                                2,
                                scheduledWhileFailed.getRecipientsScheduled().size());
            Assert.assertTrue(
                    "Requests scheduled while one recipient fails should have recipientR1_2 scheduled (not yet processed)",
                    scheduledWhileFailed.getRecipientsScheduled().contains(recipientR1_2));
            Assert.assertTrue(
                    "Requests scheduled while one recipient fails should have recipientR2_1 scheduled (just scheduled and so not yet processed)",
                    scheduledWhileFailed.getRecipientsScheduled().contains(recipientR2_1));
            // no more recipient to schedule
            Assert.assertTrue("Requests scheduled while one recipient fails should have no more recipient to schedule",
                              scheduledWhileFailed.getRecipientsToSchedule().isEmpty());
        }
    }

    @Test
    public void testProcessFailWhileRetryConcurrent() throws Exception {
        // possible because of rule that could not be matched that is being retried while first recipients have not yet been handled
        // we cannot directly spy on jpa repository so we need to do this by hand: (don't ask me why but it cannot be done later...)
        INotificationRequestRepository spiedRepo = Mockito.mock(INotificationRequestRepository.class,
                MockReset.withSettings(MockReset.AFTER)
                        .defaultAnswer(AdditionalAnswers.delegatesTo(
                                notificationRequestRepository)));
        // now we need to inject it into the service thanks to ReflectionTestUtils
        ReflectionTestUtils.setField(notificationRegistrationService, "notificationRequestRepository", spiedRepo);
        ReflectionTestUtils.setField(notificationMatchingService, "notificationRequestRepository", spiedRepo);
        ReflectionTestUtils.setField(notificationProcessingService, "notificationRequestRepository", spiedRepo);
        // possible because of rules that could not be matched
        // Init two rules with multiple recipients
        Init2Rule3Recipient twoRules3Recipients = new Init2Rule3Recipient(true);
        PluginConfiguration recipientR1_1 = twoRules3Recipients.getRecipientR1_1();
        PluginConfiguration recipientR1_2 = twoRules3Recipients.getRecipientR1_2();
        Rule rule2 = twoRules3Recipients.getRule2();
        // lets init some requests that have been matched and will be scheduled
        // we consider that rule2 could not be matched initially while rule1 could be match
        JsonObject elementBothRules = initElement("elementBothRule.json");
        JsonElement metadata = gson.toJsonTree(globalMetadata);
        List<NotificationRequest> beingRetried = new ArrayList<>();
        List<NotificationRequestEvent> beingRetriedEvents = new ArrayList<>();
        for (int i = 0; i < properties.getMaxBulkSize() / 2; i++) {
            NotificationRequest request = new NotificationRequest(elementBothRules,
                                                                  metadata.getAsJsonObject(),
                                                                  AbstractRequestEvent.generateRequestId(),
                                                                  REQUEST_OWNER,
                                                                  OffsetDateTime.now(),
                                                                  NotificationState.SCHEDULED,
                                                                  Sets.newHashSet(rule2));
            request.getRecipientsScheduled().add(recipientR1_1);
            request.getRecipientsScheduled().add(recipientR1_2);
            request.setState(NotificationState.ERROR);
            beingRetried.add(request);
            beingRetriedEvents.add(new NotificationRequestEvent(request.getPayload(),
                                                                request.getMetadata(),
                                                                request.getRequestId(),
                                                                request.getRequestOwner()));
        }
        beingRetried = notificationRequestRepository.saveAll(beingRetried);
        //prepare mockito to control concurrency
        CountDownLatch latch = new CountDownLatch(1);
        // we want to call scheduleNotificationJobs while registerNotificationRequests is calling the scheduleJobForOneRecipient for recipientR1_1.
        // in reality we want any recipient but recipientR2_1 that has no requests to send because rule2 could not be matched
        final List<String> finalHandleRetryMethodName = new ArrayList<>(1);
        final List<NotificationRequest> finalBeingRetried = beingRetried;
        Mockito.doAnswer(invocation -> {
            finalHandleRetryMethodName.add(invocation.getMethod().getName());
            // There is no action to simulate
            CompletableFuture.runAsync(() -> {
                runtimeTenantResolver.forceTenant(getDefaultTenant());
                notificationProcessingService.processRequests(finalBeingRetried, recipientR1_1);
                latch.countDown();
            });
            return invocation.callRealMethod();
        }).doAnswer((InvocationOnMock::callRealMethod)).when(notificationRegistrationService).handleRetryRequestsConcurrent(beingRetriedEvents);
        // There is no good candidate to wait for scheduleNotificationJobs, in handleRetryRequestsConcurrent, to end its execution so we can only use notificationRepo.saveAll
        // as we cannot call abstract method with invocation::callRealMethod, we bypass the spy by calling the real method from the real bean
        // !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!           WARNING           !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
        // This only works here because we are not relying on verification feature of the spy and only want to add some logic before calling the real method
        Mockito.doAnswer(invocation -> {
            // lets check just the caller is handleRetryRequestsConcurrent so as not to block anything else
            try {
                throw new Exception();
            } catch (Exception e) {
                // lets see if saveAll was called by handleRetryRequestsConcurrent i.e mock has been called so list has 1 element
                if (finalHandleRetryMethodName.size() == 1) {
                    if (Arrays.stream(e.getStackTrace())
                            .anyMatch(ste -> ste.getMethodName().equals(finalHandleRetryMethodName.get(0)))) {
                        Assert.assertTrue(
                                "Latch could not be released in less then 1 minutes! scheduleNotificationJobs was too long.",
                                latch.await(5, TimeUnit.MINUTES));
                    }
                }
            }
            return notificationRequestRepository.saveAll(invocation.getArgument(0));
        }).when(spiedRepo).saveAll(Mockito.anyCollection());
        notificationRegistrationService.registerNotificationRequests(beingRetriedEvents);
        // As we are simulating actions so that requests are being retried while recipientR1_1 is being processed, requests should be in state GRANTED(retry on rule to match)
        List<NotificationRequest> processedWhileRetriedRequests = notificationRequestRepository
                .findAllById(beingRetried.stream().map(NotificationRequest::getId).collect(Collectors.toSet()));
        Assert.assertTrue("Requests for which one recipient ended up in error while retried should all be in state "
                                  + NotificationState.GRANTED,
                          processedWhileRetriedRequests.stream()
                                  .allMatch(r -> r.getState() == NotificationState.GRANTED));
        for (NotificationRequest processedWhileRetried : processedWhileRetriedRequests) {
            Assert.assertEquals(
                    "Requests for which one recipient ended up in error while retried should have 1 rule to match left",
                    1,
                    processedWhileRetried.getRulesToMatch().size());
            Assert.assertTrue(
                    "Requests for which one recipient ended up in error while retried should have rule2 to match left",
                    processedWhileRetried.getRulesToMatch().contains(rule2));
            Assert.assertEquals(
                    "Requests for which one recipient ended up in error while retried should have only 1 recipient"
                            + " to schedule (process fail => recipientR1_1 in error, than retry which puts recipientR1_1 in toSchedule)",
                    1,
                    processedWhileRetried.getRecipientsToSchedule().size());
            Assert.assertTrue(
                    "Requests for which one recipient ended up in error while retried should have recipientR1_1"
                            + " to schedule (process fail => recipientR1_1 in error, than retry which puts recipientR1_1 in toSchedule)",
                    processedWhileRetried.getRecipientsToSchedule().contains(recipientR1_1));
            Assert.assertEquals(
                    "Requests for which one recipient ended up in error while retried should have 2 recipients scheduled",
                    1,
                    processedWhileRetried.getRecipientsScheduled().size());
            Assert.assertTrue(
                    "Requests for which one recipient ended up in error while retried should have recipientR1_2 scheduled",
                    processedWhileRetried.getRecipientsScheduled().contains(recipientR1_2));
            Assert.assertTrue(
                    "Requests for which one recipient ended up in error while retried should have no recipients in error "
                            + "(process fail => recipientR1_1 in error, than retry which puts recipientR1_1 in toSchedule)",
                    processedWhileRetried.getRecipientsInError().isEmpty());
        }
    }

    @Test
    public void testRetryWhileProcessFailConcurrent() throws Exception {
        // possible because of rule that could not be matched that is being retried while first recipients are being handled
        // we cannot directly spy on jpa repository so we need to do this by hand: (don't ask me why but it cannot be done later...)
        INotificationRequestRepository spiedRepo = Mockito.mock(INotificationRequestRepository.class,
                MockReset.withSettings(MockReset.AFTER)
                        .defaultAnswer(AdditionalAnswers.delegatesTo(
                                notificationRequestRepository)));
        // now we need to inject it into the service thanks to ReflectionTestUtils
        ReflectionTestUtils.setField(notificationRegistrationService, "notificationRequestRepository", spiedRepo);
        ReflectionTestUtils.setField(notificationMatchingService, "notificationRequestRepository", spiedRepo);
        ReflectionTestUtils.setField(notificationProcessingService, "notificationRequestRepository", spiedRepo);
        // possible because of retry in case of rule that could not be matched and previous recipient is failing
        Init2Rule3Recipient twoRules3Recipients = new Init2Rule3Recipient(true);
        PluginConfiguration recipientR1_1 = twoRules3Recipients.getRecipientR1_1();
        PluginConfiguration recipientR1_2 = twoRules3Recipients.getRecipientR1_2();
        Rule rule2 = twoRules3Recipients.getRule2();
        // we consider that rule2 could not be matched initially while rule1 could be match
        JsonObject elementBothRules = initElement("elementBothRule.json");
        JsonElement metadata = gson.toJsonTree(globalMetadata);
        List<NotificationRequest> beingProcessed = new ArrayList<>();
        List<NotificationRequestEvent> toRetry = new ArrayList<>();
        for (int i = 0; i < properties.getMaxBulkSize() / 2; i++) {
            NotificationRequest request = new NotificationRequest(elementBothRules,
                                                                  metadata.getAsJsonObject(),
                                                                  AbstractRequestEvent.generateRequestId(),
                                                                  REQUEST_OWNER,
                                                                  OffsetDateTime.now(),
                                                                  // was in state SCHEDULED because job have been scheduled but match has set requests in state TO_SCHEDULE
                                                                  NotificationState.SCHEDULED,
                                                                  Sets.newHashSet(rule2));
            request.getRecipientsScheduled().add(recipientR1_1);
            request.getRecipientsScheduled().add(recipientR1_2);
            beingProcessed.add(request);
            toRetry.add(new NotificationRequestEvent(request.getPayload(),
                                                     request.getMetadata(),
                                                     request.getRequestId(),
                                                     request.getRequestOwner()));
        }
        beingProcessed = notificationRequestRepository.saveAll(beingProcessed);
        CountDownLatch latch = new CountDownLatch(1);
        //small trick so we are sure the same of the method is really the one needed and we don't care about canonical name & co
        final List<String> finalHandleRecipientResultsMethodName = new ArrayList<>(1);
        Mockito.doAnswer(invocation -> {
            finalHandleRecipientResultsMethodName.add(invocation.getMethod().getName());
            CompletableFuture.runAsync(() -> {
                runtimeTenantResolver.forceTenant(getDefaultTenant());
                notificationRegistrationService.registerNotificationRequests(toRetry);
                latch.countDown();
            });
            return invocation.callRealMethod();
        }).doAnswer((InvocationOnMock::callRealMethod)).when(notificationProcessingService)
                .handleRecipientResultsConcurrent(Mockito.anyList(), Mockito.any(), Mockito.anyCollection());
        // There is no good candidate to wait for registerNotificationRequests in handleRecipientResultsConcurrent to end its execution so we can only use notificationRepo.saveAll
        // as we cannot call abstract method with invocation::callRealMethod, we bypass the spy by calling the real method from the real bean
        // !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!           WARNING           !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
        // This only works here because we are not relying on verification feature of the spy and only want to add some logic before calling the real method
        Mockito.doAnswer(invocation -> {
            // lets check just the caller is handleRecipientResultsConcurrent so as not to block anything else
            try {
                throw new Exception();
            } catch (Exception e) {
                // lets see if saveAll was called by handleRecipientResultsConcurrent i.e mock has been called so list has 1 element
                if (finalHandleRecipientResultsMethodName.size() == 1) {
                    if (Arrays.stream(e.getStackTrace()).anyMatch(ste -> ste.getMethodName()
                            .equals(finalHandleRecipientResultsMethodName.get(0)))) {
                        Assert.assertTrue(
                                "Latch could not be released in less then 1 minutes! matchRequestNRecipient was too long.",
                                latch.await(1, TimeUnit.MINUTES));
                    }
                }
            }
            return notificationRequestRepository.saveAll(invocation.getArgument(0));
        }).when(spiedRepo).saveAll(Mockito.anyCollection());
        notificationProcessingService.processRequests(beingProcessed, recipientR1_1);
        // requests should be in state GRANTED, recipientR1_1 should be in error, event to API callers have been send to say that there was an error
        // recipientR1_2 is still scheduled, rule2 is to be matched, no recipients to schedule
        List<NotificationRequest> retriedWhileFailedRequests = notificationRequestRepository
                .findAllById(beingProcessed.stream().map(NotificationRequest::getId).collect(Collectors.toSet()));
        Assert.assertTrue(
                "Requests retried while one recipient fails should all be in state " + NotificationState.GRANTED
                        + " and not " + retriedWhileFailedRequests.get(0).getState(),
                retriedWhileFailedRequests.stream()
                        .allMatch(request -> request.getState() == NotificationState.GRANTED));
        for (NotificationRequest retriedWhileFailed : retriedWhileFailedRequests) {
            //rule2 to match
            Assert.assertEquals("Requests retried while one recipient fails should have only 1 rule to match",
                                1,
                                retriedWhileFailed.getRulesToMatch().size());
            Assert.assertTrue("Requests retried while one recipient fails should have rule2 to match",
                              retriedWhileFailed.getRulesToMatch().contains(rule2));
            //recipientR1_1 in error
            Assert.assertEquals("Requests retried while one recipient fails should have only 1 recipient in error",
                                1,
                                retriedWhileFailed.getRecipientsInError().size());
            Assert.assertTrue("Requests retried while one recipient fails should have recipientR1_1 in error",
                              retriedWhileFailed.getRecipientsInError().contains(recipientR1_1));
            //recipientR1_2 scheduled
            Assert.assertEquals("Requests retried while one recipient fails should have two recipient scheduled",
                    1,
                    retriedWhileFailed.getRecipientsScheduled().size());
            Assert.assertTrue(
                    "Requests retried while one recipient fails should have recipientR1_2 scheduled (not yet processed)",
                    retriedWhileFailed.getRecipientsScheduled().contains(recipientR1_2));
            // no recipient to schedule
            Assert.assertTrue("Requests retried while one recipient fails should have no more recipient to schedule",
                    retriedWhileFailed.getRecipientsToSchedule().isEmpty());
        }
    }


    @Test
    public void testCheckCompletedShouldSendProperEvents() throws EncryptionException, EntityInvalidException, EntityNotFoundException {

        // Given
        PluginConfiguration recipient1 = pluginService.savePluginConfiguration(
                new PluginConfiguration("recipient1", new HashSet<>(), RecipientSender3.PLUGIN_ID));
        PluginConfiguration recipient2 = pluginService.savePluginConfiguration(
                new PluginConfiguration("recipient2", new HashSet<>(), RecipientSender4.PLUGIN_ID));

        String owner1 = "owner1";
        String owner2 = "owner2";
        String halfId = "halfSuccessRequest";
        String successId = "successRequest";
        String errorId = "errorRequest";
        JsonObject payload = initElement("elementRule1.json");
        JsonObject metadata = gson.toJsonTree(globalMetadata).getAsJsonObject();
        OffsetDateTime now = OffsetDateTime.now();
        HashSet<Rule> rules = new HashSet<>();

        NotificationRequest halfSuccessRequest = new NotificationRequest(payload, metadata, halfId, owner1, now, NotificationState.SCHEDULED, rules);
        halfSuccessRequest.getRecipientsInError().add(recipient1);
        halfSuccessRequest.getSuccessRecipients().add(recipient2);

        NotificationRequest successRequest = new NotificationRequest(payload, metadata, successId, owner2, now, NotificationState.SCHEDULED, rules);
        successRequest.getSuccessRecipients().addAll(Arrays.asList(recipient1, recipient2));

        NotificationRequest errorRequest = new NotificationRequest(payload, metadata, errorId, owner2, now, NotificationState.SCHEDULED, rules);
        errorRequest.getRecipientsInError().addAll(Arrays.asList(recipient1, recipient2));

        notificationRequestRepository.saveAll(Arrays.asList(successRequest, errorRequest, halfSuccessRequest));

        // When
        notificationProcessingService.checkCompletedRequests();

        // Then
        Mockito.verify(publisher).publish(notifierEventCaptor.capture());

        Map<String, NotifierEvent> events = notifierEventCaptor.getValue().stream().collect(Collectors.toMap(NotifierEvent::getRequestId, Function.identity()));

        assertTrue(events.keySet().containsAll(Arrays.asList(halfId, successId, errorId)));
        events.forEach((id, notifierEvent) -> {
            assertEquals(2, notifierEvent.getRecipients().size());
            assertEquals(1, notifierEvent.getRecipients().stream().filter(Recipient::isAckRequired).count());
            assertEquals(1, notifierEvent.getRecipients().stream().filter(recipient -> !recipient.isAckRequired()).count());
        });

        NotifierEvent halfEvent = events.get(halfId);
        NotifierEvent successEvent = events.get(successId);
        NotifierEvent errorEvent = events.get(errorId);

        assertEquals(0, successEvent.getRecipients().stream().filter(recipient -> RecipientStatus.ERROR.equals(recipient.getStatus())).count());
        assertEquals(2, successEvent.getRecipients().stream().filter(recipient -> RecipientStatus.SUCCESS.equals(recipient.getStatus())).count());

        assertEquals(2, errorEvent.getRecipients().stream().filter(recipient -> RecipientStatus.ERROR.equals(recipient.getStatus())).count());
        assertEquals(0, errorEvent.getRecipients().stream().filter(recipient -> RecipientStatus.SUCCESS.equals(recipient.getStatus())).count());

        assertEquals(1, halfEvent.getRecipients().stream().filter(recipient -> RecipientStatus.ERROR.equals(recipient.getStatus())).count());
        assertEquals(1, halfEvent.getRecipients().stream().filter(recipient -> RecipientStatus.SUCCESS.equals(recipient.getStatus())).count());
    }

    public void testScheduleWhileMatchConcurrent() throws Exception {
        // This is not possible because we cannot have one request that will pass in state TO_SCHEDULE while passing in state GRANTED
        throw new Exception("This case is not possible");
    }

    public void testMatchWhileRetryConcurrent() throws Exception {
        // This is not possible because we cannot have one request that will pass in state ERROR while passing in state GRANTED
        throw new Exception("This case is not possible");
    }

    private class Init2Rule3Recipient {

        private final PluginConfiguration recipientR1_1;
        private final PluginConfiguration recipientR1_2;
        private final PluginConfiguration recipientR2_1;
        private final Rule rule1;
        private final Rule rule2;


        public Init2Rule3Recipient(boolean recipientR1_1Error) throws Exception {

            recipientR1_1 = pluginService.savePluginConfiguration(
                    new PluginConfiguration(
                            RECIPIENT_R1_1_LABEL,
                            new HashSet<>(),
                            recipientR1_1Error ?
                                    RecipientSenderFail.PLUGIN_ID
                                    : RecipientSender2.PLUGIN_ID));

            recipientR1_2 = pluginService.savePluginConfiguration(
                    new PluginConfiguration(
                            RECIPIENT_R1_2_LABEL,
                            new HashSet<>(),
                            RecipientSender3.PLUGIN_ID));

            recipientR2_1 = pluginService.savePluginConfiguration(
                    new PluginConfiguration(
                            RECIPIENT_R2_1_LABEL,
                            new HashSet<>(),
                            RecipientSender4.PLUGIN_ID));

            PluginConfiguration rule1Plg = new PluginConfiguration(
                    RULE1_LABEL,
                    Sets.newHashSet(
                            IPluginParam.build(DefaultRuleMatcher.ATTRIBUTE_TO_SEEK_FIELD_NAME, "nature"),
                            IPluginParam.build(DefaultRuleMatcher.ATTRIBUTE_VALUE_TO_SEEK_FIELD_NAME, "TM")),
                    DefaultRuleMatcher.PLUGIN_ID);

            PluginConfiguration rule2Plg = new PluginConfiguration(
                    RULE2_LABEL,
                    Sets.newHashSet(
                            IPluginParam.build(DefaultRuleMatcher.ATTRIBUTE_TO_SEEK_FIELD_NAME, "info"),
                            IPluginParam.build(DefaultRuleMatcher.ATTRIBUTE_VALUE_TO_SEEK_FIELD_NAME, "toto")),
                    DefaultRuleMatcher.PLUGIN_ID);

            rule1 = ruleRepo.findByRulePluginBusinessId(
                            ruleService.createOrUpdateRule(RuleDTO.build(rule1Plg, Sets.newHashSet(recipientR1_1.getBusinessId(), recipientR1_2.getBusinessId()))).getId())
                    .orElseThrow(() -> new Exception("DB has bugged"));

            rule2 = ruleRepo.findByRulePluginBusinessId(
                            ruleService.createOrUpdateRule(RuleDTO.build(rule2Plg, Sets.newHashSet(recipientR2_1.getBusinessId()))).getId())
                    .orElseThrow(() -> new Exception("DB has bugged"));
        }

        public PluginConfiguration getRecipientR1_1() {
            return recipientR1_1;
        }

        public PluginConfiguration getRecipientR1_2() {
            return recipientR1_2;
        }

        public PluginConfiguration getRecipientR2_1() {
            return recipientR2_1;
        }

        public Rule getRule2() {
            return rule2;
        }

        public Rule getRule1() {
            return rule1;
        }
    }
}
