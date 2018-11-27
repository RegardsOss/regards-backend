/*
 * Copyright 2017-2018 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;

import fr.cnes.regards.framework.amqp.AbstractSubscriber;
import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.amqp.configuration.AmqpConstants;
import fr.cnes.regards.framework.amqp.configuration.IAmqpAdmin;
import fr.cnes.regards.framework.amqp.configuration.IRabbitVirtualHostAdmin;
import fr.cnes.regards.framework.amqp.domain.IHandler;
import fr.cnes.regards.framework.amqp.event.EventUtils;
import fr.cnes.regards.framework.amqp.event.ISubscribable;
import fr.cnes.regards.framework.amqp.event.Target;
import fr.cnes.regards.framework.amqp.event.WorkerMode;
import fr.cnes.regards.framework.multitenant.ITenantResolver;

/**
 * Single virtual host subscriber implementation
 * @author Marc Sordi
 */
public class SingleVhostSubscriber extends AbstractSubscriber implements ISubscriber {

    private final ITenantResolver tenantResolver;

    public SingleVhostSubscriber(IRabbitVirtualHostAdmin virtualHostAdmin, IAmqpAdmin amqpAdmin,
            Jackson2JsonMessageConverter jackson2JsonMessageConverter, ITenantResolver tenantResolver) {
        super(virtualHostAdmin, amqpAdmin, jackson2JsonMessageConverter);
        this.tenantResolver = tenantResolver;
    }

    @Override
    protected Set<String> resolveTenants() {
        return tenantResolver.getAllTenants();
    }

    @Override
    protected String resolveVirtualHost(String tenant) {
        return AmqpConstants.AMQP_MULTITENANT_MANAGER;
    }

    @Override
    public void addTenant(String tenant) {
        addTenantListeners(tenant);
    }

    @Override
    public void removeTenant(String tenant) {
        if (listeners != null) {
            for (Map.Entry<Class<?>, Map<String, SimpleMessageListenerContainer>> entry : listeners.entrySet()) {

                Class<?> handlerClass = entry.getKey();
                Class<? extends ISubscribable> eventType = handledEvents.get(handlerClass);
                IHandler<? extends ISubscribable> handler = handlerInstances.get(handlerClass);
                WorkerMode workerMode = EventUtils.getWorkerMode(eventType);
                Target target = EventUtils.getTargetRestriction(eventType);

                // Only useful for UNICAST tenant dependent queues
                if (WorkerMode.UNICAST.equals(workerMode)) {
                    Optional<Class<? extends IHandler<?>>> handlerType =
                            handler == null ? Optional.empty() : Optional.of(handler.getType());
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

}
