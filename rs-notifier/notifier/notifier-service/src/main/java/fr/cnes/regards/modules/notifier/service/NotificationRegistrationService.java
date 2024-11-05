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
package fr.cnes.regards.modules.notifier.service;

import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.amqp.event.AbstractRequestEvent;
import fr.cnes.regards.framework.amqp.event.notifier.NotificationRequestEvent;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.framework.notification.NotificationLevel;
import fr.cnes.regards.framework.notification.client.INotificationClient;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.framework.utils.plugins.exception.NotAvailablePluginConfigurationException;
import fr.cnes.regards.modules.notifier.dao.INotificationRequestRepository;
import fr.cnes.regards.modules.notifier.domain.NotificationRequest;
import fr.cnes.regards.modules.notifier.domain.Rule;
import fr.cnes.regards.modules.notifier.domain.plugin.IRecipientNotifier;
import fr.cnes.regards.modules.notifier.dto.in.SpecificRecipientNotificationRequestEvent;
import fr.cnes.regards.modules.notifier.dto.out.NotificationState;
import fr.cnes.regards.modules.notifier.dto.out.NotifierEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.Errors;
import org.springframework.validation.MapBindingResult;
import org.springframework.validation.Validator;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@MultitenantTransactional
@Scope(proxyMode = ScopedProxyMode.TARGET_CLASS)
public class NotificationRegistrationService {

    protected static final String OPTIMIST_LOCK_LOG_MSG = "Another schedule has updated some of the requests handled by this method while it was running.";

    private static final Logger LOGGER = LoggerFactory.getLogger(NotificationRegistrationService.class);

    public static final String BUSINESS_ID_FIELD = "businessId";

    private final Validator validator;

    private final INotificationClient notificationClient;

    private final RuleCache ruleCache;

    private final INotificationRequestRepository notificationRequestRepository;

    private final IPublisher publisher;

    private final NotificationRegistrationService self;

    private final IPluginService pluginService;

    public NotificationRegistrationService(INotificationRequestRepository notificationRequestRepository,
                                           IPublisher publisher,
                                           Validator validator,
                                           INotificationClient notificationClient,
                                           RuleCache ruleCache,
                                           NotificationRegistrationService notificationRegistrationService,
                                           IPluginService pluginService) {
        this.notificationRequestRepository = notificationRequestRepository;
        this.publisher = publisher;
        this.validator = validator;
        this.notificationClient = notificationClient;
        this.ruleCache = ruleCache;
        this.self = notificationRegistrationService;
        this.pluginService = pluginService;
    }

    /**
     * Save all notification request events in database
     */
    public void registerNotificationRequests(List<? extends NotificationRequestEvent> events) {
        if (!events.isEmpty()) {

            long startTime = System.currentTimeMillis();
            // first : ignore all duplicated events in current batch
            Map<Boolean, List<? extends NotificationRequestEvent>> eventsGroupedByDuplicated = findDuplicatedNotifEvents(
                events);
            List<? extends NotificationRequestEvent> duplicatedEvents = eventsGroupedByDuplicated.get(true);
            List<? extends NotificationRequestEvent> notDuplicatedEvents = eventsGroupedByDuplicated.get(false);
            if (!duplicatedEvents.isEmpty()) {
                LOGGER.warn(
                    "Some notification event are duplicated in same batch. REGARDS keep the first and ignore others."
                    + " Id duplicated : {}",
                    duplicatedEvents.stream().map(AbstractRequestEvent::getRequestId).toList());
            }

            // handle retry by identifying NRE with the same requestId as one request with recipient in error
            LOGGER.debug("Starting RETRY");
            Set<NotificationRequestEvent> notRetryEvents = handleRetryOrIgnoreRequests(notDuplicatedEvents);
            LOGGER.debug("Ending RETRY in {} ms", System.currentTimeMillis() - startTime);

            // then check validity
            try {
                Set<Rule> rules = ruleCache.getRules();

                Set<NotificationRequest> notificationToRegister = notRetryEvents.stream()
                                                                                .map(event -> initNotificationRequest(
                                                                                    event,
                                                                                    rules))
                                                                                .flatMap(Optional::stream)
                                                                                .collect(Collectors.toSet());
                List<NotificationRequest> notificationRequests = notificationRequestRepository.saveAll(
                    notificationToRegister);
                LOGGER.debug("------------->>> {} notifications registered", notificationToRegister.size());
            } catch (ExecutionException e) {
                LOGGER.error(e.getMessage(), e);
                // Rules could not be retrieved, so let deny everything.
                publisher.publish(notRetryEvents.stream()
                                                .map(event -> new NotifierEvent(event.getRequestId(),
                                                                                event.getRequestOwner(),
                                                                                NotificationState.DENIED,
                                                                                OffsetDateTime.now()))
                                                .collect(Collectors.toList()));
            }
        }
    }

