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
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.framework.modules.plugins.service.PluginMetadataNotFoundRuntimeException;
import fr.cnes.regards.framework.notification.NotificationLevel;
import fr.cnes.regards.framework.notification.client.INotificationClient;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.framework.utils.plugins.PluginUtilsRuntimeException;
import fr.cnes.regards.framework.utils.plugins.exception.NotAvailablePluginConfigurationException;
import fr.cnes.regards.modules.notifier.dao.INotificationRequestRepository;
import fr.cnes.regards.modules.notifier.domain.NotificationRequest;
import fr.cnes.regards.modules.notifier.domain.Rule;
import fr.cnes.regards.modules.notifier.domain.plugin.IRuleMatcher;
import fr.cnes.regards.modules.notifier.dto.out.NotificationState;
import fr.cnes.regards.modules.notifier.dto.out.NotifierEvent;
import fr.cnes.regards.modules.notifier.service.conf.NotificationConfigurationProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Order;
import org.springframework.data.util.Pair;
import org.springframework.http.MediaType;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@MultitenantTransactional
@Scope(proxyMode = ScopedProxyMode.TARGET_CLASS)
public class NotificationMatchingService {

    protected static final String OPTIMIST_LOCK_LOG_MSG = "Another schedule has updated some of the requests handled by this method while it was running.";

    private static final Logger LOGGER = LoggerFactory.getLogger(NotificationMatchingService.class);

    private final IPluginService pluginService;

    private final NotificationConfigurationProperties properties;

    private final INotificationClient notificationClient;

    private final INotificationRequestRepository notificationRequestRepository;

    private final IPublisher publisher;

    private final NotificationMatchingService self;

