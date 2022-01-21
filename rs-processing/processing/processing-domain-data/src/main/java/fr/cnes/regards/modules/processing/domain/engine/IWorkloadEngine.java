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
package fr.cnes.regards.modules.processing.domain.engine;

import fr.cnes.regards.modules.processing.domain.PExecution;
import fr.cnes.regards.modules.processing.domain.execution.ExecutionContext;
import reactor.core.publisher.Mono;


/**
 * This interface defines an engine: something that takes a context and returns a running execution.
 *
 * The engine may perform the execution's executable asynchronously, saving some state about
 * the execution, etc., but ultimately it is responsible for launching the process's
 * executable for the given context.
 *
 * @author gandrieu
 */
public interface IWorkloadEngine {

    String name();

    Mono<PExecution> run(ExecutionContext context);

    void selfRegisterInRepo();

}