    /**
     * Wrapper to handle job crash for a list of requests and a recipient id.
     * Try with optimistic lock to clean associated NotificationRequests in handleJobCrashConcurrent
     */
    public void handleJobCrash(Set<Long> requestIds, String abortedRecipientBusinessId) {
        try {
            self.handleJobCrashConcurrent(requestIds, abortedRecipientBusinessId);
        } catch (ObjectOptimisticLockingFailureException e) {
            LOGGER.trace(OPTIMIST_LOCK_LOG_MSG, e);
            // we retry until it succeed because if it does not succeed on first time it is most likely because of
            // another scheduled method that would then most likely happen at next invocation because execution delays are fixed
            handleJobCrash(requestIds, abortedRecipientBusinessId);
        }
    }

    /**
     * For each request in an aborted job, transfer every recipientScheduled to recipientToSchedule that matches the
     * abortedRecipientBusinessId of the job and then set the request state back to TO_SCHEDULE_BY_RECIPIENT
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleJobCrashConcurrent(Set<Long> requestIds, String abortedRecipientBusinessId) {
        List<NotificationRequest> notificationRequests = notificationRequestRepository.findAllById(requestIds);
        Set<Long> notificationRequestIdsToUpdate = new HashSet<>();
        notificationRequests.forEach(notificationRequest -> {
            if (notificationRequest.getState().isRunning()) {
                LOGGER.error("Job crash detected for request {}. State is reset to be handled on next scheduled "
                             + "task.", notificationRequest.getRequestId());
                Optional<PluginConfiguration> requestStillHasRecipientScheduled = notificationRequest.getRecipientsScheduled()
                                                                                                     .stream()
                                                                                                     .filter(
                                                                                                         scheduledRecipient -> scheduledRecipient.getBusinessId()
                                                                                                                                                 .equals(
                                                                                                                                                     abortedRecipientBusinessId))
                                                                                                     .findAny();
                if (requestStillHasRecipientScheduled.isPresent()) {
                    Long abortedRecipientId = requestStillHasRecipientScheduled.get().getId();
                    // Update associated recipients to remove all scheduled recipients and add them to the list of
                    // recipients to schedule. Doing this reset the request in its initial state and set it ready for
                    // restart.
                    notificationRequestRepository.addRecipientToSchedule(notificationRequest.getId(),
                                                                         abortedRecipientId);
                    notificationRequestRepository.removeRecipientScheduled(notificationRequest.getId(),
                                                                           abortedRecipientId);
                    LOGGER.error("Request {} moved from scheduled requests for recipient {} to to_schedule requests "
                                 + "cause associated job crashed", notificationRequest.getState(), abortedRecipientId);
                    // Keep request ID to update its state
                    notificationRequestIdsToUpdate.add(notificationRequest.getId());
                } else {
                    // should not happen
                    LOGGER.warn("Job crash detected for request {} and recipient business id {}, but recipient no "
                                + "longer exists", notificationRequest.getRequestId(), abortedRecipientBusinessId);
                }
            } else {
                // Nothing to do, request is already in a final state.
                LOGGER.error("Job crash detected for request {}. Request is already on a final state {} so "
                             + "nothing is done.", notificationRequest.getRequestId(), notificationRequest.getState());
            }
        });
        // Update requests in database after modification, if not empty
        if (!notificationRequestIdsToUpdate.isEmpty()) {
            notificationRequestRepository.updateState(NotificationState.TO_SCHEDULE_BY_RECIPIENT,
                                                      notificationRequestIdsToUpdate);
        }
    }

    public Set<NotificationRequestEvent> handleRetryOrIgnoreRequests(List<? extends NotificationRequestEvent> events) {
        try {
            return self.handleRetryOrIgnoreRequestsConcurrent(events);
        } catch (ObjectOptimisticLockingFailureException e) {
            LOGGER.trace(OPTIMIST_LOCK_LOG_MSG, e);
            // we retry until it succeed because if it does not succeed on first time it is most likely because of
            // another scheduled method that would then most likely happen at next invocation because execution delays are fixed
            return handleRetryOrIgnoreRequests(events);
        }
    }

    /**
     * If event.requestId already exists in database :
     * Retry requests with one of the following condition :
     * <li>There are errors in previous notification attempt </li>
     * <li>There are rules to match remaining </li>
     * Else, remove it from batch, the request will not be processed (just logged warn)
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Set<NotificationRequestEvent> handleRetryOrIgnoreRequestsConcurrent(List<? extends NotificationRequestEvent> events) {
        Map<String, ? extends NotificationRequestEvent> eventsPerRequestId = events.stream()
                                                                                   .collect(Collectors.toMap(
                                                                                       NotificationRequestEvent::getRequestId,
                                                                                       Function.identity()));
        Set<NotificationRequest> alreadyKnownRequests = notificationRequestRepository.findAllByRequestIdIn(
            eventsPerRequestId.keySet());
        Set<NotificationRequest> grantedRequests = new HashSet<>();
        Set<NotificationRequest> toScheduleRequests = new HashSet<>();
        int nbUpdatedRequests = 0;
        List<NotifierEvent> responseToSend = new ArrayList<>();

        int nbRequestRetriedForRecipientError = 0;
        int nbRequestRetriedForRulesError = 0;

        for (NotificationRequest knownRequest : alreadyKnownRequests) {
            boolean requestToUpdate = false;
            if (!knownRequest.getRecipientsInError().isEmpty()) {
                Long knownRequestId = knownRequest.getId();
                // This is a retry, let's prepare everything so that it can be retried properly
                // transfer every recipientsInError to recipientToSchedule
                knownRequest.getRecipientsInError()
                            .forEach(r -> notificationRequestRepository.addRecipientToSchedule(knownRequestId,
                                                                                               r.getId()));
                notificationRequestRepository.removeRecipientErrors(knownRequestId,
                                                                    knownRequest.getRecipientsInError()
                                                                                .stream()
                                                                                .map(PluginConfiguration::getId)
                                                                                .collect(Collectors.toSet()));

                toScheduleRequests.add(knownRequest);
                nbRequestRetriedForRecipientError++;
                requestToUpdate = true;
            }
            // This allows to retry if a rule failed to be matched to this notification.
            // THIS HAS TO BE DONE AFTER RECIPIENTS IN ERROR!!!! Otherwise, the rules won't be applied again
            if (!knownRequest.getRulesToMatch().isEmpty()) {
                grantedRequests.add(knownRequest);
                toScheduleRequests.remove(knownRequest);
                nbRequestRetriedForRulesError++;
                requestToUpdate = true;
            }
            if (requestToUpdate) {
                nbUpdatedRequests++;
                responseToSend.add(new NotifierEvent(knownRequest.getRequestId(),
                                                     knownRequest.getRequestOwner(),
                                                     NotificationState.GRANTED,
                                                     knownRequest.getRequestDate()));
            } else {
                LOGGER.warn("Notification request with request-id {} already exists (database id {}), REGARDS skip it",
                            knownRequest.getRequestId(),
                            knownRequest.getId());
            }
            // Remove this requestId from map so that we can later reconstruct the collection of event still to be handled
            eventsPerRequestId.put(knownRequest.getRequestId(), null);
        }
        if (!toScheduleRequests.isEmpty()) {
            notificationRequestRepository.updateState(NotificationState.TO_SCHEDULE_BY_RECIPIENT,
                                                      toScheduleRequests.stream()
                                                                        .map(NotificationRequest::getId)
                                                                        .collect(Collectors.toSet()));
        }
        if (!grantedRequests.isEmpty()) {
            notificationRequestRepository.updateState(NotificationState.GRANTED,
                                                      grantedRequests.stream()
                                                                     .map(NotificationRequest::getId)
                                                                     .collect(Collectors.toSet()));
        }
        publisher.publish(responseToSend);
        LOGGER.debug(
            "Out of {} request retried, {} have been handle for retry following recipient error, {} have been handle for retry following rule matching error",
            nbUpdatedRequests,
            nbRequestRetriedForRecipientError,
            nbRequestRetriedForRulesError);
        return eventsPerRequestId.values().stream().filter(Objects::nonNull).collect(Collectors.toSet());
    }

    /**
     * Split notification list to two list : it separates duplicated events and non-duplicated events.
     * Only the second occurrence of a same notification will be in the duplicated list. The first occurrence is non-duplicated.
     * </br>
     * The key is true for duplicated list and false for non-duplicated list.
     */
    private Map<Boolean, List<? extends NotificationRequestEvent>> findDuplicatedNotifEvents(List<? extends NotificationRequestEvent> events) {
        List<String> allIds = new ArrayList<>();
        List<Integer> indexOfNotifDuplicated = new ArrayList<>();
        for (int i = 0; i < events.size(); i++) {
            NotificationRequestEvent event = events.get(i);
            String requestId = event.getRequestId();
            if (allIds.contains(requestId)) {
                indexOfNotifDuplicated.add(i);
            } else {
                allIds.add(requestId);
            }
        }
        List<? extends NotificationRequestEvent> duplicatedEvents = indexOfNotifDuplicated.stream()
                                                                                          .map(events::get)
                                                                                          .collect(Collectors.toList());
        List<? extends NotificationRequestEvent> notDuplicatedEvents = events.stream()
                                                                             .filter(ev -> !duplicatedEvents.contains(ev))
                                                                             .toList();
        Map<Boolean, List<? extends NotificationRequestEvent>> eventsGroupedByDuplicated = new HashMap<>();
        eventsGroupedByDuplicated.put(true, duplicatedEvents);
        eventsGroupedByDuplicated.put(false, notDuplicatedEvents);
        return eventsGroupedByDuplicated;
    }

