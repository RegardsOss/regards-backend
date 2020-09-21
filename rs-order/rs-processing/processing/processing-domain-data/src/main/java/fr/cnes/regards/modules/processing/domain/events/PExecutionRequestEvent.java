package fr.cnes.regards.modules.processing.domain.events;

import fr.cnes.regards.framework.amqp.event.Event;
import fr.cnes.regards.framework.amqp.event.ISubscribable;
import fr.cnes.regards.framework.amqp.event.JsonMessageConverter;
import fr.cnes.regards.framework.amqp.event.Target;
import fr.cnes.regards.modules.processing.domain.PInputFile;
import io.vavr.collection.Seq;
import lombok.Value;
import lombok.With;

import java.util.UUID;

@Event(target = Target.ONE_PER_MICROSERVICE_TYPE, converter = JsonMessageConverter.GSON)
@Value @With
public class PExecutionRequestEvent implements ISubscribable {

    String executionCorrelationId;

    UUID batchId;

    Seq<PInputFile> inputFiles;

}
