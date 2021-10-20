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
import fr.cnes.regards.framework.utils.plugins.exception.NotAvailablePluginConfigurationException;
import fr.cnes.regards.modules.notifier.dao.INotificationRequestRepository;
import fr.cnes.regards.modules.notifier.domain.NotificationRequest;
import fr.cnes.regards.modules.notifier.domain.plugin.IRecipientNotifier;
import fr.cnes.regards.modules.notifier.dto.out.NotificationState;
import fr.cnes.regards.modules.notifier.dto.out.NotifierEvent;
import fr.cnes.regards.modules.notifier.dto.out.Recipient;
import fr.cnes.regards.modules.notifier.dto.out.RecipientStatus;
import fr.cnes.regards.modules.notifier.service.conf.NotificationConfigurationProperties;
import fr.cnes.regards.modules.notifier.service.job.NotificationJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
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
public class NotificationProcessingService extends AbstractNotificationService<NotificationProcessingService> {

    private static final Logger LOGGER = LoggerFactory.getLogger(NotificationProcessingService.class);

    private final IPluginService pluginService;
    private final NotificationConfigurationProperties properties;
    private final IJobInfoService jobInfoService;

    public NotificationProcessingService(INotificationRequestRepository notificationRequestRepository, IPublisher publisher, IPluginService pluginService,
            NotificationConfigurationProperties properties, IJobInfoService jobInfoService, ApplicationContext applicationContext
    ) {
        super(notificationRequestRepository, publisher, applicationContext);
        this.pluginService = pluginService;
        this.properties = properties;
        this.jobInfoService = jobInfoService;
    }

    public Pair<Integer, Integer> processRequests(List<NotificationRequest> notificationRequests, PluginConfiguration recipient) {
        if (recipient != null) {
            LOGGER.debug("Start processing for recipient {}", recipient.getLabel());
            // first lets check is recipient is not null, in this case it means it has been removed from all notification requests so the job simply has nothing to do
            Collection<NotificationRequest> notificationsInError = notifyRecipient(notificationRequests, recipient);
            // handle successful notification for this recipient
            Pair<Integer, Integer> result = self.handleRecipientResults(notificationRequests, recipient, notificationsInError);
            LOGGER.debug("End processing for recipient {}", recipient.getLabel());
            return result;
        }
        return Pair.of(notificationRequests.size(), 0);
    }

