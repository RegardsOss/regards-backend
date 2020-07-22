package fr.cnes.regards.modules.processing.repository;

import fr.cnes.regards.modules.processing.domain.engine.IWorkloadEngine;
import reactor.core.publisher.Mono;

public interface IWorkloadEngineRepository {

    Mono<IWorkloadEngine> findByName(String name);

    Mono<IWorkloadEngine> register(IWorkloadEngine engine);
}
