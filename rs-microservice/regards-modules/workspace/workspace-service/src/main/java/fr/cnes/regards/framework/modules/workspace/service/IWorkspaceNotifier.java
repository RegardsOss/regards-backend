package fr.cnes.regards.framework.modules.workspace.service;

import fr.cnes.regards.framework.security.role.DefaultRole;

/**
 * Interface allowing us to use notification module from rs-admin without adding cyclic git repository dependency
 *
 * @author Sylvain VISSIERE-GUERINET
 */
public interface IWorkspaceNotifier {

    void sendErrorNotification(String springApplicationName, String message, String title, DefaultRole role);
}
