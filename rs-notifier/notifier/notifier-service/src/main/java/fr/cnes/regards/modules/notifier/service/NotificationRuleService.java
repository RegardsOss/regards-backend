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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Order;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.Errors;
import org.springframework.validation.MapBindingResult;
import org.springframework.validation.Validator;

import com.google.common.collect.Sets;
import com.google.gson.JsonElement;
import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.framework.modules.jobs.service.IJobInfoService;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.notification.NotificationLevel;
import fr.cnes.regards.framework.notification.client.INotificationClient;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.framework.utils.plugins.exception.NotAvailablePluginConfigurationException;
import fr.cnes.regards.modules.notifier.dao.INotificationRequestRepository;
import fr.cnes.regards.modules.notifier.domain.NotificationRequest;
import fr.cnes.regards.modules.notifier.domain.Rule;
import fr.cnes.regards.modules.notifier.domain.plugin.IRecipientNotifier;
import fr.cnes.regards.modules.notifier.domain.plugin.IRuleMatcher;
import fr.cnes.regards.modules.notifier.dto.in.NotificationRequestEvent;
import fr.cnes.regards.modules.notifier.dto.out.NotificationState;
import fr.cnes.regards.modules.notifier.dto.out.NotifierEvent;
import fr.cnes.regards.modules.notifier.service.cache.AbstractCacheableRule;
import fr.cnes.regards.modules.notifier.service.conf.NotificationConfigurationProperties;
import fr.cnes.regards.modules.notifier.service.job.NotificationJob;

/**
 * Service for checking {@link Rule} applied to {@link JsonElement} for notification sending
 * @author Kevin Marchois
 *
 */
@Service
@MultitenantTransactional
public class NotificationRuleService extends AbstractCacheableRule implements INotificationRuleService {

    private static final Logger LOGGER = LoggerFactory.getLogger(NotificationRuleService.class);

    @Autowired
    private IPluginService pluginService;

    @Autowired
    private INotificationRequestRepository notificationRequestRepo;

    @Autowired
    private Validator validator;

    @Autowired
    private NotificationConfigurationProperties properties;

    @Autowired
    private IJobInfoService jobInfoService;

    @Autowired
    private INotificationClient notificationClient;

    @Autowired
    private IRuntimeTenantResolver tenantResolver;

    @Autowired
    private IPublisher publisher;

    private Collection<NotificationRequest> notifyRecipient(List<NotificationRequest> notificationRequests,
            PluginConfiguration recipientConfiguration) {
        try {
            // check that all send method of recipient return true
            Collection<NotificationRequest> errors = ((IRecipientNotifier) this.pluginService
                    .getPlugin(recipientConfiguration.getBusinessId())).send(notificationRequests);
            return errors == null ? new HashSet<>() : errors;
        } catch (Exception e) {
            // if there is an exception, we consider none of the request could be handled,
            // either due to error from plugin configuration or the plugin implementation itself
            LOGGER.error("Error while sending notification to receiver", e);
            return notificationRequests;
        }
    }

    @Override
    public Pair<Integer, Integer> processRequest(List<NotificationRequest> notificationRequests,
            PluginConfiguration recipient) {
        long startTime = System.currentTimeMillis();
        LOGGER.debug("------------->>> Reception of {} notification  event, start of notification process {} ms",
                     notificationRequests.size(),
                     startTime);

        // first lets check is recipient is not null, in this case it means it has been remove from all notification requests so the job simply has nothing to do
        if (recipient != null) {
            Collection<NotificationRequest> notificationsInError = notifyRecipient(notificationRequests, recipient);
            // handle successful notification for this recipient
            Set<NotificationRequest> notificationsSuccessfullySent = notificationRequests.stream()
                    .filter(nr -> !notificationsInError.contains(nr)).collect(Collectors.toSet());
            for (NotificationRequest successfullySent : notificationsSuccessfullySent) {
                // this notification request has been successfully handled  for this recipient, so lets remove it from scheduled
                successfullySent.getRecipientsScheduled().remove(recipient);
            }
            // handle notification errors for this recipient
            if (!notificationsInError.isEmpty()) {
                List<NotifierEvent> errorsToSend = new ArrayList<>(notificationsInError.size());
                for (NotificationRequest inError : notificationsInError) {
                    // this notification request could not be handled for this recipient, so lets remove it from scheduled but
                    // keep trace of it in recipients in error for retry purposes
                    inError.getRecipientsScheduled().remove(recipient);
                    inError.getRecipientsInError().add(recipient);
                    inError.setState(NotificationState.ERROR);
                    errorsToSend.add(new NotifierEvent(inError.getRequestId(),
                                                       inError.getRequestOwner(),
                                                       NotificationState.ERROR));
                }
                publisher.publish(errorsToSend);
            }
            // finally, save all notifications (success removal will be done later)
            notificationRequestRepo.saveAll(notificationRequests);
            return Pair.of(notificationsSuccessfullySent.size(), notificationsInError.size());
        }
        return Pair.of(notificationRequests.size(), 0);
    }

