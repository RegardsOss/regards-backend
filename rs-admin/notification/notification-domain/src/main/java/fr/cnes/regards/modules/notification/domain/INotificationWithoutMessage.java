package fr.cnes.regards.modules.notification.domain;

import java.time.OffsetDateTime;
import java.util.Set;

import org.springframework.util.MimeType;

import fr.cnes.regards.framework.notification.NotificationLevel;

/**
 * Interface used to deserialize the notification object and get it without its message {@link NotificationAdapter}
 */
public interface INotificationWithoutMessage {

    OffsetDateTime getDate();

    Long getId();

    String getSender();

    NotificationStatus getStatus();

    NotificationLevel getLevel();

    String getTitle();

    MimeType getMimeType();
}
