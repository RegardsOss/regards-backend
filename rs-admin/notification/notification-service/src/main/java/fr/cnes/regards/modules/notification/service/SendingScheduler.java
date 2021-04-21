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
package fr.cnes.regards.modules.notification.service;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.modules.notification.domain.Notification;
import fr.cnes.regards.modules.notification.domain.NotificationFrequency;
import fr.cnes.regards.modules.notification.domain.NotificationToSendEvent;
import fr.cnes.regards.modules.notification.service.utils.NotificationUserSetting;

/**
 * Service responsible for scheduling the sending of notifications to their recipients.<br>
 * It periodically retrieves the notifications to send, and sends thems to their recipients.<br>
 * Implements a strategy pattern on the sending method in order to easily add ways of sending notifications (mail, ihm,
 * owl...).
 *
 * @author Xavier-Alexandre Brochard
 */
@Service
@EnableScheduling
public class SendingScheduler implements ApplicationListener<NotificationToSendEvent> {

    /**
     * The service responsible for managing notifications
     */
    private final INotificationService notificationService;

    /**
     * The service responsible for managing notification settings
     */
    private final INotificationSettingsService notificationSettingsService;

    /**
     * Notification sending strategy
     */
    private ISendingStrategy strategy;

    /**
     * {@link IRuntimeTenantResolver} instance
     */
    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    /**
     * Create a new scheduler with passed services and repositories
     *
     * @param strategy The notification sending strategy
     * @param notificationService The notification service
     * @param notificationSettingsService The notification settings repository
     */
    public SendingScheduler(final ISendingStrategy strategy, final INotificationService notificationService,
            final INotificationSettingsService notificationSettingsService) {
        super();
        this.strategy = strategy;
        this.notificationService = notificationService;
        this.notificationSettingsService = notificationSettingsService;
    }

    /**
     * Find all notifications which should be sent daily and send them with the sending strategy
     */
    @Scheduled(cron = "${regards.notification.cron.daily}")
    public void sendDaily() {
        final Predicate<NotificationUserSetting> filter = createFrequencyFilter(NotificationFrequency.DAILY);
        filterAndSend(filter);
    }

    /**
     * Find all notifications which should be sent weekly and send them with the sending strategy
     */
    @Scheduled(cron = "${regards.notification.cron.weekly}")
    public void sendWeekly() {
        final Predicate<NotificationUserSetting> filter = createFrequencyFilter(NotificationFrequency.WEEKLY);
        filterAndSend(filter);
    }

    /**
     * Find all notifications which should be sent weekly and send them with the sending strategy
     */
    @Scheduled(cron = "${regards.notification.cron.monthly}")
    public void sendMonthly() {
        final Predicate<NotificationUserSetting> filter = createFrequencyFilter(NotificationFrequency.MONTHLY);
        filterAndSend(filter);
    }

    /**
     * Find all notifications which have a custom sending frequency and send them with the sending strategy if need be
     */
    @Scheduled(cron = "${regards.notification.cron.daily}")
    public void sendCustom() {
        // The filter for custom frequency
        final Predicate<NotificationUserSetting> filterOnCustom = createFrequencyFilter(NotificationFrequency.CUSTOM);

        // Define a filter selecting only triplets (notif, user, setting) for which the duration between now and the
        // (past) notif's sent date is longer than the setting's custom duration (means we should re-send the notif)
        final Predicate<NotificationUserSetting> filterOnPeriodExceeded = n -> {
            final Duration effectiveGap = Duration.between(n.getNotification().getDate(), OffsetDateTime.now());
            final Duration settingGap = Duration.ofDays(n.getSettings().getDays())
                    .plus(Duration.ofHours(n.getSettings().getHours()));
            return effectiveGap.compareTo(settingGap) >= 0;
        };

        filterAndSend(filterOnCustom.and(filterOnPeriodExceeded));
    }

    /**
     * Find all notifications which should be sent, filter them with passed filter and send them with the sending
     * strategy.<br>
     * We gather the triplet (notification, project user, settings) in a simple agregator class in order to allow the
     * usage of filters on the three entities in the stream.
     *
     * @param pFilter The filter with which filter the project users
     */
    private void filterAndSend(final Predicate<NotificationUserSetting> pFilter) {
        // With the stream of unsent notifications
        String tenant = runtimeTenantResolver.getTenant();
        notificationService.retrieveNotificationsToSend(PageRequest.of(0, 10)).getContent().forEach(notification -> {
            runtimeTenantResolver.forceTenant(tenant);
            // Build the list of recipients
            String[] recipients = notificationService.findRecipients(notification).stream()
                    .map(projectUser -> new NotificationUserSetting(notification,
                                                                    projectUser,
                                                                    notificationSettingsService
                                                                            .retrieveNotificationSettings(projectUser)))
                    .filter(pFilter).map(NotificationUserSetting::getProjectUser).distinct().toArray(String[]::new);

            // Send the notification to recipients
            sendNotification(notification, recipients);

        });

    }

    private void sendNotification(Notification notification, String... recipients) {
        if (recipients.length > 0) {
            strategy.send(notification, recipients);
            // Update sent date
            notification.setDate(OffsetDateTime.now());
        }
    }

    /**
     * Create a filter working on {@link NotificationUserSetting} aggregator class in order to keep only elements for
     * which the setting's <code>frequency</code> is equal to passed.
     *
     * @param pFrequency The frequency for the filter
     * @return The filter as a {@link Predicate}. Use in a {@link Stream#map} for example.
     */
    private Predicate<NotificationUserSetting> createFrequencyFilter(final NotificationFrequency pFrequency) {
        return n -> pFrequency
                .equals(notificationSettingsService.retrieveNotificationSettings(n.getProjectUser()).getFrequency());
    }

    /**
     * Change the sending strategy
     *
     * @param pStrategy the strategy to set
     */
    public void changeStrategy(final ISendingStrategy pStrategy) {
        strategy = pStrategy;
    }

    /**
     * Allows to trigger sending process before the frequency
     */
    @Override
    public void onApplicationEvent(NotificationToSendEvent event) {
        Notification notif = event.getNotification();
        String[] recipients = notificationService.findRecipients(notif).toArray(new String[0]);
        sendNotification(notif, recipients);
    }
}
