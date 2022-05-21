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
package fr.cnes.regards.modules.processing.demo.engine;

import fr.cnes.regards.modules.processing.domain.PExecution;
import fr.cnes.regards.modules.processing.domain.engine.IWorkloadEngine;
import fr.cnes.regards.modules.processing.domain.execution.ExecutionContext;
import fr.cnes.regards.modules.processing.domain.repository.IWorkloadEngineRepository;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Delegate the processing to someone else, just communicating with it through amqp.
 *
 * @author gandrieu
 */
@Component
public class DemoEngine implements IWorkloadEngine, InitializingBean {

    private final IWorkloadEngineRepository engineRepo;

    @Autowired
    public DemoEngine(IWorkloadEngineRepository engineRepo) {
        this.engineRepo = engineRepo;
    }

    @Override
    public String name() {
        return "DEMO";
    }

    @Override
    public Mono<PExecution> run(ExecutionContext context) {
        return null;
    }

    @Override
    public void afterPropertiesSet() {
        // selfRegisterInRepo
        engineRepo.register(this);
    }
}
