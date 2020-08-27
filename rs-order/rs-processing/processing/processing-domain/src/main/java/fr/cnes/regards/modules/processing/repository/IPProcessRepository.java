package fr.cnes.regards.modules.processing.repository;

import fr.cnes.regards.modules.processing.domain.PBatch;
import fr.cnes.regards.modules.processing.domain.PProcess;
import fr.cnes.regards.modules.processing.domain.PUserAuth;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface IPProcessRepository {

    Flux<PProcess> findAllByTenant(String tenant);

    Mono<PProcess> findByTenantAndProcessName(String tenant, String processName);

    Mono<PProcess> findByTenantAndProcessBusinessID(String tenant, UUID processId);

    Flux<PProcess> findAllByTenantAndUserRole(PUserAuth auth);

    Mono<PProcess> findByBatch(PBatch batch);

}
