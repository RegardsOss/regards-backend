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

import com.google.common.collect.Sets;
import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.framework.modules.jobs.service.IJobInfoService;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.framework.utils.RsRuntimeException;
import fr.cnes.regards.modules.notifier.dao.INotificationRequestRepository;
import fr.cnes.regards.modules.notifier.domain.NotificationRequest;
import fr.cnes.regards.modules.notifier.domain.plugin.IRecipientNotifier;
import fr.cnes.regards.modules.notifier.dto.conf.RecipientPluginConf;
import fr.cnes.regards.modules.notifier.dto.out.NotificationState;
import fr.cnes.regards.modules.notifier.dto.out.NotifierEvent;
import fr.cnes.regards.modules.notifier.dto.out.Recipient;
import fr.cnes.regards.modules.notifier.dto.out.RecipientStatus;
import fr.cnes.regards.modules.notifier.service.conf.NotificationConfigurationProperties;
import fr.cnes.regards.modules.notifier.service.job.NotificationJob;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Order;
import org.springframework.data.util.Pair;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@MultitenantTransactional
@Scope(proxyMode = ScopedProxyMode.TARGET_CLASS)
public class NotificationProcessingService {

    public static final NotificationState[] RUNNING_STATES = { NotificationState.SCHEDULED };

    protected static final String OPTIMIST_LOCK_LOG_MSG = "Another schedule has updated some of the requests handled by this method while it was running.";

    private static final Logger LOGGER = LoggerFactory.getLogger(NotificationProcessingService.class);

    private final IPluginService pluginService;

    private final NotificationConfigurationProperties properties;

    private final IJobInfoService jobInfoService;

    private final INotificationRequestRepository notificationRequestRepository;

    private final IPublisher publisher;

    private final NotificationProcessingService self;

    public NotificationProcessingService(INotificationRequestRepository notificationRequestRepository,
                                         IPublisher publisher,
                                         IPluginService pluginService,
                                         NotificationConfigurationProperties properties,
                                         IJobInfoService jobInfoService,
                                         NotificationProcessingService notificationProcessingService) {
        this.notificationRequestRepository = notificationRequestRepository;
        this.publisher = publisher;
        this.pluginService = pluginService;
        this.properties = properties;
        this.jobInfoService = jobInfoService;
        this.self = notificationProcessingService;
    }

    public Pair<Integer, Integer> processRequests(List<NotificationRequest> notificationRequests,
                                                  PluginConfiguration recipient) {
        // If recipient is null, it means it has been removed from all notification requests - therefore there's nothing to do
        if (recipient != null) {
            long startTime = System.currentTimeMillis();
            LOGGER.debug("Start processing for recipient {}", recipient.getLabel());
            Collection<NotificationRequest> notificationsInError = notifyRecipient(notificationRequests, recipient);
            Pair<Integer, Integer> result = self.handleRecipientResults(notificationRequests,
                                                                        recipient,
                                                                        notificationsInError);
            LOGGER.debug("End processing for recipient {} done in {}ms",
                         recipient.getLabel(),
                         System.currentTimeMillis() - startTime);
            return result;
        }
        return Pair.of(notificationRequests.size(), 0);
    }

