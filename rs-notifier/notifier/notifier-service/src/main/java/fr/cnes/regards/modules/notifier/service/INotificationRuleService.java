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

import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.springframework.data.util.Pair;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.modules.notifier.domain.NotificationRequest;
import fr.cnes.regards.modules.notifier.domain.Rule;
import fr.cnes.regards.modules.notifier.dto.in.NotificationRequestEvent;

/**
 * Notification service interface
 * @author Kevin Marchois
 *
 */
public interface INotificationRuleService {

    /**
     * Handle a list of {@link NotificationRequest}
     * @param notificationRequests requests to handle
     * @param recipient recipient to process
     * @return pair of nbSent/nbErrors notifications
     */
    Pair<Integer, Integer> processRequest(List<NotificationRequest> notificationRequests,
            PluginConfiguration recipient);

    Pair<Integer, Integer> handleRecipientResults(List<NotificationRequest> notificationRequests,
            PluginConfiguration recipient, Collection<NotificationRequest> notificationsInError);

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    Pair<Integer, Integer> handleRecipientResultsConcurrent(List<NotificationRequest> notificationRequests,
            PluginConfiguration recipient, Collection<NotificationRequest> notificationsInError);

    /**
     * Register {@link NotificationRequestEvent} to schedule notifications
     */
    void registerNotificationRequests(List<NotificationRequestEvent> events);

    Set<NotificationRequestEvent> handleRetryRequests(List<NotificationRequestEvent> events);

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    Set<NotificationRequestEvent> handleRetryRequestsConcurrent(List<NotificationRequestEvent> events);

    /**
     * Clean cache of rules. Need to be called after each configuration modification.
     */
    void cleanCache();

    /**
     * Match one page of {@link NotificationRequest} to multiple recipient({@link PluginConfiguration}) using all {@link Rule}s
     * @return Pair representing how many notification have been matched to how many recipient (nbNotificationHandled, nbRecipient)
     */
    Pair<Integer, Integer> matchRequestNRecipient();

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    Pair<Integer, Integer> matchRequestNRecipientConcurrent(List<NotificationRequest> toBeMatched);

    /**
     * @param recipient recipient for which we want to schedule a {@link fr.cnes.regards.modules.notifier.service.job.NotificationJob}
     * @return 1 if there is {@link NotificationRequest} to schedule for this recipient, 0 otherwise
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    Set<Long> scheduleJobForOneRecipient(PluginConfiguration recipient);

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    Set<Long> scheduleJobForOneRecipientConcurrent(PluginConfiguration recipient,
            List<NotificationRequest> requestsToSchedule);

    /**
     * Check whether a {@link NotificationRequest} is in success or not and notify its success
     * @return number of request in success detected this time.
     */
    int checkSuccess();
}
