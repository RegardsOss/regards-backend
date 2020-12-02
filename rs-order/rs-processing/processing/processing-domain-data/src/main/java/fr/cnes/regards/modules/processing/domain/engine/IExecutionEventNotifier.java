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

import fr.cnes.regards.modules.processing.domain.PExecution;
import fr.cnes.regards.modules.processing.domain.POutputFile;
import fr.cnes.regards.modules.processing.domain.step.PStepFinal;
import fr.cnes.regards.modules.processing.domain.step.PStepIntermediary;
import io.vavr.Function1;
import io.vavr.collection.Seq;
import reactor.core.publisher.Mono;

import static fr.cnes.regards.modules.processing.domain.engine.ExecutionEvent.event;

/**
 * This interface is given to an IExecutable to allow the executable to notify events for its execution.
 *
 * @author gandrieu 
 */
public interface IExecutionEventNotifier extends Function1<ExecutionEvent, Mono<PExecution>> {

    Mono<PExecution> notifyEvent(ExecutionEvent event);

    default Mono<PExecution> apply(ExecutionEvent event) {
        return notifyEvent(event);
    }

    default  Mono<PExecution> notifyEvent(PStepFinal step) {
        return apply(event(step));
    }

    default  Mono<PExecution> notifyEvent(PStepIntermediary step) {
        return apply(event(step));
    }

    default  Mono<PExecution> notifyEvent(PStepFinal step, Seq<POutputFile> files) {
        return apply(event(step, files));
    }
}
