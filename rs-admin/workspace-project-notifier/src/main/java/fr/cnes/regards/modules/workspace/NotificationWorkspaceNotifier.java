package fr.cnes.regards.modules.workspace;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import fr.cnes.regards.framework.feign.security.FeignSecurityManager;
import fr.cnes.regards.framework.modules.workspace.service.IWorkspaceNotifier;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.modules.notification.client.INotificationClient;
import fr.cnes.regards.modules.notification.domain.NotificationLevel;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
@Component
public class NotificationWorkspaceNotifier implements IWorkspaceNotifier {

    @Autowired
    private INotificationClient notificationClient;

    @Override
    public void sendErrorNotification(String message, String title, DefaultRole role) {
        FeignSecurityManager.asSystem();
        notificationClient.notify(message, title, NotificationLevel.ERROR, role);
        FeignSecurityManager.reset();
    }

    @Override
    public void sendWarningNotification(String message, String title, DefaultRole role) {
        FeignSecurityManager.asSystem();
        notificationClient.notify(message, title, NotificationLevel.WARNING, role);
        FeignSecurityManager.reset();
    }
}
