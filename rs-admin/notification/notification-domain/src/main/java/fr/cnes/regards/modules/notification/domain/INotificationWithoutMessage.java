package fr.cnes.regards.modules.notification.domain;

import fr.cnes.regards.framework.gson.annotation.Gsonable;
import fr.cnes.regards.framework.notification.NotificationLevel;
import org.springframework.util.MimeType;

import java.time.OffsetDateTime;
import java.util.Set;

/**
 * Interface used to deserialize the notification object and get it without its message {@link NotificationAdapter}
 */
public interface INotificationWithoutMessage {
    OffsetDateTime getDate();
    Long getId();
    Set<String> getRoleRecipients();
    Set<String> getProjectUserRecipients();
    String getSender();
    NotificationStatus getStatus();
    NotificationLevel getLevel();
    String getTitle();
    MimeType getMimeType();
}
