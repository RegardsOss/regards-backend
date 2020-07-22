package fr.cnes.regards.modules.processing.repository;

import fr.cnes.regards.modules.processing.domain.PProcess;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface IPProcessRepository {

    Flux<PProcess> findAll();

    Flux<PProcess> findAllByTenantAndUserRole(String tenant, String userRole);

    Mono<PProcess> findByName(String name);

}