    @Override
    public void registerNotificationRequests(List<NotificationRequestEvent> events) {
        if (!events.isEmpty()) {
            // first handle retry by identifying NRE with the same requestId as one request with recipient in error
            Set<NotificationRequestEvent> notRetryEvents = handleRetryRequests(events);
            // then check validity
            try {
                Set<Rule> rules = getRules();
                Set<NotificationRequest> notificationToRegister = notRetryEvents.stream()
                        .map(event -> initNotificationRequest(event, rules)).collect(Collectors.toSet());
                notificationToRegister.remove(null);
                this.notificationRequestRepo.saveAll(notificationToRegister);
                LOGGER.debug("------------->>> {} notifications registred", notificationToRegister.size());
            } catch (ExecutionException e) {
                // Rules could not be retrieve, so let deny everything.
                List<NotifierEvent> denied = notRetryEvents.stream()
                        .map(event -> new NotifierEvent(event.getRequestId(),
                                                        event.getRequestOwner(),
                                                        NotificationState.DENIED)).collect(Collectors.toList());
                publisher.publish(denied);
            }
        }
    }

    private Set<NotificationRequestEvent> handleRetryRequests(List<NotificationRequestEvent> events) {
        Map<String, NotificationRequestEvent> eventsPerRequestId = events.stream()
                .collect(Collectors.toMap(NotificationRequestEvent::getRequestId, Function.identity()));
        Set<NotificationRequest> alreadyKnownRequests = this.notificationRequestRepo
                .findAllByStateAndRequestIdIn(NotificationState.ERROR, eventsPerRequestId.keySet());
        Set<NotificationRequest> updated = new HashSet<>();
        for (NotificationRequest known : alreadyKnownRequests) {
            if (!known.getRecipientsInError().isEmpty()) {
                // This is a retry, lets prepare everything so it can be retried properly
                known.getRecipientsToSchedule().addAll(known.getRecipientsInError());
                known.getRecipientsInError().clear();
                known.setState(NotificationState.TO_SCHEDULE_BY_RECIPIENT);
                updated.add(known);
                publisher.publish(new NotifierEvent(known.getRequestId(),
                                                    known.getRequestOwner(),
                                                    NotificationState.GRANTED));
                //lets remove this requestId from map so we can later reconstruct the collection of event still to be handled
                eventsPerRequestId.put(known.getRequestId(), null);
            }
        }
        notificationRequestRepo.saveAll(updated);
        return eventsPerRequestId.values().stream().filter(Objects::nonNull).collect(Collectors.toSet());
    }

    /**
     * Check if a notification event is valid and create a request publish an error otherwise
     * @return a implemented {@link NotificationRequest} or null if invalid
     */
    private NotificationRequest initNotificationRequest(NotificationRequestEvent event, Set<Rule> rules) {
        Errors errors = new MapBindingResult(new HashMap<>(), NotificationRequestEvent.class.getName());
        this.validator.validate(event, errors);

        if (!errors.hasErrors()) {
            publisher.publish(new NotifierEvent(event.getRequestId(),
                                                event.getRequestOwner(),
                                                NotificationState.GRANTED));
            return new NotificationRequest(event.getPayload(),
                                           event.getMetadata(),
                                           event.getRequestId(),
                                           event.getRequestOwner(),
                                           event.getRequestDate(),
                                           NotificationState.GRANTED,
                                           rules.stream().map(Rule::getRulePlugin).collect(Collectors.toSet()));
        }
        this.notificationClient.notify(errors.toString(),
                                       "A NotificationRequestEvent received is invalid",
                                       NotificationLevel.ERROR,
                                       DefaultRole.ADMIN);
        publisher.publish(new NotifierEvent(event.getRequestId(), event.getRequestOwner(), NotificationState.DENIED));
        return null;
    }

    @Override
    public void cleanCache() {
        this.cleanTenantCache(tenantResolver.getTenant());
    }

