/*
 * Copyright 2017 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;

import fr.cnes.regards.framework.amqp.configuration.IRabbitVirtualHostAdmin;
import fr.cnes.regards.framework.amqp.configuration.RegardsAmqpAdmin;
import fr.cnes.regards.framework.multitenant.ITenantResolver;

/**
 *
 * {@link Subscriber} uses {@link ITenantResolver} to resolve tenants in multitenant context. On listener will be
 * created for each tenant.
 *
 * @author svissier
 * @author Marc Sordi
 *
 */
public class Subscriber extends AbstractSubscriber implements ISubscriber {

    /**
     * Class logger
     */
    @SuppressWarnings("unused")
    private static final Logger LOGGER = LoggerFactory.getLogger(Subscriber.class);

    /**
     * provider of projects allowing us to listen to any necessary RabbitMQ Vhost
     */
    private final ITenantResolver tenantResolver;

    public Subscriber(IRabbitVirtualHostAdmin pVirtualHostAdmin, RegardsAmqpAdmin pRegardsAmqpAdmin,
            Jackson2JsonMessageConverter pJackson2JsonMessageConverter, ITenantResolver pTenantResolver) {
        super(pVirtualHostAdmin, pRegardsAmqpAdmin, pJackson2JsonMessageConverter);
        tenantResolver = pTenantResolver;
    }

    @Override
    protected Set<String> resolveTenants() {
        return tenantResolver.getAllTenants();
    }

    @Override
    public void addTenant(String pTenant) {
        addTenantListeners(pTenant);
    }

    @Override
    public void removeTenant(String pTenant) {
        removeTenantListeners(pTenant);
    }

}
