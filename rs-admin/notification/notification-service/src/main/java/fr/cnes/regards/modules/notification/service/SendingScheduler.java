/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.notification.service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import fr.cnes.regards.modules.notification.dao.INotificationSettingsRepository;
import fr.cnes.regards.modules.notification.domain.Notification;
import fr.cnes.regards.modules.notification.domain.NotificationFrequency;
import fr.cnes.regards.modules.notification.service.utils.NotificationUserSetting;

/**
 * Service responsible for scheduling the sending of notifications to their recipients.<br>
 * It periodicly retrieves the notifications to send, and sends thems to their recipients.<br>
 * Implements a strategy pattern on the sending method in order to easily add ways of sending notifications (mail, ihm,
 * owl...).
 *
 * @author CS SI
 */
@Service
public class SendingScheduler {

    /**
     * Notification sending strategy
     */
    private ISendingStrategy strategy;

    /**
     * The service responsible for managing notifications
     */
    private final INotificationService notificationService;

    /**
     * The service responsible for managing notification settings
     */
    private final INotificationSettingsRepository notificationSettingsRepository;

    /**
     * Create a new scheduler with passed services and repositories
     *
     * @param pStrategy
     *            The notification sending strategy
     * @param pNotificationService
     *            The notification service
     * @param pNotificationSettingsRepository
     *            The notification settings repository
     */
    public SendingScheduler(final ISendingStrategy pStrategy, final INotificationService pNotificationService,
            final INotificationSettingsRepository pNotificationSettingsRepository) {
        super();
        strategy = pStrategy;
        notificationService = pNotificationService;
        notificationSettingsRepository = pNotificationSettingsRepository;
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

        // The filter selecting only triplets (notif, user, setting) for which the duration between now and the (past)
        // notif's sent date is longer than the setting's custom duration (means we should re-send the notif)
        final Predicate<NotificationUserSetting> filterOnPeriodExceeded = n -> {
            final Duration effectiveGap = Duration.between(n.getNotification().getDate().toLocalDate(),
                                                           LocalDate.now());
            final Duration settingGap = Duration.ofDays(n.getSettings().getDays())
                    .plus(Duration.ofHours(n.getSettings().getHours()));
            return settingGap.compareTo(effectiveGap) >= 0;
        };

        filterAndSend(filterOnCustom.and(filterOnPeriodExceeded));
    }

    /**
     * Find all notifications which should be sent, filter them with passed filter and send them with the sending
     * strategy.<br>
     * We gather the triplet (notification, project user, settings) in a simple agregator class in order to allow the
     * usage of filters on the three entities in the stream.
     *
     * @param pFilter
     *            The filter with which filter the project users
     */
    private void filterAndSend(final Predicate<NotificationUserSetting> pFilter) {
        // With the stream of unsent notifications
        try (final Stream<Notification> stream = notificationService.retrieveNotificationsToSend().parallelStream()) {
            stream.forEach(notification -> {
                // Build the list of recipients
                final String[] recipients = notificationService.findRecipients(notification)
                        .map(projectUser -> new NotificationUserSetting(notification, projectUser,
                                notificationSettingsRepository.findOneByProjectUser(projectUser)))
                        .filter(pFilter).map(n -> n.getProjectUser().getEmail()).distinct().toArray(s -> new String[s]);

                // Send the notification to recipients
                if (recipients.length > 0) {
                    strategy.send(notification, recipients);
                }

                // Update sent date
                notification.setDate(LocalDateTime.now());
            });
        }
    }

    /**
     * Create a filter working on {@link NotificationUserSetting} aggregator class in order to keep only elements for
     * which the setting's <code>frequency</code> is equal to passed.
     *
     * @param pFrequency
     *            The frequency for the filter
     * @return The filter as a {@link Predicate}. Use in a {@link Stream#map} for example.
     */
    private Predicate<NotificationUserSetting> createFrequencyFilter(final NotificationFrequency pFrequency) {
        return n -> pFrequency
                .equals(notificationSettingsRepository.findOneByProjectUser(n.getProjectUser()).getFrequency());
    }

    /**
     * Change the sending strategy
     *
     * @param pStrategy
     *            the strategy to set
     */
    public void changeStrategy(final ISendingStrategy pStrategy) {
        strategy = pStrategy;
    }
}