    /**
     * Check if a notification request event is valid and create a notification request otherwise publish an error
     *
     * @return a implemented {@link NotificationRequest} or empty value if invalid
     */
    private Optional<NotificationRequest> initNotificationRequest(NotificationRequestEvent event, Set<Rule> rules) {
        Errors errors;
        NotificationState notificationState;
        Set<PluginConfiguration> recipientsToSchedule;
        Set<Rule> rulesForNotificationRequest;

        // Check if request will to be notified to specific recipient or need to be checked against rules
        if (event instanceof SpecificRecipientNotificationRequestEvent specificRecipientNotificationRequestEvent) {
            // Request event containing the list of recipients for a direct notification without rules check.
            notificationState = NotificationState.TO_SCHEDULE_BY_RECIPIENT;
            errors = new MapBindingResult(new HashMap<>(), SpecificRecipientNotificationRequestEvent.class.getName());
            validator.validate(event, errors);

            recipientsToSchedule = validateRecipients(specificRecipientNotificationRequestEvent.getRecipients(),
                                                      errors);
            rulesForNotificationRequest = new HashSet<>();
        } else {
            // Request event need to be checked against rules
            notificationState = NotificationState.GRANTED;
            errors = new MapBindingResult(new HashMap<>(), NotificationRequestEvent.class.getName());
            validator.validate(event, errors);

            recipientsToSchedule = new HashSet<>();
            rulesForNotificationRequest = rules;
        }
        // When no error, create notification request
        if (!errors.hasErrors()) {
            publisher.publish(new NotifierEvent(event.getRequestId(),
                                                event.getRequestOwner(),
                                                notificationState,
                                                OffsetDateTime.now()));
            // Create the notification request
            NotificationRequest notificationRequest = new NotificationRequest(event.getPayload(),
                                                                              event.getMetadata(),
                                                                              event.getRequestId(),
                                                                              event.getRequestOwner(),
                                                                              event.getRequestDate(),
                                                                              notificationState);
            notificationRequest.getRulesToMatch().addAll(rulesForNotificationRequest);
            notificationRequest.getRecipientsToSchedule().addAll(recipientsToSchedule);

            return Optional.of(notificationRequest);
        }
        // Handle error
        // Publish denied event and notification to admin
        notificationClient.notify(errors.toString(),
                                  "A NotificationRequestEvent received is invalid",
                                  NotificationLevel.ERROR,
                                  DefaultRole.ADMIN);
        publisher.publish(new NotifierEvent(event.getRequestId(),
                                            event.getRequestOwner(),
                                            NotificationState.DENIED,
                                            OffsetDateTime.now()));

        return Optional.empty();
    }

