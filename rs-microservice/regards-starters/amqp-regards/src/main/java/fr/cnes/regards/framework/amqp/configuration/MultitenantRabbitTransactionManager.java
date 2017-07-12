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
package fr.cnes.regards.framework.amqp.configuration;

import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.transaction.RabbitTransactionManager;
import org.springframework.transaction.TransactionDefinition;

import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;

/**
 *
 * Override {@link RabbitTransactionManager} to manage virtual host in transaction
 *
 * @author Marc Sordi
 *
 */
@SuppressWarnings("serial")
public class MultitenantRabbitTransactionManager extends RabbitTransactionManager {

    /**
     * Resolve current tenant at runtime. The resolver must be thread safe.
     */
    private transient final IRuntimeTenantResolver runtimeTenantResolver;

    /**
     * Virtual host admin
     */
    private transient final IRabbitVirtualHostAdmin rabbitVirtualHostAdmin;

    public MultitenantRabbitTransactionManager(ConnectionFactory pConnectionFactory,
            IRuntimeTenantResolver pRuntimeTenantResolver, IRabbitVirtualHostAdmin pRabbitVirtualHostAdmin) {
        super(pConnectionFactory);
        this.runtimeTenantResolver = pRuntimeTenantResolver;
        this.rabbitVirtualHostAdmin = pRabbitVirtualHostAdmin;
    }

    @Override
    protected void doBegin(Object pTransaction, TransactionDefinition pDefinition) {
        try {
            rabbitVirtualHostAdmin.bind(runtimeTenantResolver.getTenant());
            super.doBegin(pTransaction, pDefinition);
        } finally {
            rabbitVirtualHostAdmin.unbind();
        }
    }
}
