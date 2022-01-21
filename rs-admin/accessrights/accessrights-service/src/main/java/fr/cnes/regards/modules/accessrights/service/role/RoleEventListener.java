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
package fr.cnes.regards.modules.accessrights.service.role;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.support.ListenerExecutionFailedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import fr.cnes.regards.framework.amqp.IInstancePublisher;
import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.jpa.multitenant.event.TenantConnectionFailed;
import fr.cnes.regards.framework.jpa.multitenant.event.spring.TenantConnectionReady;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.security.event.ResourceAccessInit;

/**
 * Listener for {@link IRoleService} on a new {@link TenantConnectionReady}.
 * Initialize the default Roles for a new tenant.
 * @author Sébastien Binda
 */
@Component
public class RoleEventListener {

    /**
     * Class logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(RoleEventListener.class);

    @Value("${spring.application.name}")
    private String microserviceName;

    @Autowired
    private IRoleService roleService;

    /**
     * AMQP tenant publisher
     */
    @Autowired
    private IPublisher publisher;

    /**
     * AMQP instance message publisher
     */
    @Autowired
    private IInstancePublisher instancePublisher;

    /**
     * {@link IRuntimeTenantResolver} instance
     */
    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    /**
     * {@link Order} : Needs to be initialize before other bean to creates default roles for new tenant.
     * @param event
     */
    @EventListener
    @Order(0)
    public void processEvent(TenantConnectionReady event) {
        try {
            // Init default role for this tenant
            runtimeTenantResolver.forceTenant(event.getTenant());
            roleService.initDefaultRoles();
            // Populate default roles with resources informing security starter to process
            publisher.publish(new ResourceAccessInit());
        } catch (final ListenerExecutionFailedException e) {
            LOGGER.error("Cannot initialize connection  for tenant " + event.getTenant(), e);
            instancePublisher.publish(new TenantConnectionFailed(event.getTenant(), microserviceName));
        } finally {
            runtimeTenantResolver.clearTenant();
        }
    }

}
