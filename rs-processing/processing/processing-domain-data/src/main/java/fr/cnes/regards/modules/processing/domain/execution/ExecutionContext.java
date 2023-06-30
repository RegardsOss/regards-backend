/* Copyright 2017-2021 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.processing.domain.execution;

import fr.cnes.regards.modules.processing.domain.PBatch;
import fr.cnes.regards.modules.processing.domain.PExecution;
import fr.cnes.regards.modules.processing.domain.PProcess;
import fr.cnes.regards.modules.processing.domain.engine.ExecutionEvent;
import fr.cnes.regards.modules.processing.domain.engine.IExecutionEventNotifier;
import fr.cnes.regards.modules.processing.exceptions.ProcessingException;
import fr.cnes.regards.modules.processing.exceptions.ProcessingExceptionType;
import io.vavr.control.Try;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.With;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

/**
 * This class corresponds to all the context for an execution:
 * - its designated process,
 * - its parent batch,
 * - the execution itself,
 * - a set of parameters (free params left to the engine, independent from the batch parameters),
 * - an event notifier to send occurring steps to.
 *
 * @author gandrieu
 */
@Data
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ExecutionContext {

    @With
    PExecution exec;

    PBatch batch;

    PProcess process;

    IExecutionEventNotifier eventNotifier;

    Map<Class<?>, Object> params;

    public ExecutionContext(PExecution exec, PBatch batch, PProcess process, IExecutionEventNotifier notifierFor) {
        this(exec, batch, process, notifierFor, new HashMap<>());
    }

    public Mono<ExecutionContext> sendEvent(ExecutionEvent event) {
        return getEventNotifier().notifyEvent(event).map(this::withExec);
    }

    public <T> ExecutionContext withParam(Class<T> type, T newValue, BiFunction<T, T, T> mergeFn) {
        T mergedValue;
        if (params.containsKey(type)) {
            mergedValue = Try.of(() -> (T) params.get(type))
                             .map(oldValue -> mergeFn.apply(oldValue, newValue))
                             .getOrElse(newValue);

        } else {
            mergedValue = newValue;
        }
        params.put(type, mergedValue);
        return this;
    }

    public <T> ExecutionContext withParam(Class<T> type, T value) {
        params.put(type, value);
        return this;
    }

    public <T> ExecutionContext withParams(Map<Class<?>, Object> paramsToAdd) {
        params.putAll(paramsToAdd);
        return this;
    }

    @SuppressWarnings("unchecked")
    public <T> Mono<T> getParam(Class<T> type) {
        return Try.of(() -> params.get(type))
                  .map(o -> (T) o)
                  .map(Mono::just)
                  .getOrElse(() -> Mono.error(new MissingExecutionContextParameterException("Param for type '"
                                                                                            + type.getSimpleName()
                                                                                            + "' not found")));
    }

    @SuppressWarnings("serial")
    public static class MissingExecutionContextParameterException extends ProcessingException {

        public MissingExecutionContextParameterException(String desc) {
            super(ProcessingExceptionType.MISSING_EXECUTION_CONTEXT_PARAM_ERROR, desc);
        }

        @Override
        public String getMessage() {
            return desc;
        }
    }

}
