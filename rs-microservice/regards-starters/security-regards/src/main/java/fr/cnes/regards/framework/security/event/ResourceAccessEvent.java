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

import fr.cnes.regards.framework.amqp.event.Event;
import fr.cnes.regards.framework.amqp.event.ISubscribable;
import fr.cnes.regards.framework.amqp.event.Target;

/**
 * This event must be sent when one or more resource access configuration change so security cache needs to be
 * refreshed.
 * @author Marc Sordi
 */
@Event(target = Target.ALL)
public class ResourceAccessEvent implements ISubscribable {

    /**
     * Name of the concerned microservice
     */
    private String microservice;

    /**
     * Concerned role name
     */
    private String roleName;

    /**
     * for serialization needs
     */
    private ResourceAccessEvent() {
    }

    /**
     * Constructor setting the parameters as attributes
     */
    public ResourceAccessEvent(String microservice, String roleName) {
        this.microservice = microservice;
        this.roleName = roleName;
    }

    /**
     * @return the role name
     */
    public String getRoleName() {
        return roleName;
    }

    /**
     * Set the role name
     */
    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    /**
     * @return the microservice
     */
    public String getMicroservice() {
        return microservice;
    }

    /**
     * Set the microservice
     */
    public void setMicroservice(String pMicroservice) {
        microservice = pMicroservice;
    }
}
