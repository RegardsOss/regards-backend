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
package fr.cnes.regards.framework.jpa.multitenant.event;

import fr.cnes.regards.framework.amqp.event.Event;
import fr.cnes.regards.framework.amqp.event.ISubscribable;
import fr.cnes.regards.framework.amqp.event.Target;
import fr.cnes.regards.framework.jpa.multitenant.event.spring.TenantConnectionDiscarded;
import fr.cnes.regards.framework.jpa.multitenant.properties.TenantConnection;

/**
 *
 *
 * Event that informs a tenant connection is delete and has to be remove from JPA management.<br/>
 * This event must only be handled by the starter in each microservice instance. When the connection is deleted, the
 * starter sends to all microservice instances of the current type a {@link TenantConnectionDiscarded}.
 *
 * @author Sébastien Binda
 * @since 1.0-SNAPSHOT
 */
@Event(target = Target.ALL)
public class TenantConnectionConfigurationDeleted extends AbstractTenantConnectionEvent implements ISubscribable {

    public TenantConnectionConfigurationDeleted() {
        super();
    }

    public TenantConnectionConfigurationDeleted(final TenantConnection pTenant, final String pMicroserviceName) {
        super(pTenant, pMicroserviceName);
    }
}
