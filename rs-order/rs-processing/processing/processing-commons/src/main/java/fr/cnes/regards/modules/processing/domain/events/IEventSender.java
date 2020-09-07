package fr.cnes.regards.modules.processing.domain.events;

import fr.cnes.regards.framework.amqp.event.ISubscribable;
import reactor.core.publisher.Mono;

public interface IEventSender<M extends ISubscribable> {

    Mono<M> send(String tenant, M message);

}
