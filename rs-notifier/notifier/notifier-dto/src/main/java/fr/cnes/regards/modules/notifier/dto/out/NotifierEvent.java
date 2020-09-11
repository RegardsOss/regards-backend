package fr.cnes.regards.modules.notifier.dto.out;

import fr.cnes.regards.framework.amqp.event.Event;
import fr.cnes.regards.framework.amqp.event.ISubscribable;
import fr.cnes.regards.framework.amqp.event.JsonMessageConverter;
import fr.cnes.regards.framework.amqp.event.Target;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
@Event(target = Target.ONE_PER_MICROSERVICE_TYPE, converter = JsonMessageConverter.GSON)
public class NotifierEvent implements ISubscribable {

    private String requestId;

    private NotificationState state;

    public NotifierEvent(String requestId, NotificationState state) {
        this.requestId = requestId;
        this.state = state;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public NotificationState getState() {
        return state;
    }

    public void setState(NotificationState state) {
        this.state = state;
    }
}