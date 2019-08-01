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
package fr.cnes.regards.framework.jpa.multitenant.event;

import fr.cnes.regards.framework.amqp.event.Event;
import fr.cnes.regards.framework.amqp.event.ISubscribable;
import fr.cnes.regards.framework.amqp.event.Target;

/**
 * Event that informs a tenant connection fail and has to be disabled by JPA multitenant starter.<br/>
 * This event must only be handled by the starter in each microservice instance.
 * @author Marc Sordi
 */
@Event(target = Target.ALL)
public class TenantConnectionFailed extends AbstractTenantEvent implements ISubscribable {

    public TenantConnectionFailed() {
        // JSON constructor
    }

    public TenantConnectionFailed(String tenant, String microserviceName) {
        super(tenant, microserviceName);
    }
}
