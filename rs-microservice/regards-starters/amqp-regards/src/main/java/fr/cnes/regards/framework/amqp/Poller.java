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
package fr.cnes.regards.framework.amqp;

import org.springframework.amqp.rabbit.core.RabbitTemplate;

import fr.cnes.regards.framework.amqp.configuration.IAmqpAdmin;
import fr.cnes.regards.framework.amqp.configuration.IRabbitVirtualHostAdmin;
import fr.cnes.regards.framework.amqp.configuration.RabbitVirtualHostAdmin;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;

/**
 * {@link Poller} uses {@link IRuntimeTenantResolver} to resolve current thread tenant to poll an event in the
 * multitenant context.
 *
 * @author svissier
 * @author Marc Sordi
 *
 */
public class Poller extends AbstractPoller implements IPoller {

    /**
     * Resolve thread tenant
     */
    private final IRuntimeTenantResolver threadTenantResolver;

    public Poller(IRabbitVirtualHostAdmin pVirtualHostAdmin, RabbitTemplate rabbitTemplate, IAmqpAdmin amqpAdmin,
            IRuntimeTenantResolver pThreadTenantResolver) {
        super(pVirtualHostAdmin, rabbitTemplate, amqpAdmin);
        this.threadTenantResolver = pThreadTenantResolver;
    }

    @Override
    protected String resolveTenant() {
        return threadTenantResolver.getTenant();
    }

    @Override
    protected String resolveVirtualHost(String tenant) {
        return RabbitVirtualHostAdmin.getVhostName(tenant);
    }
}