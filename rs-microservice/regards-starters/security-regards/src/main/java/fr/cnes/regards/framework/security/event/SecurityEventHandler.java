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
package fr.cnes.regards.framework.security.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;

import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.amqp.domain.IHandler;
import fr.cnes.regards.framework.amqp.domain.TenantWrapper;
import fr.cnes.regards.framework.security.domain.SecurityException;
import fr.cnes.regards.framework.security.endpoint.MethodAuthorizationService;

/**
 * This class manages multitenant security event workflow
 *
 * @author Marc Sordi
 *
 */
public class SecurityEventHandler implements ApplicationListener<ApplicationReadyEvent> {

    /**
     * Class logger
     */
    @SuppressWarnings("unused")
    private static final Logger LOGGER = LoggerFactory.getLogger(SecurityEventHandler.class);

    /**
     * Current microservice
     */
    private final String microservice;

    /**
     * Tenant subscriber
     */
    private final ISubscriber subscriber;

    /**
     * Security manager
     */
    private final MethodAuthorizationService methodAuthorizationService;

    public SecurityEventHandler(String microservice, final ISubscriber subscriber,
            final MethodAuthorizationService methodAuthorizationService) {
        this.microservice = microservice;
        this.subscriber = subscriber;
        this.methodAuthorizationService = methodAuthorizationService;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.springframework.context.ApplicationListener#onApplicationEvent(org.springframework.context.ApplicationEvent)
     */
    @Override
    public void onApplicationEvent(ApplicationReadyEvent pEvent) {
        // Listen to resource event (creation, deletion, update)
        subscriber.subscribeTo(ResourceAccessEvent.class, new ResourceAccessUpdateHandler());
        // Listen to new tenant resource initialization
        subscriber.subscribeTo(ResourceAccessInit.class, new ResourceAccessInitHandler());
        // Listen to role event (creation, deletion, update)
        subscriber.subscribeTo(RoleEvent.class, new RoleEventHandler());
    }

    /**
     * Handle {@link ResourceAccessEvent} event to refresh security cache
     *
     * @author Marc Sordi
     *
     */
    private class ResourceAccessUpdateHandler implements IHandler<ResourceAccessEvent> {

        @Override
        public void handle(TenantWrapper<ResourceAccessEvent> pWrapper) {
            String tenant = pWrapper.getTenant();
            ResourceAccessEvent event = pWrapper.getContent();

            // Only manage event for the concerned microservice
            if ((event != null) && microservice.equals(event.getMicroservice())) {
                methodAuthorizationService.updateAuthoritiesFor(tenant, event.getRoleName());
            }
        }
    }

    /**
     * Handle {@link ResourceAccessInit} event to register default resource access for a new tenant
     *
     * @author Marc Sordi
     *
     */
    private class ResourceAccessInitHandler implements IHandler<ResourceAccessInit> {

        @Override
        public void handle(TenantWrapper<ResourceAccessInit> pWrapper) {
            try {
                methodAuthorizationService.manageTenant(pWrapper.getTenant());
            } catch (SecurityException e) {
                LOGGER.error("Microservice resource cannot be register for tenant {} and microservice {}",
                             pWrapper.getTenant(), microservice);
                LOGGER.error(e.getMessage(), e);
            }

        }
    }

    /**
     * Handle {@link RoleEvent} to refresh security cache
     *
     * @author Marc Sordi
     *
     */
    private class RoleEventHandler implements IHandler<RoleEvent> {

        @Override
        public void handle(TenantWrapper<RoleEvent> pWrapper) {
            try {
                methodAuthorizationService.collectRolesAndAuthorities(pWrapper.getTenant());
            } catch (SecurityException e) {
                LOGGER.error("Security cache cannot be refresh for role {}, tenant {} and microservice {}",
                             pWrapper.getContent().getRole(), pWrapper.getTenant(), microservice);
                LOGGER.error(e.getMessage(), e);
            }
        }

    }

}
