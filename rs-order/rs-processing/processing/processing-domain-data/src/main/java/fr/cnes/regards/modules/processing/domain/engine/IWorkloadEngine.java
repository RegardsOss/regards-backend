package fr.cnes.regards.modules.processing.domain.engine;

import fr.cnes.regards.modules.processing.domain.PExecution;
import fr.cnes.regards.modules.processing.domain.execution.ExecutionContext;
import reactor.core.publisher.Mono;

import javax.annotation.PostConstruct;

public interface IWorkloadEngine {

    String name();

    Mono<PExecution> run(ExecutionContext context);

    @PostConstruct
    void selfRegisterInRepo();

}
