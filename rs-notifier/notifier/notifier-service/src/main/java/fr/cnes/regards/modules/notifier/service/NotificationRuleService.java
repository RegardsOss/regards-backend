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
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Order;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import org.springframework.validation.Errors;
import org.springframework.validation.MapBindingResult;
import org.springframework.validation.Validator;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Sets;
import com.google.gson.JsonElement;
import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.authentication.IAuthenticationResolver;
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
import fr.cnes.regards.modules.notifier.dao.INotificationActionRepository;
import fr.cnes.regards.modules.notifier.dao.IRecipientErrorRepository;
import fr.cnes.regards.modules.notifier.domain.NotifRequestId;
import fr.cnes.regards.modules.notifier.domain.NotificationRequest;
import fr.cnes.regards.modules.notifier.domain.RecipientError;
import fr.cnes.regards.modules.notifier.domain.Rule;
import fr.cnes.regards.modules.notifier.domain.plugin.IRecipientNotifier;
import fr.cnes.regards.modules.notifier.domain.plugin.IRuleMatcher;
import fr.cnes.regards.modules.notifier.dto.in.NotificationActionEvent;
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
    private INotificationActionRepository notificationActionRepo;

    @Autowired
    private IRecipientErrorRepository recipientErrorRepo;

    @Autowired
    private Validator validator;

    @Autowired
    private NotificationConfigurationProperties properties;

    @Autowired
    private IAuthenticationResolver authResolver;

    @Autowired
    private IJobInfoService jobInfoService;

    @Autowired
    private INotificationClient notificationClient;

    @Autowired
    private IRuntimeTenantResolver tenantResolver;

    @Autowired
    private IPublisher publisher;

    /**
     * Handle a {@link NotificationRequest} and check if it matches with enabled {@link Rule} in that case
     * send a notification to Recipient
     * @param notification {@link NotificationRequest} to handle
     * @param notificationsInErrors list of {@link NotificationRequest} where a Recipient fail
     * @return number of notification sended
     * @throws ExecutionException in case issue occurs with rules cache
     */
    private int handleNotificationRequest(NotificationRequest notification,
            ListMultimap<NotificationRequest, PluginConfiguration> notificationsInErrors) throws ExecutionException {
        int notificationNumber = 0;
        for (Rule rule : getRules()) {
            try {
                // check if the  element match with the rule
                if (((IRuleMatcher) this.pluginService.getPlugin(rule.getRulePlugin().getBusinessId()))
                        .match(notification.getPayload())) {

                    for (PluginConfiguration recipient : rule.getRecipients()) {
                        if (notifyRecipient(notification, recipient)) {
                            publisher
                                    .publish(new NotifierEvent(notification.getRequestId(), NotificationState.SUCCESS));
                            notificationNumber++;
                        } else {
                            publisher.publish(new NotifierEvent(notification.getRequestId(), NotificationState.ERROR));
                            notification.setState(NotificationState.ERROR);
                            notificationsInErrors.put(notification, recipient);
                        }
                    }
                }
            } catch (ModuleException | NotAvailablePluginConfigurationException e) {
                // exception from rule plugin instantiation
                LOGGER.error(String.format("Error while get plugin with id %S", rule.getRulePlugin().getId()), e);
                publisher.publish(new NotifierEvent(notification.getRequestId(), NotificationState.ERROR));
            }
        }
        return notificationNumber;
    }

    /**
     * Notify a recipient return false if a problem occurs
     * @param notification {@link JsonElement} about to notify
     * @param recipientConfiguration Recipient of the notification
     * @return whether notification could be sent
     */
    private boolean notifyRecipient(NotificationRequest notification, PluginConfiguration recipientConfiguration) {
        try {
            // check that all send method of recipient return true
            return ((IRecipientNotifier) this.pluginService.getPlugin(recipientConfiguration.getBusinessId()))
                    .send(notification);
        } catch (Exception e) {
            LOGGER.error("Error while sending notification to receiver", e);
            return false;
        }
    }

    @Override
    public Pair<Integer, Integer> processRequest(List<NotificationRequest> toHandles, UUID jobInfoId) {
        long startTime = System.currentTimeMillis();
        LOGGER.debug("------------->>> Reception of {} notification  event, start of notification process {} ms",
                     toHandles.size(),
                     startTime);
        int nbSend = 0;
        int nbError;
        long averageNotificationTreatmentTime = 0;
        long startNotificationTreatmentTime;

        //get RecipientRrror for jobInfoId if exists
        List<RecipientError> recipientErrors = this.recipientErrorRepo.findByJobId(jobInfoId);

        // if empty we send notifications according rules
        if (recipientErrors.isEmpty()) {
            ListMultimap<NotificationRequest, PluginConfiguration> notificationsInErrors = ArrayListMultimap.create();

            for (NotificationRequest notification : toHandles) {
                startNotificationTreatmentTime = System.currentTimeMillis();
                try {
                    nbSend += handleNotificationRequest(notification, notificationsInErrors);
                } catch (ExecutionException e) {
                    LOGGER.error("Error during notification", e);
                }
                averageNotificationTreatmentTime += System.currentTimeMillis() - startNotificationTreatmentTime;
            }
            LOGGER.debug("------------->>> End of notification process in {} ms, {} notifications sended"
                                 + " with a average treatment time of {} ms",
                         System.currentTimeMillis() - startTime,
                         nbSend,
                         averageNotificationTreatmentTime / (nbSend == 0 ? 1 : nbSend));
            // delete all Notification not in the list in errors
            toHandles.removeAll(notificationsInErrors.keySet());
            this.notificationActionRepo.saveAll(notificationsInErrors.keySet());
            // save notification in error for resend them later
            saveRecipientErrors(notificationsInErrors, this.jobInfoService.retrieveJob(jobInfoId));
            nbError = notificationsInErrors.values().size();
            this.notificationActionRepo.deleteAll(toHandles);

        } else { //if not empty we resend notification only for failed recipient
            nbSend = resendNotification(recipientErrors);
            nbError = recipientErrors.size() - nbSend;
        }
        return Pair.of(nbSend, nbError);
    }

    /**
     * Try to resend failed Recipient
     * @param recipientErrors list of previous notification failed
     * @return the number of notification sended
     */
    private int resendNotification(List<RecipientError> recipientErrors) {
        int nbSend = 0;
        List<RecipientError> succededRecipient = new ArrayList<>();
        for (RecipientError error : recipientErrors) {

            if (notifyRecipient(error.getNotification(), error.getRecipient())) {
                succededRecipient.add(error);
                nbSend++;
            }
        }
        this.recipientErrorRepo.deleteAll(succededRecipient);
        // delete notification it have no errors left
        this.notificationActionRepo.deleteNoticationWithoutErrors();
        return nbSend;
    }

    /**
     * Create and save {@link RecipientError} from Recipient
     * @param notificationsInErrors Map of failed {@link NotificationRequest}=>Recipient
     * @param jobInfo current {@link JobInfo}
     */
    private void saveRecipientErrors(ListMultimap<NotificationRequest, PluginConfiguration> notificationsInErrors,
            JobInfo jobInfo) {
        this.recipientErrorRepo.saveAll(notificationsInErrors.entries().stream().map(entry -> RecipientError
                .build(entry.getValue(), jobInfo, entry.getKey())).collect(Collectors.toList()));
    }

    @Override
    public void registerNotifications(List<NotificationActionEvent> events) {
        Set<NotificationRequest> notificationToRegister = events.stream().map(this::initNotificationRequest)
                .collect(Collectors.toSet());
        notificationToRegister.remove(null);
        this.notificationActionRepo.saveAll(notificationToRegister);
        LOGGER.debug("------------->>> {} notifications registred", notificationToRegister.size());
    }

    /**
     * Check if a notification event is valid and create a request publish an error otherwise
     * @return a implemented {@link NotificationRequest} or null if invalid
     */
    private NotificationRequest initNotificationRequest(NotificationActionEvent event) {
        Errors errors = new MapBindingResult(new HashMap<>(), NotificationActionEvent.class.getName());
        this.validator.validate(event, errors);

        if (!errors.hasErrors()) {
            publisher.publish(new NotifierEvent(event.getRequestId(), NotificationState.GRANTED));
            return new NotificationRequest(event.getPayload(),
                                           event.getMetadata(),
                                           event.getRequestId(),
                                           event.getRequestDate(),
                                           NotificationState.GRANTED);
        }
        this.notificationClient.notify(errors.toString(),
                                       "A NotificationActionEvent received is invalid",
                                       NotificationLevel.ERROR,
                                       DefaultRole.ADMIN);
        publisher.publish(new NotifierEvent(event.getRequestId(), NotificationState.DENIED));
        return null;
    }

    @Override
    public int scheduleRequests() {
        // Shedule job
        Set<JobParameter> jobParameters = Sets.newHashSet();
        List<Long> requestIds = this.notificationActionRepo.findByState(NotificationState.GRANTED,
                                                                        PageRequest.of(0,
                                                                                       properties.getMaxBulkSize(),
                                                                                       Sort.by(Order.asc("requestDate"))))
                .getContent().stream().map(NotifRequestId::getId).collect(Collectors.toList());
        long scheduleStart = System.currentTimeMillis();

        if (!requestIds.isEmpty()) {
            jobParameters.add(new JobParameter(NotificationJob.IDS_PARAMETER, requestIds));

            this.notificationActionRepo.updateState(NotificationState.SCHEDULED, requestIds);
            // the job priority will be set according the priority of the first request to schedule
            JobInfo jobInfo = new JobInfo(false,
                                          0,
                                          jobParameters,
                                          authResolver.getUser(),
                                          NotificationJob.class.getName());
            jobInfoService.createAsQueued(jobInfo);

            LOGGER.debug("------------->>> {} notifications scheduled in {} ms",
                         requestIds.size(),
                         System.currentTimeMillis() - scheduleStart);
        }
        return requestIds.size();
    }

    @Override
    public void cleanCache() {
        this.cleanTenantCache(tenantResolver.getTenant());
    }
}
