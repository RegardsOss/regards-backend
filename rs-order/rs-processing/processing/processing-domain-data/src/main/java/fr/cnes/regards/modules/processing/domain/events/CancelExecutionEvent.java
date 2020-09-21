package fr.cnes.regards.modules.processing.domain.events;

import fr.cnes.regards.framework.amqp.event.Event;
import fr.cnes.regards.framework.amqp.event.ISubscribable;
import fr.cnes.regards.framework.amqp.event.JsonMessageConverter;
import fr.cnes.regards.framework.amqp.event.Target;
import lombok.Value;
import lombok.With;

import java.util.UUID;

// TODO: cancelling an execution is not supported yet
@Event(target = Target.ONE_PER_MICROSERVICE_TYPE, converter = JsonMessageConverter.GSON)
@Value @With
public class CancelExecutionEvent implements ISubscribable {

    UUID executionId;
    String message;

}
