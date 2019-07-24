/*
 * Copyright 2017-2019 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.framework.amqp.configuration;

import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.transaction.RabbitTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.util.Assert;

import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;

/**
 * Override {@link RabbitTransactionManager} to manage virtual host in transaction
 * @author Marc Sordi
 */
@SuppressWarnings("serial")
public class MultitenantRabbitTransactionManager extends RabbitTransactionManager {

    private final VirtualHostMode mode;

    /**
     * Resolve current tenant at runtime. The resolver must be thread safe.
     */
    private final transient IRuntimeTenantResolver runtimeTenantResolver;

    /**
     * Virtual host admin
     */
    private final transient IRabbitVirtualHostAdmin rabbitVirtualHostAdmin;

    public MultitenantRabbitTransactionManager(VirtualHostMode mode, ConnectionFactory connectionFactory,
            IRuntimeTenantResolver pRuntimeTenantResolver, IRabbitVirtualHostAdmin rabbitVirtualHostAdmin) {
        super(connectionFactory);
        this.mode = mode;
        this.runtimeTenantResolver = pRuntimeTenantResolver;
        this.rabbitVirtualHostAdmin = rabbitVirtualHostAdmin;
    }

    @Override
    protected void doBegin(Object transaction, TransactionDefinition definition) {
        String virtualHost = AmqpConstants.AMQP_MULTITENANT_MANAGER;
        if (VirtualHostMode.MULTI.equals(mode)) {
            virtualHost = RabbitVirtualHostAdmin.getVhostName(runtimeTenantResolver.getTenant());
        }

        try {
            rabbitVirtualHostAdmin.bind(virtualHost);
            super.doBegin(transaction, definition);
        } finally {
            rabbitVirtualHostAdmin.unbind();
        }
    }

    /**
     * Temproary fix waiting for spring amq fix issue : 
     * https://github.com/spring-projects/spring-amqp/issues/883
     */
    @Override
    public void afterPropertiesSet() {
        Assert.notNull(getConnectionFactory(), "Property 'connectionFactory' is required");
    }

}
