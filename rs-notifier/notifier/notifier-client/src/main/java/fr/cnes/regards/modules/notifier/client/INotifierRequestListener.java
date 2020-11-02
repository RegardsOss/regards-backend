package fr.cnes.regards.modules.notifier.client;

import java.util.List;

import fr.cnes.regards.modules.notifier.dto.out.NotifierEvent;

/**
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
