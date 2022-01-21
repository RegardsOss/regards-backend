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
package fr.cnes.regards.modules.processing.service.handlers;

import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.modules.processing.domain.PExecution;
import fr.cnes.regards.modules.processing.domain.events.PExecutionResultEvent;
import fr.cnes.regards.modules.processing.domain.exception.ProcessingExecutionException;
import fr.cnes.regards.modules.processing.domain.handlers.IExecutionResultEventSender;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import static fr.cnes.regards.modules.processing.exceptions.ProcessingException.mustWrap;
import static fr.cnes.regards.modules.processing.exceptions.ProcessingExceptionType.SEND_EXECUTION_RESULT_ERROR;
import static fr.cnes.regards.modules.processing.utils.ReactorErrorTransformers.errorWithContextMono;

/**
 * This class defines the sender for execution result events.
 *
 * @author gandrieu
 */
@Component
public class ExecutionResultEventSender implements IExecutionResultEventSender {

    private final IRuntimeTenantResolver runtimeTenantResolver;

    private final IPublisher publisher;

    @Autowired
    public ExecutionResultEventSender(IRuntimeTenantResolver runtimeTenantResolver, IPublisher publisher) {
        this.runtimeTenantResolver = runtimeTenantResolver;
        this.publisher = publisher;
    }

    @Override
    public Mono<PExecutionResultEvent> send(String tenant, PExecutionResultEvent message) {
        return Mono.fromCallable(() -> {
            runtimeTenantResolver.forceTenant(tenant);
            publisher.publish(message);
            return message;
        }).onErrorResume(mustWrap(), errorWithContextMono(PExecution.class, (exec,
                t) -> new SendExecutionResultException(exec, "Sending execution result failed: " + message, t)));
    }

    public static class SendExecutionResultException extends ProcessingExecutionException {

        public SendExecutionResultException(PExecution exec, String message, Throwable throwable) {
            super(SEND_EXECUTION_RESULT_ERROR, exec, message, throwable);
        }
    }
}