    public NotificationMatchingService(INotificationRequestRepository notificationRequestRepository,
                                       IPublisher publisher,
                                       IPluginService pluginService,
                                       NotificationConfigurationProperties properties,
                                       INotificationClient notificationClient,
                                       NotificationMatchingService notificationMatchingService) {
        this.notificationRequestRepository = notificationRequestRepository;
        this.publisher = publisher;
        this.pluginService = pluginService;
        this.properties = properties;
        this.notificationClient = notificationClient;
        this.self = notificationMatchingService;
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public Pair<Integer, Integer> matchRequestNRecipient() {
        LOGGER.debug("------------------------ Starting MATCHING");
        long startTime = System.currentTimeMillis();
        List<NotificationRequest> grantedToBeMatched = notificationRequestRepository.findByState(NotificationState.GRANTED,
                                                                                                 PageRequest.of(0,
                                                                                                                properties.getMaxBulkSize(),
                                                                                                                Sort.by(
                                                                                                                    Order.asc(
                                                                                                                        NotificationRequest.REQUEST_DATE_JPQL_NAME))))
                                                                                    .getContent();
        Pair<Integer, Integer> result = matchRequestNRecipientRetryable(grantedToBeMatched);
        LOGGER.debug("------------------------ Stopping MATCHING in {} ms", System.currentTimeMillis() - startTime);
        return result;
    }

    private Pair<Integer, Integer> matchRequestNRecipientRetryable(List<NotificationRequest> toBeMatched) {
        try {
            return self.matchRequestNRecipientConcurrent(toBeMatched);
        } catch (ObjectOptimisticLockingFailureException e) {
            LOGGER.trace(OPTIMIST_LOCK_LOG_MSG, e);
            // we retry until it succeed because if it does not succeed on first time it is most likely because of
            // another scheduled method that would then most likely happen at next invocation because execution delays are fixed
            // Moreover, we cannot retry on the same content as it has to be reloaded from DB
            return matchRequestNRecipientRetryable(notificationRequestRepository.findAllById(toBeMatched.stream()
                                                                                                        .map(
                                                                                                            NotificationRequest::getId)
                                                                                                        .collect(
                                                                                                            Collectors.toSet())));
        }
    }

    /**
     * Retrieve a {@link IRuleMatcher} plugin from given map cache or from
     * {@link fr.cnes.regards.framework.modules.plugins.service.PluginService} if not in cache.
     */
    private IRuleMatcher getPlugin(String pluginConfId, Map<String, IRuleMatcher> pluginCache)
        throws ModuleException, NotAvailablePluginConfigurationException {
        IRuleMatcher ruleMatcher = pluginCache.get(pluginConfId);
        if (ruleMatcher == null) {
            ruleMatcher = pluginService.getPlugin(pluginConfId);
            pluginCache.put(pluginConfId, ruleMatcher);
        }
        return ruleMatcher;
    }

    private record RuleMatchingResult //NOSONAR bug with sonar and record -> it's considered as empty method.
        (boolean match,
         boolean error) {

    }

    /**
     * Check if the given request match the given rule for notification.
     * Result object indicates if the rule match and if an error occurred during match process.
     */
    private RuleMatchingResult isRuleMatching(Rule rule,
                                              NotificationRequest notificationRequest,
                                              Map<String, IRuleMatcher> pluginCache) {
        boolean ruleMatched = false;
        boolean error = false;

        try {
            IRuleMatcher rulePlugin = getPlugin(rule.getRulePlugin().getBusinessId(), pluginCache);
            // check if the  element match with the rule
            ruleMatched = rulePlugin.match(notificationRequest.getMetadata(), notificationRequest.getPayload());
        } catch (ModuleException | PluginMetadataNotFoundRuntimeException | PluginUtilsRuntimeException e) {
            // exception from rule plugin instantiation
            LOGGER.error(String.format("Error while get plugin with id %S", rule.getRulePlugin().getBusinessId()), e);
            // we do not set notification request in error so that we can later handle recipients that could be matched
            // moreover, we do not stop the matching process as we want to process recipients as soon as possible
            // the only drawback is that it is possible to process one recipient twice in case multiple rules
            // associate the same recipient to one request and at least one of those rules could not be instantiated
            error = true;
        } catch (Exception e) {
            LOGGER.error("Rule could not be matched because of unexpected issue: " + e.getMessage(), e);
            error = true;
        }
        return new RuleMatchingResult(ruleMatched, error);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Pair<Integer, Integer> matchRequestNRecipientConcurrent(List<NotificationRequest> toBeMatched) {

        long firstStart = System.currentTimeMillis();
        LOGGER.debug("[MATCHING] Start ...");

        Set<PluginConfiguration> recipientsActuallyMatched = new HashSet<>();
        Set<NotificationRequest> requestsActuallyMatched = new HashSet<>();
        Set<NotificationRequest> requestsCouldNotBeMatched = new HashSet<>();
        // iterate over notification request that now know which rules are to be matched
        // (association of pattern strategy(rules) and command(notification requests know what to apply))
        Set<PluginConfiguration> cannotBeInstantiatedRules = new HashSet<>();

        Map<String, IRuleMatcher> pluginCache = new HashMap<>();

        Set<NotificationRequest> fullyHandledRequests = new HashSet<>();
        Set<NotificationRequest> requestsToSchedule = new HashSet<>();

        for (NotificationRequest notificationRequest : toBeMatched) {
            Set<Rule> couldBeMatched = new HashSet<>();
            boolean ruleMatchingError = false;
            int nbRecipientsSchedule = 0;
            Set<PluginConfiguration> matchedRecipients = new HashSet<>();

            for (Rule rule : notificationRequest.getRulesToMatch()) {
                RuleMatchingResult result = isRuleMatching(rule, notificationRequest, pluginCache);
                if (result.match) {
                    requestsActuallyMatched.add(notificationRequest);
                    // If at least one rule match, add all recipients associated to the rule to the list of
                    // recipients to schedule.
                    if (!rule.getRecipients().isEmpty()) {
                        matchedRecipients.addAll(rule.getRecipients());
                        requestsToSchedule.add(notificationRequest);
                    }
                }
                // Check if an error occurs during rule matching
                if (result.error) {
                    requestsCouldNotBeMatched.add(notificationRequest);
                    cannotBeInstantiatedRules.add(rule.getRulePlugin());
                    ruleMatchingError = true;
                } else {
                    couldBeMatched.add(rule);
                    recipientsActuallyMatched.addAll(rule.getRecipients());
                }
            }

            // Add all recipients id to schedule for the current request.
            matchedRecipients.forEach(recipient -> {
                notificationRequestRepository.addRecipientToSchedule(notificationRequest.getId(), recipient.getId());
            });
            // If ruleMatchingError occurs, only delete rules matching succeed ones. Keep errors in rules to match for next launch.
            if (ruleMatchingError) {
                List<Long> ruleIdsToRemove = couldBeMatched.stream().map(Rule::getId).toList();
                if (!ruleIdsToRemove.isEmpty()) {
                    notificationRequestRepository.removeRulesToMatch(notificationRequest.getId(), ruleIdsToRemove);
                }
            } else {
                // Else, only add request to the list of success ended request to perform delete in one request after.
                fullyHandledRequests.add(notificationRequest);
            }

            // notificationRequest.getRulesToMatch().removeAll(couldBeMatched);
            LOGGER.debug("[MATCHING] Notification {} is to send to {} recipients",
                         notificationRequest.getRequestId(),
                         nbRecipientsSchedule);
        }

        LOGGER.debug("[MATCHING] Calculation done in {}ms", System.currentTimeMillis() - firstStart);
        long start = System.currentTimeMillis();
        // None of the notification requests have been set in state error
        // But there is indeed an issue that can only be resolved later (thanks to human interaction) so we need to say
        // the request has been in error so callers can handle it and ask for retry later.
        publisher.publish(requestsCouldNotBeMatched.stream()
                                                   .map(request -> new NotifierEvent(request.getRequestId(),
                                                                                     request.getRequestOwner(),
                                                                                     NotificationState.ERROR,
                                                                                     request.getRequestDate()))
                                                   .collect(Collectors.toList()));
        if (!cannotBeInstantiatedRules.isEmpty()) {
            String message = cannotBeInstantiatedRules.stream()
                                                      .map(couldNotBeInstantiated -> String.format(
                                                          "%s plugin [businessId=%s label=%s] could not be "
                                                          + "instantiated so notifier "
                                                          + "cannot fully handle any requests for now.",
                                                          couldNotBeInstantiated.getPluginId(),
                                                          couldNotBeInstantiated.getBusinessId(),
                                                          couldNotBeInstantiated.getLabel()))
                                                      .collect(Collectors.joining("<br>", "<p>", "</p>"));
            notificationClient.notify(message,
                                      String.format("Some %s plugins could not be instantiated",
                                                    IRuleMatcher.class.getSimpleName()),
                                      NotificationLevel.FATAL,
                                      MediaType.TEXT_HTML,
                                      DefaultRole.ADMIN);
        }
        LOGGER.debug("[MATCHING] Notification done in {}ms", System.currentTimeMillis() - start);

        Set<Long> requestsIdsFullyHandled = fullyHandledRequests.stream()
                                                                .map(NotificationRequest::getId)
                                                                .collect(Collectors.toSet());
        Set<Long> requestIdsToSchedule = requestsToSchedule.stream()
                                                           .map(NotificationRequest::getId)
                                                           .collect(Collectors.toSet());
        if (!fullyHandledRequests.isEmpty()) {
            // For each request to fully handled (no error), delete all rules to match associated. Match is done.
            notificationRequestRepository.removeRulesToMatch(requestsIdsFullyHandled);
            // Remove all to scheduled request to keep only finished and not to schedule requests.
            requestsIdsFullyHandled.removeAll(requestIdsToSchedule);
        }

        if (!requestsIdsFullyHandled.isEmpty()) {
            // For each request handled but not to schedule set state to SCHEDULED. The request will be next processed
            // by the check completed requests scheduler.
            notificationRequestRepository.updateState(NotificationState.SCHEDULED, requestsIdsFullyHandled);
        }

        if (!requestIdsToSchedule.isEmpty()) {
            // For each request to schedule, update state to TO_SCHEDULE_BY_RECIPIENT
            notificationRequestRepository.updateState(NotificationState.TO_SCHEDULE_BY_RECIPIENT, requestIdsToSchedule);
        }

        LOGGER.debug("[MATCHING] done in {}ms", System.currentTimeMillis() - firstStart);
        return Pair.of(requestsActuallyMatched.size(), recipientsActuallyMatched.size());
    }

}
