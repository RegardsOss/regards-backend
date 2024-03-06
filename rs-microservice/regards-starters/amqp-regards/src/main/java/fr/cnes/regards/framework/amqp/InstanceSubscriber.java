/*
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import fr.cnes.regards.framework.amqp.configuration.*;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.HashSet;
import java.util.Set;

/**
 * {@link InstanceSubscriber} uses a fixed tenant to subscribe to instance events.
 *
 * @author Marc Sordi
 */
public class InstanceSubscriber extends AbstractSubscriber implements IInstanceSubscriber {

    public InstanceSubscriber(IRabbitVirtualHostAdmin pVirtualHostAdmin,
                              RabbitTemplate rabbitTemplate,
                              TransactionTemplate transactionTemplate,
                              IAmqpAdmin amqpAdmin,
                              MessageConverter jsonMessageConverters,
                              RegardsErrorHandler errorHandler,
                              String microserviceName,
                              IInstancePublisher instancePublisher,
                              IPublisher publisher,
                              IRuntimeTenantResolver runtimeTenantResolver,
                              ApplicationEventPublisher applicationEventPublisher,
                              int declarationRetries,
                              long failedDeclarationRetryInterval,
                              RetryProperties retryProperties) {
        super(pVirtualHostAdmin,
              amqpAdmin,
              jsonMessageConverters,
              errorHandler,
              microserviceName,
              instancePublisher,
              publisher,
              runtimeTenantResolver,
              null,
              rabbitTemplate,
              transactionTemplate,
              applicationEventPublisher,
              declarationRetries,
              failedDeclarationRetryInterval,
              retryProperties);
    }

    @Override
    protected Set<String> resolveTenants() {
        // Instance is considered as a single tenant
        Set<String> tenants = new HashSet<>();
        tenants.add(AmqpChannel.INSTANCE_TENANT);
        return tenants;
    }

    @Override
    protected String resolveVirtualHost(String tenant) {
        return AmqpChannel.AMQP_INSTANCE_MANAGER;
    }
}
