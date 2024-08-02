/* Copyright 2017-2024 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.processing.demo.process;

import fr.cnes.regards.modules.processing.demo.engine.event.StartWithProfileEvent;
import fr.cnes.regards.modules.processing.domain.engine.IExecutable;
import fr.cnes.regards.modules.processing.domain.execution.ExecutionContext;
import reactor.core.publisher.Mono;

import static fr.cnes.regards.modules.processing.demo.DemoConstants.NO_PROFILE_FOUND;
import static fr.cnes.regards.modules.processing.demo.DemoConstants.PROFILE;

/**
 * This class is the demo executable.
 *
 * @author gandrieu
 */
public class DemoProcessExecutable implements IExecutable {

    private final DemoSimulatedAsyncProcessFactory asyncProcessFactory;

    public DemoProcessExecutable(DemoSimulatedAsyncProcessFactory asyncProcessFactory) {
        this.asyncProcessFactory = asyncProcessFactory;
    }

    @Override
    public Mono<ExecutionContext> execute(ExecutionContext context) {
        return Mono.fromCallable(() -> {
            // Extract the parameters
            String profile = context.getBatch()
                                    .getUserSuppliedParameters()
                                    .filter(v -> v.getName().equals(PROFILE))
                                    .map(v -> v.getValue())
                                    .headOption()
                                    .getOrElse(NO_PROFILE_FOUND);

            // Call async service, which will provide the steps as amqp messages
            asyncProcessFactory.make()
                               .send(context,
                                     new StartWithProfileEvent(profile,
                                                               context.getExec()
                                                                      .getInputFiles()
                                                                      .map(f -> f.getUrl().toString())
                                                                      .toList()));

            return context;
        });
    }
}