    private Collection<NotificationRequest> notifyRecipient(List<NotificationRequest> notificationRequests,
                                                            PluginConfiguration recipientConfiguration) {
        try {
            Collection<NotificationRequest> errors = ((IRecipientNotifier) pluginService.getPlugin(
                recipientConfiguration.getBusinessId())).send(notificationRequests);
            return Optional.ofNullable(errors).orElse(Collections.emptySet());
        } catch (Exception e) {
            // If there is an exception, we consider none of the requests could be handled,
            // either due to error from plugin configuration or the plugin implementation itself
            LOGGER.error("Error while sending notification to receiver", e);
            return notificationRequests;
        }
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public Pair<Integer, Integer> handleRecipientResults(List<NotificationRequest> notificationRequests,
                                                         PluginConfiguration recipient,
                                                         Collection<NotificationRequest> notificationsInError) {
        try {
            return self.handleRecipientResultsConcurrent(notificationRequests, recipient, notificationsInError);
        } catch (ObjectOptimisticLockingFailureException e) {
            LOGGER.trace(OPTIMIST_LOCK_LOG_MSG, e);
            notificationRequests = notificationRequestRepository.findAllById(notificationRequests.stream()
                                                                                                 .map(
                                                                                                     NotificationRequest::getId)
                                                                                                 .collect(Collectors.toSet()));
            // Failure happens because of concurrent updates on notification request - for that reason we retry until it succeeds
            return handleRecipientResults(notificationRequests, recipient, notificationsInError);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Pair<Integer, Integer> handleRecipientResultsConcurrent(List<NotificationRequest> notificationRequests,
                                                                   PluginConfiguration recipient,
                                                                   Collection<NotificationRequest> notificationsInError) {
        notificationRequests.forEach(notificationRequest -> {
            if (notificationsInError.contains(notificationRequest)) {
                notificationRequestRepository.addRecipientInError(notificationRequest.getId(), recipient.getId());
            } else {
                notificationRequestRepository.addRecipientInSuccess(notificationRequest.getId(), recipient.getId());
            }
        });

        notificationRequestRepository.removeRecipientsScheduledForRequestIds(notificationRequests.stream()
                                                                                                 .map(
                                                                                                     NotificationRequest::getId)
                                                                                                 .collect(Collectors.toSet()),
                                                                             recipient.getId());
        return Pair.of(notificationRequests.size() - notificationsInError.size(), notificationsInError.size());
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public Set<Long> scheduleJobForOneRecipient(PluginConfiguration recipient) {
        LOGGER.debug("Starting SCHEDULING FOR {}", recipient.getLabel());
        long startTime = System.currentTimeMillis();
        Page<NotificationRequest> requestsToSchedule = findPageToScheduleContaining(recipient);
        Set<Long> result = scheduleJobForOneRecipientRetryable(recipient, requestsToSchedule.getContent());
        LOGGER.debug("Ending SCHEDULING FOR {} in {} ms", recipient.getLabel(), System.currentTimeMillis() - startTime);
        return result;
    }

    private Page<NotificationRequest> findPageToScheduleContaining(PluginConfiguration recipient) {
        return notificationRequestRepository.findPageByStateAndRecipientsToScheduleContaining(NotificationState.TO_SCHEDULE_BY_RECIPIENT,
                                                                                              recipient,
                                                                                              PageRequest.of(0,
                                                                                                             properties.getMaxBulkSize(),
                                                                                                             Sort.by(
                                                                                                                 Order.asc(
                                                                                                                     NotificationRequest.REQUEST_DATE_JPQL_NAME))));
    }

    private Set<Long> scheduleJobForOneRecipientRetryable(PluginConfiguration recipient,
                                                          List<NotificationRequest> requestsToSchedule) {
        try {
            return self.scheduleJobForOneRecipientConcurrent(recipient, requestsToSchedule);
        } catch (ObjectOptimisticLockingFailureException e) {
            LOGGER.trace(OPTIMIST_LOCK_LOG_MSG, e);
            // Failure happens because of concurrent updates on notification request - for that reason we retry until it succeeds
            // Moreover, we cannot retry on the same content as it has to be reloaded from DB
            return scheduleJobForOneRecipientRetryable(recipient,
                                                       notificationRequestRepository.findAllById(requestsToSchedule.stream()
                                                                                                                   .map(
                                                                                                                       NotificationRequest::getId)
                                                                                                                   .collect(
                                                                                                                       Collectors.toSet())));
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Set<Long> scheduleJobForOneRecipientConcurrent(PluginConfiguration recipient,
                                                          List<NotificationRequest> requestsToSchedule) {
        Set<Long> requestIdsToSchedule = new HashSet<>();
        if (!requestsToSchedule.isEmpty()) {
            Set<Long> scheduledRequestIds = new HashSet<>();
            for (NotificationRequest request : requestsToSchedule) {
                requestIdsToSchedule.add(request.getId());
                notificationRequestRepository.addRecipientScheduled(request.getId(), recipient.getId());
                // If recipient to remove is the last one we can change request status to fully scheduled.
                // A request is in SCHEDULED status only when all rules have been checked (no more entries in rules to
                // match)
                // Nevertheless, if request state is back to GRANTED status that means than a retry of errors has
                // been done for this request. So request should remains at GRANTED status to allow new rule matching
                // process.
                boolean isLastRecipient = request.getRecipientsToSchedule().size() == 1
                                          && request.getRecipientsToSchedule().contains(recipient);
                if (isLastRecipient && (request.getRulesToMatch().isEmpty() || (request.getState()
                                                                                != NotificationState.GRANTED))) {
                    scheduledRequestIds.add(request.getId());
                }
            }

            notificationRequestRepository.removeRecipientToScheduleForRequestIds(requestIdsToSchedule,
                                                                                 recipient.getId());
            if (!scheduledRequestIds.isEmpty()) {
                notificationRequestRepository.updateState(NotificationState.SCHEDULED, scheduledRequestIds);
            }

            JobInfo notificationJobForRecipient = new JobInfo(false,
                                                              0,
                                                              Sets.newHashSet(new JobParameter(NotificationJob.NOTIFICATION_REQUEST_IDS,
                                                                                               requestIdsToSchedule),
                                                                              new JobParameter(NotificationJob.RECIPIENT_BUSINESS_ID,
                                                                                               recipient.getBusinessId())),
                                                              null,
                                                              NotificationJob.class.getName());
            jobInfoService.createAsQueued(notificationJobForRecipient);
            return requestIdsToSchedule;
        }
        return new HashSet<>();
    }

    /**
     * Retrieves all completed notification requests, and sends the appropriate NotifierEvent for each of them
     * <br>
     * Returns the number of requests that ended with success and error.
     *
     * @return A pair of integer, comprised of (left) number of requests in success and (right) number of requests in error
     */
    public Pair<Integer, Integer> checkCompletedRequests() {
        Pair<Integer, Integer> result = Pair.of(0, 0);
        try {

            List<NotificationRequest> completedRequests = findCompletedRequests();

            Map<Boolean, List<NotificationRequest>> requestsByIsSuccessful = completedRequests.stream()
                                                                                              .collect(Collectors.partitioningBy(
                                                                                                  notificationRequest -> notificationRequest.getRecipientsInError()
                                                                                                                                            .isEmpty()));
            List<NotificationRequest> successRequests = requestsByIsSuccessful.get(true);
            List<NotificationRequest> errorRequests = requestsByIsSuccessful.get(false);

            // Publish all completed requests as events
            Map<String, RecipientPluginConf> recipientsInfoCache = new HashMap<>();
            List<NotifierEvent> events = completedRequests.stream()
                                                          .map(request -> this.mapRequestToEvent(request,
                                                                                                 recipientsInfoCache))
                                                          .toList();
            if (!events.isEmpty()) {
                publisher.publish(events);
            }

            // Delete all successful requests
            notificationRequestRepository.deleteByIdIn(successRequests.stream()
                                                                      .map(NotificationRequest::getId)
                                                                      .toList());
            // Update state to ERROR for all completed request in error
            if (!errorRequests.isEmpty()) {
                notificationRequestRepository.updateState(NotificationState.ERROR,
                                                          errorRequests.stream()
                                                                       .map(NotificationRequest::getId)
                                                                       .collect(Collectors.toSet()));
            }

            result = Pair.of(successRequests.size(), errorRequests.size());

        } catch (ObjectOptimisticLockingFailureException e) {
            LOGGER.trace(OPTIMIST_LOCK_LOG_MSG, e);
        }

        return result;
    }

    private List<NotificationRequest> findCompletedRequests() {
        return notificationRequestRepository.findCompletedRequests(properties.getMaxBulkSize());
    }

    private NotifierEvent mapRequestToEvent(NotificationRequest notificationRequest,
                                            Map<String, RecipientPluginConf> recipientsInfoCache) {

        NotificationState notificationState = notificationRequest.getRecipientsInError().isEmpty() ?
            NotificationState.SUCCESS :
            NotificationState.ERROR;
        NotifierEvent notifierEvent = new NotifierEvent(notificationRequest.getRequestId(),
                                                        notificationRequest.getRequestOwner(),
                                                        notificationState,
                                                        notificationRequest.getRequestDate());

        Set<Recipient> successRecipients = notificationRequest.getSuccessRecipients()
                                                              .stream()
                                                              .map(pluginConfiguration -> getRecipient(
                                                                  pluginConfiguration,
                                                                  RecipientStatus.SUCCESS,
                                                                  recipientsInfoCache))
                                                              .filter(Objects::nonNull)
                                                              .collect(Collectors.toSet());
        Set<Recipient> errorRecipients = notificationRequest.getRecipientsInError()
                                                            .stream()
                                                            .map(pluginConfiguration -> getRecipient(pluginConfiguration,
                                                                                                     RecipientStatus.ERROR,
                                                                                                     recipientsInfoCache))
                                                            .filter(Objects::nonNull)
                                                            .collect(Collectors.toSet());

        notifierEvent.getRecipients().addAll(successRecipients);
        notifierEvent.getRecipients().addAll(errorRecipients);

        return notifierEvent;
    }

    private Recipient getRecipient(PluginConfiguration pluginConfiguration,
                                   RecipientStatus status,
                                   Map<String, RecipientPluginConf> recipientInfosCache) {
        Recipient recipient = null;
        RecipientPluginConf recipientConf = recipientInfosCache.get(pluginConfiguration.getBusinessId());

        try {
            if (recipientConf == null) {
                IRecipientNotifier plugin = pluginService.getPlugin(pluginConfiguration.getBusinessId());
                recipientConf = new RecipientPluginConf(plugin.getRecipientLabel(),
                                                        plugin.isAckRequired(),
                                                        plugin.isBlockingRequired());
                recipientInfosCache.put(pluginConfiguration.getBusinessId(), recipientConf);
            }
            // Only build a recipient if recipientLabel has been set
            if (StringUtils.isNotBlank(recipientConf.recipientLabel())) {
                recipient = new Recipient(recipientConf.recipientLabel(),
                                          status,
                                          recipientConf.ackRequired(),
                                          recipientConf.blocking());
            }
        } catch (IllegalArgumentException e) {
            LOGGER.error(e.getMessage(), e);
            return null;
        } catch (ModuleException e) {
            LOGGER.error(e.getMessage(), e);
            // Should never happen, hopefully, but hey, you never know - expect the unexpected to show how modern you are
            throw new RsRuntimeException(e);
        }
        return recipient;
    }

}
