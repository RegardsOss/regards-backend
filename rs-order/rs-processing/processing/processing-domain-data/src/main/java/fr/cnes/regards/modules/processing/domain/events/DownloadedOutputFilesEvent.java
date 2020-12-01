package fr.cnes.regards.modules.processing.domain.events;

import fr.cnes.regards.framework.amqp.event.Event;
import fr.cnes.regards.framework.amqp.event.ISubscribable;
import fr.cnes.regards.framework.amqp.event.JsonMessageConverter;
import fr.cnes.regards.framework.amqp.event.Target;
import io.vavr.collection.List;
import lombok.Value;
import lombok.With;

import java.net.URL;
import java.util.UUID;

@Event(target = Target.ONE_PER_MICROSERVICE_TYPE, converter = JsonMessageConverter.GSON)
@Value @With
public class DownloadedOutputFilesEvent implements ISubscribable {

    List<URL> outputFileUrls;

}
