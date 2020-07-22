package fr.cnes.regards.modules.processing.service.events;

import fr.cnes.regards.framework.amqp.event.Event;
import fr.cnes.regards.framework.amqp.event.ISubscribable;
import fr.cnes.regards.framework.amqp.event.JsonMessageConverter;
import fr.cnes.regards.framework.amqp.event.Target;
import fr.cnes.regards.modules.processing.domain.parameters.ExecutionFileParameterValue;
import io.vavr.collection.Seq;
import lombok.Value;
import lombok.With;

import java.util.UUID;

@Value @With

@Event(target = Target.ONE_PER_MICROSERVICE_TYPE, converter = JsonMessageConverter.GSON)
// TODO verify these @Event parameters
public class PExecutionRequestEvent implements ISubscribable {

    UUID executionId;

    UUID batchId;

    Seq<ExecutionFileParameterValue> inputFiles;

}
