package fr.cnes.regards.modules.processing.domain.engine;

import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.modules.processing.repository.IWorkloadEngineRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
public abstract class AbstractWorkloadEngine implements IWorkloadEngine {

    private final IWorkloadEngineRepository engineRepo;

    private final ISubscriber subscriber;

    private final IPublisher publisher;

    @Autowired
    public AbstractWorkloadEngine(
            IWorkloadEngineRepository engineRepo,
            ISubscriber subscriber,
            IPublisher publisher
    ) {
        this.engineRepo = engineRepo;
        this.subscriber = subscriber;
        this.publisher = publisher;
    }

    @PostConstruct
    public final void register() {
        engineRepo.register(this);
    }

}
