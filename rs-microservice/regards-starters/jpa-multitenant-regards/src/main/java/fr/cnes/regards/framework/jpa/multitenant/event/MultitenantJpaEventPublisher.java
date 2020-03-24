/*
 * Copyright 2017-2020 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.framework.jpa.multitenant.event;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;

import fr.cnes.regards.framework.jpa.multitenant.event.spring.TenantConnectionDiscarded;
import fr.cnes.regards.framework.jpa.multitenant.event.spring.TenantConnectionReady;

/**
 * This class manages JPA event workflow on local microservice using Spring events
 * @author Marc Sordi
 */
public class MultitenantJpaEventPublisher implements ApplicationEventPublisherAware {

    private ApplicationEventPublisher publisher;

    @Override
    public void setApplicationEventPublisher(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    public void publishConnectionReady(String tenant) {
        TenantConnectionReady event = new TenantConnectionReady(this, tenant);
        publisher.publishEvent(event);
    }

    public void publishConnectionDiscarded(String tenant) {
        TenantConnectionDiscarded event = new TenantConnectionDiscarded(this, tenant);
        publisher.publishEvent(event);
    }
}