    @Override
    public Pair<Integer, Integer> matchRequestNRecipient() {
        Page<NotificationRequest> toBeMatched = notificationRequestRepo.findByState(NotificationState.GRANTED,
                                                                                    PageRequest.of(0,
                                                                                                   properties
                                                                                                           .getMaxBulkSize(),
                                                                                                   Sort.by(Order.asc(
                                                                                                           NotificationRequest.REQUEST_DATE_JPQL_NAME))));
        // dispatch notification per recipient
        Set<PluginConfiguration> recipientsActuallyMatched = new HashSet<>();
        Set<NotificationRequest> requestsActuallyMatched = new HashSet<>();
        Set<NotificationRequest> requestsInError = new HashSet<>();
        try {
            //TODO: iterate over notification request that now know which rules are to be matched (association of pattern strategy(rules) and command(notification requests know what to apply))
            Set<Rule> rules = getRules();
            for (Rule rule : rules) {
                try {
                    IRuleMatcher rulePlugin = this.pluginService.getPlugin(rule.getRulePlugin().getBusinessId());
                    for (NotificationRequest notificationRequest : toBeMatched) {
                        // check if the  element match with the rule
                        if (rulePlugin.match(notificationRequest.getPayload())) {
                            for (PluginConfiguration recipient : rule.getRecipients()) {
                                notificationRequest.getRecipientsToSchedule().add(recipient);
                                // this is done so we can know how many recipient have been matched by at least one request
                                requestsActuallyMatched.add(notificationRequest);
                                recipientsActuallyMatched.add(recipient);
                            }
                        }
                        // as we set notification state in error as soon as possible, we should not override this right now
                        // moreover, we do not stop the matching process as we want the rules that can be matched
                        if (notificationRequest.getState() != NotificationState.ERROR) {
                            notificationRequest.setState(NotificationState.TO_SCHEDULE_BY_RECIPIENT);
                        }
                    }
                } catch (ModuleException | NotAvailablePluginConfigurationException e) {
                    // exception from rule plugin instantiation
                    LOGGER.error(String.format("Error while get plugin with id %S", rule.getRulePlugin().getId()), e);
                    //                    notificationRequest.getRecipientsInError().addAll(rule.getRecipients());
                    //                    notificationRequest.setState(NotificationState.ERROR);
                    //                    requestsInError.add(notificationRequest);
                    //                    publisher.publish(new NotifierEvent(notificationRequest.getRequestId(), NotificationState.ERROR));
                }
            }
            // do not forget to handle all requests that were not matched by any rule and so should be considered successful
            // right now (for simplicity issue lets set its state to SCHEDULED and wait for the check to be done)
            //FIXME
            Set<Long> notMatchedNNotErrorIds = toBeMatched.stream().filter(r -> !requestsActuallyMatched.contains(r))
                    .map(NotificationRequest::getId).collect(Collectors.toSet());
            //            updateState(NotificationState.SCHEDULED, notMatchedAtAllIds);
        } catch (ExecutionException e) {
            LOGGER.error("Could not match requests and recipients because rules could not be retrieved", e);
            return Pair.of(0, 0);
        }
        return Pair.of(recipientsActuallyMatched.size(), recipientsActuallyMatched.size());
    }

    private Page<NotificationRequest> findPageToScheduleContaining(PluginConfiguration recipient) {
        return notificationRequestRepo
                .findPageByStateAndRecipientsToScheduleContaining(NotificationState.TO_SCHEDULE_BY_RECIPIENT,
                                                                  recipient,
                                                                  PageRequest.of(0,
                                                                                 properties.getMaxBulkSize(),
                                                                                 Sort.by(Order.asc(NotificationRequest.REQUEST_DATE_JPQL_NAME))));
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Set<Long> scheduleJobForOneRecipient(PluginConfiguration recipient) {
        Set<Long> toScheduleId = new HashSet<>();
        // we need to find a page of notification request that contains this recipient to be scheduled
        Page<NotificationRequest> requestsToSchedule = findPageToScheduleContaining(recipient);
        if (!requestsToSchedule.isEmpty()) {
            for (NotificationRequest request : requestsToSchedule) {
                request.getRecipientsToSchedule().remove(recipient);
                request.getRecipientsScheduled().add(recipient);
                // the state of this requests cannot be update right now otherwise if a job should be scheduled for the next rule too it won't be.
                toScheduleId.add(request.getId());
            }
            JobInfo notificationJobForRecipient = new JobInfo(false,
                                                              0,
                                                              Sets.newHashSet(new JobParameter(NotificationJob.NOTIFICATION_REQUEST_IDS,
                                                                                               toScheduleId),
                                                                              new JobParameter(NotificationJob.RECIPIENT_BUSINESS_ID,
                                                                                               recipient
                                                                                                       .getBusinessId())),
                                                              null,
                                                              NotificationJob.class.getName());
            jobInfoService.createAsQueued(notificationJobForRecipient);
            return toScheduleId;
        }
        return new HashSet<>();
    }

    @Override
    public int checkSuccess() {
        Page<NotificationRequest> successes = findPageScheduledWithNoMoreRecipientToHandle();
        List<NotifierEvent> responseToSend = new ArrayList<>();
        for (NotificationRequest requestInSuccess : successes) {
            responseToSend.add(new NotifierEvent(requestInSuccess.getRequestId(),
                                                 requestInSuccess.getRequestOwner(),
                                                 NotificationState.SUCCESS));
        }
        notificationRequestRepo.deleteInBatch(successes);
        publisher.publish(responseToSend);
        return successes.getSize();
    }

    private Page<NotificationRequest> findPageScheduledWithNoMoreRecipientToHandle() {
        return notificationRequestRepo
                .findByStateByRecipientsToScheduleEmptyByRecipientsScheduledEmptyByRecipientsInErrorEmpty(
                        NotificationState.SCHEDULED,
                        PageRequest.of(0,
                                       properties.getMaxBulkSize(),
                                       Sort.by(Order.asc(NotificationRequest.REQUEST_DATE_JPQL_NAME))));
    }

    @Override
    public void updateState(NotificationState state, Set<Long> ids) {
        if (!ids.isEmpty()) {
            notificationRequestRepo.updateState(state, ids);
        }
    }
}
