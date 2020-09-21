package fr.cnes.regards.modules.processing.demo.engine;

import fr.cnes.regards.modules.processing.domain.PExecution;
import fr.cnes.regards.modules.processing.domain.engine.IWorkloadEngine;
import fr.cnes.regards.modules.processing.domain.execution.ExecutionContext;
import fr.cnes.regards.modules.processing.domain.repository.IWorkloadEngineRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Delegate the processing to someone else, just communicating with it through amqp.
 */
@Component
public class DemoEngine implements IWorkloadEngine {

    private final IWorkloadEngineRepository engineRepo;

    @Autowired
    public DemoEngine(IWorkloadEngineRepository engineRepo) {
        this.engineRepo = engineRepo;
    }

    @Override public String name() {
        return "DEMO";
    }

    @Override public Mono<PExecution> run(ExecutionContext context) {
        return null;
    }

    @Override public void selfRegisterInRepo() {
        engineRepo.register(this);
    }
}
