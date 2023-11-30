package fr.cnes.regards.modules.notifier.client;

import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.amqp.batch.IBatchHandler;
import fr.cnes.regards.modules.notifier.dto.out.NotifierEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;

import java.util.ArrayList;
import java.util.List;

/**
 * Handler to handle {@link NotifierEvent} events
 *
 * @author Sylvain VISSIERE-GUERINET
 */
@Component
public class NotificationEventHandler
    implements ApplicationListener<ApplicationReadyEvent>, IBatchHandler<NotifierEvent> {

    @Autowired
    private ISubscriber subscriber;

    @Autowired(required = false)
    private INotifierRequestListener notifierRequestListener;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent applicationReadyEvent) {
        if (notifierRequestListener != null) {
            subscriber.subscribeTo(NotifierEvent.class, this);
        } else {
            LOGGER.warn("No listener configured to collect storage NotifierEvent bus messages !!");
        }
    }

    @Override
    public Errors validate(NotifierEvent message) {
        return null;
    }

    @Override
    public void handleBatch(List<NotifierEvent> messages) {
        LOGGER.debug("[NOTIFIER RESPONSES HANDLER] Handling {} NotifierEvent...", messages.size());
        long start = System.currentTimeMillis();

        handle(messages);

        LOGGER.debug("[NOTIFIER RESPONSES HANDLER] {} NotifierEvent handled in {} ms",
                     messages.size(),
                     System.currentTimeMillis() - start);
    }

    private void handle(List<NotifierEvent> events) {
        List<NotifierEvent> denied = new ArrayList<>();
        List<NotifierEvent> granted = new ArrayList<>();
        List<NotifierEvent> success = new ArrayList<>();
        List<NotifierEvent> error = new ArrayList<>();
        // dispatch event according to which state caused them to be sent
        for (NotifierEvent event : events) {
            switch (event.getState()) {
                case DENIED:
                    denied.add(event);
                    break;
                case GRANTED:
                    granted.add(event);
                    break;
                case SCHEDULED:
                    // this state is of no meaning for clients,
                    // they already know that request has been taken into account thanks to GRANTED
                    break;
                case SUCCESS:
                    success.add(event);
                    break;
                case ERROR:
                    error.add(event);
                    break;
                default:
                    break;
            }
        }
        // now manage message in right order
        if (!denied.isEmpty()) {
            manageDenied(denied);
        }
        if (!granted.isEmpty()) {
            manageGranted(granted);
        }
        if (!error.isEmpty()) {
            manageError(error);
        }
        if (!success.isEmpty()) {
            manageSuccess(success);
        }
    }

    private void manageDenied(List<NotifierEvent> denied) {
        notifierRequestListener.onRequestDenied(denied);
    }

    private void manageSuccess(List<NotifierEvent> success) {
        notifierRequestListener.onRequestSuccess(success);
    }

    private void manageError(List<NotifierEvent> error) {
        notifierRequestListener.onRequestError(error);
    }

    private void manageGranted(List<NotifierEvent> granted) {
        notifierRequestListener.onRequestGranted(granted);
    }
}
