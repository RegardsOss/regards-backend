/*
 * Copyright 2017-2024 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.framework.amqp.single;

import fr.cnes.regards.framework.amqp.AbstractSubscriber;
import fr.cnes.regards.framework.amqp.IInstancePublisher;
import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.amqp.configuration.*;
import fr.cnes.regards.framework.amqp.event.EventUtils;
import fr.cnes.regards.framework.amqp.event.Target;
import fr.cnes.regards.framework.amqp.event.WorkerMode;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.multitenant.ITenantResolver;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

/**
 * Single virtual host subscriber implementation
 *
 * @author Marc Sordi
 */
public class SingleVhostSubscriber extends AbstractSubscriber implements ISubscriber {

    private final ITenantResolver tenantResolver;

    public SingleVhostSubscriber(IRabbitVirtualHostAdmin virtualHostAdmin,
                                 RabbitTemplate rabbitTemplate,
                                 IAmqpAdmin amqpAdmin,
                                 MessageConverter jsonMessageConverters,
                                 ITenantResolver tenantResolver,
                                 RegardsErrorHandler errorHandler,
                                 String microserviceName,
                                 IInstancePublisher instancePublisher,
                                 IPublisher publisher,
                                 IRuntimeTenantResolver runtimeTenantResolver,
                                 ApplicationEventPublisher applicationEventPublisher,
                                 int declarationRetries,
                                 long failedDeclarationRetryInterval,
                                 RetryProperties retryProperties,
                                 TransactionTemplate transactionTemplate) {
        super(virtualHostAdmin,
              amqpAdmin,
              jsonMessageConverters,
              errorHandler,
              microserviceName,
              instancePublisher,
              publisher,
              runtimeTenantResolver,
              tenantResolver,
              rabbitTemplate,
              transactionTemplate,
              applicationEventPublisher,
              declarationRetries,
              failedDeclarationRetryInterval,
              retryProperties);
        this.tenantResolver = tenantResolver;
    }

    @Override
    protected Set<String> resolveTenants() {
        return tenantResolver.getAllTenants();
    }

    @Override
    protected String resolveVirtualHost(String tenant) {
        return AmqpChannel.AMQP_MULTITENANT_MANAGER;
    }

    @Override
    public void addTenant(String tenant) {
        addTenantListeners(tenant);
    }

    @Override
    public void removeTenant(String tenant) {
        for (Map.Entry<String, ConcurrentMap<String, SimpleMessageListenerContainer>> entry : listeners.entrySet()) {

            String handlerClassName = entry.getKey();
            Class<?> eventType = handledEvents.get(handlerClassName);
            WorkerMode workerMode = EventUtils.getWorkerMode(eventType);
            Target target = EventUtils.getTargetRestriction(eventType);

            // Only useful for UNICAST tenant dependent queues
            if (WorkerMode.UNICAST.equals(workerMode)) {
                String queueNameToRemove = amqpAdmin.getUnicastQueueName(tenant, eventType, target);
                String virtualHost = resolveVirtualHost(tenant);

                Map<String, SimpleMessageListenerContainer> vhostsContainers = entry.getValue();
                SimpleMessageListenerContainer container = vhostsContainers.get(virtualHost);
                container.removeQueueNames(queueNameToRemove);
            }
            // Nothing to do for BROADCAST
        }
    }

}
