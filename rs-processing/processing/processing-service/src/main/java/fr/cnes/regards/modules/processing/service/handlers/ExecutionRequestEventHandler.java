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

import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.amqp.domain.IHandler;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.modules.processing.domain.events.PExecutionRequestEvent;
import fr.cnes.regards.modules.processing.domain.service.IExecutionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * This class defines the handler for execution request events.
 *
 * @author gandrieu
 */
@Component
public class ExecutionRequestEventHandler
    implements ApplicationListener<ApplicationReadyEvent>, IHandler<PExecutionRequestEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExecutionRequestEventHandler.class);

    private final IRuntimeTenantResolver runtimeTenantResolver;

    private final ISubscriber subscriber;

    private final IExecutionService execService;

    @Autowired
    public ExecutionRequestEventHandler(IRuntimeTenantResolver runtimeTenantResolver,
                                        ISubscriber subscriber,
                                        IExecutionService execService) {
        this.runtimeTenantResolver = runtimeTenantResolver;
        this.subscriber = subscriber;
        this.execService = execService;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        subscriber.subscribeTo(PExecutionRequestEvent.class, this);
    }

    @Override
    public void handle(String tenant, PExecutionRequestEvent message) {
        runtimeTenantResolver.forceTenant(tenant); // Needed in order to publish events

        String execCid = message.getExecutionCorrelationId();
        LOGGER.info("exec={} - Execution request received", execCid);

        execService.launchExecution(message)
                   .switchIfEmpty(Mono.defer(() -> Mono.error(new RuntimeException(
                       "PExecutionRequestEvent yielded an empty result: " + message))))
                   .subscribe(exec -> LOGGER.info("exec={} - Execution request registered correctly", execCid),
                              err -> LOGGER.error("exec={} - Execution request error: {}", execCid, err.getMessage()));
    }
}