    private Collection<NotificationRequest> notifyRecipient(List<NotificationRequest> notificationRequests, PluginConfiguration recipientConfiguration) {
        try {
            Collection<NotificationRequest> errors = ((IRecipientNotifier) pluginService.getPlugin(recipientConfiguration.getBusinessId())).send(notificationRequests);
            return Optional.ofNullable(errors).orElse(Collections.emptySet());
        } catch (Exception e) {
            // if there is an exception, we consider none of the request could be handled,
            // either due to error from plugin configuration or the plugin implementation itself
            LOGGER.error("Error while sending notification to receiver", e);
            return notificationRequests;
        }
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public Pair<Integer, Integer> handleRecipientResults(List<NotificationRequest> notificationRequests, PluginConfiguration recipient,
            Collection<NotificationRequest> notificationsInError
    ) {
        try {
            return self.handleRecipientResultsConcurrent(notificationRequests, recipient, notificationsInError);
        } catch (ObjectOptimisticLockingFailureException e) {
            LOGGER.trace(OPTIMIST_LOCK_LOG_MSG, e);
            notificationRequests = notificationRequestRepository.findAllById(notificationRequests.stream().map(NotificationRequest::getId).collect(Collectors.toSet()));
            // we retry until it succeed because if it does not succeed on first time it is most likely because of
            // another scheduled method that would then most likely happen at next invocation because execution delays are fixed
            return handleRecipientResults(notificationRequests, recipient, notificationsInError);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Pair<Integer, Integer> handleRecipientResultsConcurrent(List<NotificationRequest> notificationRequests, PluginConfiguration recipient,
            Collection<NotificationRequest> notificationsInError
    ) {

        notificationRequests.forEach(notificationRequest -> {
            if (notificationsInError.contains(notificationRequest)) {
                notificationRequest.getRecipientsScheduled().remove(recipient);
                notificationRequest.getRecipientsInError().add(recipient);
                // because of concurrency and retries we can actually end with a process failing but that cannot be set to state ERROR
                // in order to continue handling other recipients to schedule or rules to match
                if (notificationRequest.getState() != NotificationState.TO_SCHEDULE_BY_RECIPIENT && notificationRequest.getState() != NotificationState.GRANTED) {
                    notificationRequest.setState(NotificationState.ERROR);
                }
            } else {
                notificationRequest.getSuccessRecipients().add(recipient);
                notificationRequest.getRecipientsScheduled().remove(recipient);
            }
        });

        // Save all notifications (success removal will be done later)
        notificationRequestRepository.saveAll(notificationRequests);
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
        return notificationRequestRepository.findPageByStateAndRecipientsToScheduleContaining(NotificationState.TO_SCHEDULE_BY_RECIPIENT, recipient,
                PageRequest.of(0, properties.getMaxBulkSize(), Sort.by(Order.asc(NotificationRequest.REQUEST_DATE_JPQL_NAME))));
    }

    private Set<Long> scheduleJobForOneRecipientRetryable(PluginConfiguration recipient, List<NotificationRequest> requestsToSchedule) {
        try {
            // we need to find a page of notification request that contains this recipient to be scheduled
            return self.scheduleJobForOneRecipientConcurrent(recipient, requestsToSchedule);
        } catch (ObjectOptimisticLockingFailureException e) {
            LOGGER.trace(OPTIMIST_LOCK_LOG_MSG, e);
            // we retry until it succeed because if it does not succeed on first time it is most likely because of
            // another scheduled method that would then most likely happen at next invocation because execution delays are fixed
            // Moreover, we cannot retry on the same content as it has to be reloaded from DB
            return scheduleJobForOneRecipientRetryable(
                    recipient,
                    notificationRequestRepository.findAllById(requestsToSchedule.stream().map(NotificationRequest::getId).collect(Collectors.toSet())));
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Set<Long> scheduleJobForOneRecipientConcurrent(PluginConfiguration recipient, List<NotificationRequest> requestsToSchedule) {
        Set<Long> toScheduleId = new HashSet<>();
        if (!requestsToSchedule.isEmpty()) {
            for (NotificationRequest request : requestsToSchedule) {
                request.getRecipientsToSchedule().remove(recipient);
                request.getRecipientsScheduled().add(recipient);
                // because of concurrency issues, we have to try to set this request state to SCHEDULED here and we can't do this later
                if (request.getRecipientsToSchedule().isEmpty() && (
                        // This condition is a bit more complex because it represents the case when we retry requests during schedule.
                        // Indeed, if rulesToMatch are empty it means it hasn't been retried for error while matching
                        // a rule so the retry set the state to TO_SCHEDULE.
                        // In this case, it is already handled by the "recipientToSchedule empty" condition.
                        // But if rules to match are not empty and we are in state GRANTED, it means it has just been
                        // retried so we cannot change state so the rule can be matched by matching process.
                        // if rule to match are not empty and we are not in state GRANTED, it means it has not yet been
                        // retried so we have nothing to worry about
                        request.getRulesToMatch().isEmpty() || (request.getState() != NotificationState.GRANTED))) {
                    request.setState(NotificationState.SCHEDULED);
                }
                toScheduleId.add(request.getId());
            }
            JobInfo notificationJobForRecipient = new JobInfo(
                    false,
                    0,
                    Sets.newHashSet(new JobParameter(NotificationJob.NOTIFICATION_REQUEST_IDS, toScheduleId),
                            new JobParameter(NotificationJob.RECIPIENT_BUSINESS_ID, recipient.getBusinessId())),
                    null,
                    NotificationJob.class.getName());
            jobInfoService.createAsQueued(notificationJobForRecipient);
            // save recipient to schedule remove and recipient scheduled added
            notificationRequestRepository.saveAll(requestsToSchedule);
            return toScheduleId;
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

            Page<NotificationRequest> completedRequests = findCompletedRequests();

            Map<Boolean, List<NotificationRequest>> requestsByIsSuccessful =
                    completedRequests.stream().collect(Collectors.partitioningBy(notificationRequest -> notificationRequest.getRecipientsInError().isEmpty()));
            List<NotificationRequest> successRequests = requestsByIsSuccessful.get(true);
            List<NotificationRequest> errorRequests = requestsByIsSuccessful.get(false);

            // Publish all completed requests as events
            publisher.publish(completedRequests.stream().map(this::mapRequestToEvent).collect(Collectors.toList()));

            // Delete all successful requests
            notificationRequestRepository.deleteInBatch(successRequests);

            result = Pair.of(successRequests.size(), errorRequests.size());

        } catch (ObjectOptimisticLockingFailureException e) {
            LOGGER.trace(OPTIMIST_LOCK_LOG_MSG, e);
        }

        return result;
    }

    private Page<NotificationRequest> findCompletedRequests() {
        return notificationRequestRepository.findCompletedRequests(
                NotificationState.SCHEDULED,
                PageRequest.of(0, properties.getMaxBulkSize(), Sort.by(Order.asc(NotificationRequest.REQUEST_DATE_JPQL_NAME))));
    }

    private NotifierEvent mapRequestToEvent(NotificationRequest notificationRequest) {

        NotificationState notificationState = notificationRequest.getRecipientsInError().isEmpty() ? NotificationState.SUCCESS : NotificationState.ERROR;
        NotifierEvent notifierEvent = new NotifierEvent(notificationRequest.getRequestId(), notificationRequest.getRequestOwner(), notificationState);

        Set<Recipient> successRecipients = notificationRequest.getSuccessRecipients().stream()
                .map(pluginConfiguration -> getRecipient(pluginConfiguration, RecipientStatus.SUCCESS))
                .collect(Collectors.toSet());
        Set<Recipient> errorRecipients = notificationRequest.getRecipientsInError().stream()
                .map(pluginConfiguration -> getRecipient(pluginConfiguration, RecipientStatus.ERROR))
                .collect(Collectors.toSet());

        notifierEvent.getRecipients().addAll(successRecipients);
        notifierEvent.getRecipients().addAll(errorRecipients);

        return notifierEvent;
    }

    private Recipient getRecipient(PluginConfiguration pluginConfiguration, RecipientStatus status) {
        Recipient recipient;
        try {
            IRecipientNotifier plugin = pluginService.getPlugin(pluginConfiguration.getBusinessId());
            recipient = new Recipient(plugin.getRecipientId(), status, plugin.isAckRequired());
        } catch (ModuleException | NotAvailablePluginConfigurationException e) {
            // Should never happen, hopefully, but hey, you never know - expect the unexpected to show how modern you are
            throw new RsRuntimeException(e);
        }
        return recipient;
    }

}
