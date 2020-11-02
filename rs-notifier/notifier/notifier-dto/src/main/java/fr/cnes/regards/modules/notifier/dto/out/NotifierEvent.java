package fr.cnes.regards.modules.notifier.dto.out;

import java.util.Objects;

import fr.cnes.regards.framework.amqp.event.Event;
import fr.cnes.regards.framework.amqp.event.ISubscribable;
import fr.cnes.regards.framework.amqp.event.JsonMessageConverter;
import fr.cnes.regards.framework.amqp.event.Target;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
@Event(target = Target.ONE_PER_MICROSERVICE_TYPE, converter = JsonMessageConverter.GSON)
public class NotifierEvent implements ISubscribable {

    private final String requestId;

    private final String requestOwner;

    private final NotificationState state;

    public NotifierEvent(String requestId, String requestOwner, NotificationState state) {
        this.requestId = requestId;
        this.requestOwner = requestOwner;
        this.state = state;
    }

    public String getRequestId() {
        return requestId;
    }

    public NotificationState getState() {
        return state;
    }

    public String getRequestOwner() {
        return requestOwner;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        NotifierEvent that = (NotifierEvent) o;
        return Objects.equals(requestId, that.requestId) && Objects.equals(requestOwner, that.requestOwner);
    }

    @Override
    public int hashCode() {
        return Objects.hash(requestId, requestOwner);
    }
}