package fr.cnes.regards.modules.notifier.client;

import fr.cnes.regards.modules.notifier.dto.out.NotifierEvent;

import java.util.List;

/**
 * Listener to implement to handle {@link NotifierEvent}, which contains infos about Notifier requests it handled
 *
 * @author Sylvain VISSIERE-GUERINET
 */
public interface INotifierRequestListener {

    /**
     * Allows to react on a notification request that has NOT been accepted by notifier
     */
    void onRequestDenied(List<NotifierEvent> denied);

    /**
     * Allows to react on a notification request that has been accepted by notifier
     */
    void onRequestGranted(List<NotifierEvent> granted);

    /**
     * Allows to react on a notification request that has been successfully handled by notifier
     */
    void onRequestSuccess(List<NotifierEvent> success);

    /**
     * Allows to react on a notification request that has NOT been successfully handled by notifier
     */
    void onRequestError(List<NotifierEvent> error);
}
