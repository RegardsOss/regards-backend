package fr.cnes.regards.framework.modules.workspace.service;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

import fr.cnes.regards.framework.security.role.DefaultRole;

/**
 * Default implementation, doing nothing. This implementation is only used if no other implementation is detected by spring.
 * @author Sylvain VISSIERE-GUERINET
 */
@Component
@ConditionalOnMissingBean(IWorkspaceNotifier.class)
public class DefaultWorkspaceNotifier implements IWorkspaceNotifier {

    @Override
    public void sendErrorNotification(String springApplicationName, String message, String title, DefaultRole role) {
        //does nothing
    }

}
