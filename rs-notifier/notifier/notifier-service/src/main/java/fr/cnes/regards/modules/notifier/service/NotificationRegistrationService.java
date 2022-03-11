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
package fr.cnes.regards.modules.notifier.service;

import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.notification.NotificationLevel;
import fr.cnes.regards.framework.notification.client.INotificationClient;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.modules.notifier.dao.INotificationRequestRepository;
import fr.cnes.regards.modules.notifier.domain.NotificationRequest;
import fr.cnes.regards.modules.notifier.domain.Rule;
import fr.cnes.regards.modules.notifier.dto.in.NotificationRequestEvent;
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

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@MultitenantTransactional
@Scope(proxyMode = ScopedProxyMode.TARGET_CLASS)
public class NotificationRegistrationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(NotificationRegistrationService.class);

    protected static final String OPTIMIST_LOCK_LOG_MSG = "Another schedule has updated some of the requests handled by this method while it was running.";

    private final Validator validator;

    private final INotificationClient notificationClient;

    private final RuleCache ruleCache;

    private final INotificationRequestRepository notificationRequestRepository;

    private final IPublisher publisher;

    private final NotificationRegistrationService self;

    public NotificationRegistrationService(INotificationRequestRepository notificationRequestRepository,
            IPublisher publisher, Validator validator, INotificationClient notificationClient, RuleCache ruleCache,
            NotificationRegistrationService notificationRegistrationService) {
        this.notificationRequestRepository = notificationRequestRepository;
        this.publisher = publisher;
        this.validator = validator;
        this.notificationClient = notificationClient;
        this.ruleCache = ruleCache;
        this.self = notificationRegistrationService;
    }

    public void registerNotificationRequests(List<NotificationRequestEvent> events) {

        if (!events.isEmpty()) {

            long startTime = System.currentTimeMillis();

            // first handle retry by identifying NRE with the same requestId as one request with recipient in error
            LOGGER.debug("Starting RETRY");
            Set<NotificationRequestEvent> notRetryEvents = handleRetryRequests(events);
            LOGGER.debug("Ending RETRY in {} ms", System.currentTimeMillis() - startTime);

            // then check validity
            try {
                Set<Rule> rules = ruleCache.getRules();
                Set<NotificationRequest> notificationToRegister = notRetryEvents.stream()
                        .map(event -> initNotificationRequest(event, rules)).collect(Collectors.toSet());
                notificationToRegister.remove(null);
                notificationRequestRepository.saveAll(notificationToRegister);
                LOGGER.debug("------------->>> {} notifications registered", notificationToRegister.size());
            } catch (ExecutionException e) {
                LOGGER.error(e.getMessage(), e);
                // Rules could not be retrieved, so let deny everything.
                List<NotifierEvent> denied = notRetryEvents.stream()
                        .map(event -> new NotifierEvent(event.getRequestId(), event.getRequestOwner(), NotificationState.DENIED))
                        .collect(Collectors.toList());
                publisher.publish(denied);
            }
        }
    }

    public Set<NotificationRequestEvent> handleRetryRequests(List<NotificationRequestEvent> events) {
        try {
            return self.handleRetryRequestsConcurrent(events);
        } catch (ObjectOptimisticLockingFailureException e) {
            LOGGER.trace(OPTIMIST_LOCK_LOG_MSG, e);
            // we retry until it succeed because if it does not succeed on first time it is most likely because of
            // another scheduled method that would then most likely happen at next invocation because execution delays are fixed
            return handleRetryRequests(events);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Set<NotificationRequestEvent> handleRetryRequestsConcurrent(List<NotificationRequestEvent> events) {

        Map<String, NotificationRequestEvent> eventsPerRequestId = events.stream()
                .collect(Collectors.toMap(NotificationRequestEvent::getRequestId, Function.identity()));
        Set<NotificationRequest> alreadyKnownRequests = notificationRequestRepository.findAllByRequestIdIn(eventsPerRequestId.keySet());
        Set<NotificationRequest> updated = new HashSet<>();
        Set<NotifierEvent> responseToSend = new HashSet<>();

        int nbRequestRetriedForRecipientError = 0;
        int nbRequestRetriedForRulesError = 0;

        for (NotificationRequest known : alreadyKnownRequests) {

            if (!known.getRecipientsInError().isEmpty()) {
                // This is a retry, let's prepare everything so that it can be retried properly
                known.getRecipientsToSchedule().addAll(known.getRecipientsInError());
                known.getRecipientsInError().clear();
                known.setState(NotificationState.TO_SCHEDULE_BY_RECIPIENT);
                updated.add(known);
                responseToSend.add(new NotifierEvent(known.getRequestId(), known.getRequestOwner(), NotificationState.GRANTED));
                // Remove this requestId from map so that we can later reconstruct the collection of event still to be handled
                eventsPerRequestId.put(known.getRequestId(), null);
                nbRequestRetriedForRecipientError++;
            }
            // This allows to retry if a rule failed to be matched to this notification.
            // THIS HAS TO BE DONE AFTER RECIPIENTS IN ERROR!!!! Otherwise, the rules won't be applied again
            if (!known.getRulesToMatch().isEmpty()) {
                known.setState(NotificationState.GRANTED);
                updated.add(known);
                // This is a set so that we are not adding multiple time the same notifier event
                responseToSend.add(new NotifierEvent(known.getRequestId(), known.getRequestOwner(), NotificationState.GRANTED));
                // Remove this requestId from map so that we can later reconstruct the collection of event still to be handled
                // in worst case this is done twice, not a problem
                eventsPerRequestId.put(known.getRequestId(), null);
                nbRequestRetriedForRulesError++;
            }
        }
        publisher.publish(new ArrayList<>(responseToSend));
        notificationRequestRepository.saveAll(updated);
        LOGGER.debug(
                "Out of {} request retried, {} have been handle for retry following recipient error, {} have been handle for retry following rule matching error",
                updated.size(), nbRequestRetriedForRecipientError, nbRequestRetriedForRulesError);
        return eventsPerRequestId.values().stream().filter(Objects::nonNull).collect(Collectors.toSet());
    }

    /**
     * Check if a notification event is valid and create a request publish an error otherwise
     *
     * @return a implemented {@link NotificationRequest} or null if invalid
     */
    private NotificationRequest initNotificationRequest(NotificationRequestEvent event, Set<Rule> rules) {

        Errors errors = new MapBindingResult(new HashMap<>(), NotificationRequestEvent.class.getName());
        validator.validate(event, errors);

        if (!errors.hasErrors()) {
            publisher.publish(new NotifierEvent(event.getRequestId(), event.getRequestOwner(), NotificationState.GRANTED));
            return new NotificationRequest(event.getPayload(), event.getMetadata(), event.getRequestId(), event.getRequestOwner(),
                                           event.getRequestDate(), NotificationState.GRANTED, rules);
        }
        notificationClient.notify(errors.toString(), "A NotificationRequestEvent received is invalid", NotificationLevel.ERROR,
                                  DefaultRole.ADMIN);
        publisher.publish(new NotifierEvent(event.getRequestId(), event.getRequestOwner(), NotificationState.DENIED));
        return null;
    }

}
