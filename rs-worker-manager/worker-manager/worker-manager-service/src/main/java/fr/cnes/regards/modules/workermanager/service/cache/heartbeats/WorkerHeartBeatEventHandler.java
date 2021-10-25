package fr.cnes.regards.modules.workermanager.service.cache.heartbeats;

import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.amqp.batch.IBatchHandler;
import fr.cnes.regards.modules.workermanager.dto.events.in.WorkerHeartBeatEvent;
import fr.cnes.regards.modules.workermanager.service.cache.WorkerCacheService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;

import java.util.List;

/**
 * Handler of {@link WorkerHeartBeatEvent} events
 *
 * @author Léo Mieulet
 */
@Component
@Profile("!nohandler")
public class WorkerHeartBeatEventHandler
        implements IBatchHandler<WorkerHeartBeatEvent>, ApplicationListener<ApplicationReadyEvent> {

    @Autowired
    private ISubscriber subscriber;

    @Autowired
    private WorkerCacheService workerCacheService;

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
        subscriber.subscribeTo(WorkerHeartBeatEvent.class, this);
    }
}
