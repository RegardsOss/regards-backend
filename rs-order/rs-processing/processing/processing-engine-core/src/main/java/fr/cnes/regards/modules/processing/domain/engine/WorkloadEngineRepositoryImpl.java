/* Copyright 2017-2020 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.processing.domain.engine;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import fr.cnes.regards.modules.processing.domain.repository.IWorkloadEngineRepository;
import io.vavr.control.Option;
import reactor.core.publisher.Mono;

/**
 * TODO : Class description
 *
 * @author Guillaume Andrieu
 *
 */
@Component
public class WorkloadEngineRepositoryImpl implements IWorkloadEngineRepository {

    private final Map<String, IWorkloadEngine> enginesByName = new HashMap<>();

    @Override
    public Mono<IWorkloadEngine> register(IWorkloadEngine engine) {
        String name = engine.name();
        if (enginesByName.containsKey(name)) {
            return Mono.error(new EngineAlreadyExistsException(name));
        } else {
            return Mono.just(engine);
        }
    }

    @Override
    public Mono<IWorkloadEngine> findByName(String name) {
        return Mono.defer(() -> Option.of(enginesByName.get(name))
                .fold(() -> Mono.error(new EngineNotFoundException(name)), Mono::just));
    }

    @SuppressWarnings("serial")
    public static class EngineAlreadyExistsException extends Exception {

        public EngineAlreadyExistsException(String s) {
            super("Engine already exists for name '" + s + "'");
        }
    }

    @SuppressWarnings("serial")
    public static class EngineNotFoundException extends Exception {

        public EngineNotFoundException(String s) {
            super("Engine not found for name '" + s + "'");
        }
    }
}
