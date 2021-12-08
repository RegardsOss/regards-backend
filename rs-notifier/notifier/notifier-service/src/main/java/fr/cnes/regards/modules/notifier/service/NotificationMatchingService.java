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
import org.springframework.context.ApplicationContext;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Order;
import org.springframework.data.util.Pair;
import org.springframework.http.MediaType;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@MultitenantTransactional
public class NotificationMatchingService extends AbstractNotificationService<NotificationMatchingService> {

    private static final Logger LOGGER = LoggerFactory.getLogger(NotificationMatchingService.class);

    private final IPluginService pluginService;
    private final NotificationConfigurationProperties properties;
    private final INotificationClient notificationClient;

    public NotificationMatchingService(INotificationRequestRepository notificationRequestRepository, IPublisher publisher, IPluginService pluginService,
            NotificationConfigurationProperties properties, INotificationClient notificationClient, ApplicationContext applicationContext
    ) {
        super(notificationRequestRepository, publisher, applicationContext);
        this.pluginService = pluginService;
        this.properties = properties;
        this.notificationClient = notificationClient;
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public Pair<Integer, Integer> matchRequestNRecipient() {
        LOGGER.debug("------------------------ Starting MATCHING");
        long startTime = System.currentTimeMillis();
        List<NotificationRequest> grantedToBeMatched = notificationRequestRepository
                .findByState(NotificationState.GRANTED, PageRequest.of(0, properties.getMaxBulkSize(), Sort.by(Order.asc(NotificationRequest.REQUEST_DATE_JPQL_NAME))))
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
            return matchRequestNRecipientRetryable(notificationRequestRepository.findAllById(toBeMatched.stream().map(NotificationRequest::getId).collect(Collectors.toSet())));
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Pair<Integer, Integer> matchRequestNRecipientConcurrent(List<NotificationRequest> toBeMatched) {

        Set<PluginConfiguration> recipientsActuallyMatched = new HashSet<>();
        Set<NotificationRequest> requestsActuallyMatched = new HashSet<>();
        Set<NotificationRequest> requestsCouldNotBeMatched = new HashSet<>();
        // iterate over notification request that now know which rules are to be matched
        // (association of pattern strategy(rules) and command(notification requests know what to apply))
        Set<PluginConfiguration> cannotBeInstantiatedRules = new HashSet<>();

        for (NotificationRequest notificationRequest : toBeMatched) {
            Set<Rule> couldBeMatched = new HashSet<>();
            for (Rule rule : notificationRequest.getRulesToMatch()) {
                try {
                    IRuleMatcher rulePlugin = pluginService.getPlugin(rule.getRulePlugin().getBusinessId());
                    // check if the  element match with the rule
                    if (rulePlugin.match(notificationRequest.getMetadata(), notificationRequest.getPayload())) {
                        for (PluginConfiguration recipient : rule.getRecipients()) {
                            notificationRequest.getRecipientsToSchedule().add(recipient);
                            // this is done so that we can know how many recipients have been matched by at least one request
                            recipientsActuallyMatched.add(recipient);
                        }
                        requestsActuallyMatched.add(notificationRequest);
                    }
                    notificationRequest.setState(NotificationState.TO_SCHEDULE_BY_RECIPIENT);
                    couldBeMatched.add(rule);
                } catch (ModuleException | NotAvailablePluginConfigurationException | PluginMetadataNotFoundRuntimeException | PluginUtilsRuntimeException e) {
                    // exception from rule plugin instantiation
                    LOGGER.error(String.format("Error while get plugin with id %S", rule.getRulePlugin().getBusinessId()), e);
                    // we do not set notification request in error so that we can later handle recipients that could be matched
                    // moreover, we do not stop the matching process as we want to process recipients as soon as possible
                    // the only drawback is that it is possible to process one recipient twice in case multiple rules
                    // associate the same recipient to one request and at least one of those rules could not be instantiated
                    cannotBeInstantiatedRules.add(rule.getRulePlugin());
                    requestsCouldNotBeMatched.add(notificationRequest);
                } catch (Exception e) {
                    LOGGER.error("Rule could not be matched because of unexpected issue: " + e.getMessage(), e);
                    requestsCouldNotBeMatched.add(notificationRequest);
                }
            }
            // we remove all rules that could be matched now to avoid playing with iterators
            notificationRequest.getRulesToMatch().removeAll(couldBeMatched);
        }
        // None of the notification requests have been set in state error
        // But there is indeed an issue that can only be resolved later (thanks to human interaction) so we need to say
        // the request has been in error so callers can handle it and ask for retry later.
        publisher.publish(
                requestsCouldNotBeMatched.stream()
                        .map(request -> new NotifierEvent(request.getRequestId(), request.getRequestOwner(), NotificationState.ERROR))
                        .collect(Collectors.toList()));
        if (!cannotBeInstantiatedRules.isEmpty()) {
            String message = cannotBeInstantiatedRules.stream()
                    .map(couldNotBeInstantiated -> String.format("%s plugin with id %s could not be instantiated so notifier cannot fully handle any requests for now.",
                            couldNotBeInstantiated.getPluginClassName(), couldNotBeInstantiated.getBusinessId()))
                    .collect(Collectors.joining("<br>", "<p>", "</p>"));
            notificationClient.notify(message, String.format("Some %s plugins could not be instantiated", IRuleMatcher.class.getSimpleName()),
                    NotificationLevel.FATAL, MediaType.TEXT_HTML, DefaultRole.ADMIN);
        }
        // do not forget to handle all requests that were not matched by any rule and so should be considered successful
        // right now (for simplicity issue lets set its state to SCHEDULED and wait for the check to be done)
        Predicate<NotificationRequest> isSchedulable =
                r -> Stream.of(
                                !requestsCouldNotBeMatched.contains(r),
                                !requestsActuallyMatched.contains(r),
                                // because of retry logic in case of previous error in the matching process,
                                // we have to check that nothing is to be done (already planned)
                                // This case can happen if the rule that could not be matched earlier does not match the request
                                r.getRecipientsToSchedule().isEmpty(),
                                r.getRecipientsInError().isEmpty(),
                                r.getRecipientsScheduled().isEmpty(),
                                r.getRulesToMatch().isEmpty())
                        .allMatch(b -> b);

        toBeMatched.stream().filter(isSchedulable).forEach(request -> request.setState(NotificationState.SCHEDULED));

        notificationRequestRepository.saveAll(toBeMatched);
        return Pair.of(requestsActuallyMatched.size(), recipientsActuallyMatched.size());
    }

}
