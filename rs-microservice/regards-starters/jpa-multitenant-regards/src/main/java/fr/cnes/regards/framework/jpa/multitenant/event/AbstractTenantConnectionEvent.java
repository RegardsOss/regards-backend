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

import fr.cnes.regards.framework.jpa.multitenant.properties.TenantConnection;

/**
 * Common {@link TenantConnection} event structure
 * @author Marc Sordi
 */
public abstract class AbstractTenantConnectionEvent {

    /**
     * New tenant
     */
    private TenantConnection tenant;

    /**
     * Microservice target of this message
     */
    private String microserviceName;

    public AbstractTenantConnectionEvent() {
        super();
    }

    public AbstractTenantConnectionEvent(final TenantConnection pTenant, final String pMicroserviceName) {
        super();
        tenant = pTenant;
        microserviceName = pMicroserviceName;
    }

    public String getMicroserviceName() {
        return microserviceName;
    }

    public TenantConnection getTenant() {
        return tenant;
    }

    public void setMicroserviceName(final String pMicroserviceName) {
        microserviceName = pMicroserviceName;
    }

    public void setTenant(final TenantConnection pTenant) {
        tenant = pTenant;
    }

}
