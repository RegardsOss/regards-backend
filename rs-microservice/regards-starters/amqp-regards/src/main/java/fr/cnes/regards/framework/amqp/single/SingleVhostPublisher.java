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
package fr.cnes.regards.framework.amqp.single;

import com.google.gson.Gson;
import fr.cnes.regards.framework.amqp.AbstractPublisher;
import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.amqp.configuration.AmqpChannel;
import fr.cnes.regards.framework.amqp.configuration.IAmqpAdmin;
import fr.cnes.regards.framework.amqp.configuration.IRabbitVirtualHostAdmin;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.List;

/**
 * Single virtual host publisher implementation
 *
 * @author Marc Sordi
 */
public class SingleVhostPublisher extends AbstractPublisher implements IPublisher {

    private final IRuntimeTenantResolver threadTenantResolver;

    public SingleVhostPublisher(String applicationId,
                                RabbitTemplate rabbitTemplate,
                                RabbitAdmin rabbitAdmin,
                                IAmqpAdmin amqpAdmin,
                                IRabbitVirtualHostAdmin rabbitVirtualHostAdmin,
                                IRuntimeTenantResolver threadTenantResolver,
                                Gson gson,
                                List<String> eventsToNotifier) {
        super(rabbitTemplate, rabbitAdmin, amqpAdmin, rabbitVirtualHostAdmin, applicationId, gson, eventsToNotifier);
        this.threadTenantResolver = threadTenantResolver;
    }

    @Override
    protected String resolveTenant() {
        return threadTenantResolver.getTenant();
    }

    @Override
    protected String resolveVirtualHost(String tenant) {
        return AmqpChannel.AMQP_MULTITENANT_MANAGER;
    }
}
