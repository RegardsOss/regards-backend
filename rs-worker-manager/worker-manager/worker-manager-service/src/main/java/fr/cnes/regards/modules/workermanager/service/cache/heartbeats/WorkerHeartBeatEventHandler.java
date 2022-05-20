package fr.cnes.regards.modules.workermanager.service.cache.heartbeats;

import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.amqp.batch.IBatchHandler;
import fr.cnes.regards.modules.workermanager.dto.events.in.WorkerHeartBeatEvent;
import fr.cnes.regards.modules.workermanager.service.cache.WorkerCacheService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;

import java.util.List;

/**
 * Handler of {@link WorkerHeartBeatEvent} events
 * <p>
 * NOTE : heartbeat queue is created with autoDelete amqp option. With this options tests will fails during queue
 * creation. So this handler bean is never instantiated in test context
 *
 * @author Léo Mieulet
 */
@Component
@Profile("!testAmqp")
public class WorkerHeartBeatEventHandler
    implements IBatchHandler<WorkerHeartBeatEvent>, ApplicationListener<ApplicationReadyEvent> {

    private final ISubscriber subscriber;

    private final WorkerCacheService workerCacheService;

    @Value("${regards.amqp.microservice.instanceIdentifier: instance}")
    private String instanceIdentifier;

    public WorkerHeartBeatEventHandler(ISubscriber subscriber, WorkerCacheService workerCacheService) {
        this.subscriber = subscriber;
        this.workerCacheService = workerCacheService;
    }

    @Override
    public Class<WorkerHeartBeatEvent> getMType() {
        return WorkerHeartBeatEvent.class;
    }

    @Override
    public Errors validate(WorkerHeartBeatEvent message) {
        return null;
    }

    @Override
    public void handleBatch(List<WorkerHeartBeatEvent> messages) {
        workerCacheService.registerWorkers(messages);
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        // Use a random uuid to create a unique queue for this instance of the microservice.
        // Heartbeat needs to be listened by each instance of microservice so a queue is bind to the exchange for each instance.
        subscriber.subscribeTo(WorkerHeartBeatEvent.class,
                               this,
                               "regards.worker.manager.heartbeat." + instanceIdentifier,
                               "regards.worker.manager.heartbeat");
    }
}