    private Set<PluginConfiguration> validateRecipients(Set<String> recipientIds, Errors errors) {
        String fieldInError = "businessId";
        Set<PluginConfiguration> pluginConfigurations = new HashSet<>();
        for (String businessId : recipientIds) {
            try {
                PluginConfiguration pluginConfiguration = pluginService.getPluginConfiguration(businessId);
                pluginConfigurations.add(pluginConfiguration);
                IRecipientNotifier recipientNotifierPlugin = pluginService.getPlugin(pluginConfiguration);
                // Check if the plugin can enable the direct notification
                if (!recipientNotifierPlugin.isDirectNotificationEnabled()) {
                    errors.rejectValue(BUSINESS_ID_FIELD,
                                       "specificRecipientNotificationRequestEvent.recipients.not.enable"
                                       + ".directnotification"
                                       + ".error.message",
                                       String.format("This plugin[id:%s] does not enable the direct notification.",
                                                     businessId));
                }
            } catch (EntityNotFoundException | NotAvailablePluginConfigurationException e) {
                errors.rejectValue(BUSINESS_ID_FIELD,
                                   "specificRecipientNotificationRequestEvent.recipients.not.available"
                                   + ".error.message",
                                   String.format("This plugin[id:%s] does not available.", businessId));
            } catch (ModuleException e) {
                errors.rejectValue(BUSINESS_ID_FIELD,
                                   "specificRecipientNotificationRequestEvent.recipients.error.message",
                                   String.format("An error occurs during the instantiating of plugin[id:%s].",
                                                 businessId));
            }
        }
        return errors.hasErrors() ? new HashSet<>() : pluginConfigurations;
    }

}
