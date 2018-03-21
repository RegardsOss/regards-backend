package fr.cnes.regards.modules.workspace;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import fr.cnes.regards.framework.modules.workspace.service.IWorkspaceNotifier;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.modules.notification.client.INotificationClient;
import fr.cnes.regards.modules.notification.domain.NotificationType;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
@Component
public class NotificationWorkspaceNotifier implements IWorkspaceNotifier {

    @Autowired
    private INotificationClient notificationClient;

    @Override
    public void sendErrorNotification(String sender, String message, String title, DefaultRole role) {
        notificationClient.notifyRoles(message, title, sender, NotificationType.ERROR, role);
    }

    @Override
    public void sendWarningNotification(String sender, String message, String title, DefaultRole role) {
        notificationClient.notifyRoles(message, title, sender, NotificationType.WARNING, role);
    }
}
