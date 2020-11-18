package fr.cnes.regards.modules.processing.demo.repository;

import fr.cnes.regards.modules.processing.demo.engine.DemoEngine;
import fr.cnes.regards.modules.processing.demo.process.DemoProcess;
import fr.cnes.regards.modules.processing.demo.process.DemoSimulatedAsyncProcessFactory;
import fr.cnes.regards.modules.processing.domain.PBatch;
import fr.cnes.regards.modules.processing.domain.PProcess;
import fr.cnes.regards.modules.processing.domain.PUserAuth;
import fr.cnes.regards.modules.processing.domain.repository.IPProcessRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
public class DemoProcessRepository implements IPProcessRepository {

    private final DemoEngine engine;
    private final DemoSimulatedAsyncProcessFactory asyncProcessFactory;

    @Autowired
    public DemoProcessRepository(DemoEngine engine, DemoSimulatedAsyncProcessFactory asyncProcessFactory) {
        this.engine = engine;
        this.asyncProcessFactory = asyncProcessFactory;
    }

    public Flux<PProcess> findAll() {
        return Flux.just(new DemoProcess(engine, asyncProcessFactory));
    }

    public Mono<PProcess> findById(UUID processId) {
        return findAll().filter(p -> p.getProcessId().equals(processId)).next();
    }

    @Override public Flux<PProcess> findAllByTenant(String tenant) {
        return findAll(); // No tenants usd  here
    }

    @Override public Mono<PProcess> findByTenantAndProcessName(String tenant, String processName) {
        return findAll().filter(p -> p.getProcessName().equals(processName)).next(); // No tenants used  here
    }

    @Override public Mono<PProcess> findByTenantAndProcessBusinessID(String tenant, UUID processId) {
        return findById(processId); // No tenants used  here
    }

    @Override public Flux<PProcess> findAllByTenantAndUserRole(PUserAuth auth) {
        return findAll();
    }

    @Override public Mono<PProcess> findByBatch(PBatch batch) {
        return findById(batch.getProcessBusinessId());
    }
}
