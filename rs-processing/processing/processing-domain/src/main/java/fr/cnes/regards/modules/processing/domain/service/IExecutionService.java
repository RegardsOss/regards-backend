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
package fr.cnes.regards.modules.processing.domain.service;

import fr.cnes.regards.modules.processing.domain.PExecution;
import fr.cnes.regards.modules.processing.domain.events.PExecutionRequestEvent;
import fr.cnes.regards.modules.processing.domain.execution.ExecutionContext;
import reactor.core.publisher.Mono;

import java.util.UUID;

import static fr.cnes.regards.modules.processing.utils.LogUtils.setOrderIdInMdc;

/**
 * This interface defines a service contract for {@link PExecution} entities.
 *
 * @author gandrieu
 */
public interface IExecutionService {

    Mono<PExecution> launchExecution(PExecutionRequestEvent request);

    void scheduledTimeoutNotify();

    Mono<ExecutionContext> createContext(UUID execId);

    default Mono<PExecution> runExecutable(UUID execId) {
        return createContext(execId).flatMap(ctx -> {
            String correlationId = ctx.getExec().getBatchCorrelationId();
            setOrderIdInMdc(correlationId);

            return ctx.getProcess().getExecutable().execute(ctx);
        }).map(ExecutionContext::getExec);
    }

}
