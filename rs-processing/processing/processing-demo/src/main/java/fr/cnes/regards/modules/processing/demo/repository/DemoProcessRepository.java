/* Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
 *
 * This file is part of REGARDS.
 *
 * REGARDS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * REGARDS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with REGARDS. If not, see <http://www.gnu.org/licenses/>.
 */
package fr.cnes.regards.modules.processing.demo.repository;

import fr.cnes.regards.modules.processing.demo.engine.DemoEngine;
import fr.cnes.regards.modules.processing.demo.process.DemoProcess;
import fr.cnes.regards.modules.processing.demo.process.DemoSimulatedAsyncProcessFactory;
import fr.cnes.regards.modules.processing.domain.PBatch;
import fr.cnes.regards.modules.processing.domain.PProcess;
import fr.cnes.regards.modules.processing.domain.repository.IPProcessRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * This class is the demo process repository.
 *
 * @author gandrieu
 */
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

    @Override
    public Flux<PProcess> findAllByTenant(String tenant) {
        return findAll(); // No tenants usd  here
    }

    @Override
    public Mono<PProcess> findByTenantAndProcessBusinessID(String tenant, UUID processId) {
        return findById(processId); // No tenants used  here
    }

    @Override
    public Mono<PProcess> findByBatch(PBatch batch) {
        return findById(batch.getProcessBusinessId());
    }
}
