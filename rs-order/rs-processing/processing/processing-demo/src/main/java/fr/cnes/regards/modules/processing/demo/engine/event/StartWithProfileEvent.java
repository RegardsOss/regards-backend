package fr.cnes.regards.modules.processing.demo.engine.event;

import fr.cnes.regards.framework.amqp.event.ISubscribable;
import io.vavr.collection.List;

@lombok.Value
public class StartWithProfileEvent implements ISubscribable {
    String profile;
    List<String> inputUrls;
}
