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
package fr.cnes.regards.framework.jpa.multitenant.resolver;

import java.util.List;

import fr.cnes.regards.framework.jpa.multitenant.exception.JpaMultitenantException;
import fr.cnes.regards.framework.jpa.multitenant.properties.TenantConnection;

/**
 *
 * Class IMultitenantConnectionsReader
 *
 * Interface to create a custom datasources configuration reader. All datasources returned by the method getDataSources
 * are managed by regards multitenancy jpa.
 *
 * @author Sébastien Binda
 * @since 1.0-SNAPSHOT
 */
public interface ITenantConnectionResolver {

    /**
     *
     * Retrieve all <b>enabled connection configuration</b> for each tenant of the specified microservice
     *
     * @param microserviceName
     *            related microservice
     * @return List of existing {@link TenantConnection}
     * @since 1.0-SNAPSHOT
     */
    List<TenantConnection> getTenantConnections(String microserviceName) throws JpaMultitenantException;

    /**
     *
     * Add a new tenant connection
     *
     * @param microserviceName
     *            related microservice
     * @param pTenantConnection
     *            tenant connection for specified microservice
     * @throws JpaMultitenantException
     *             implementation exception
     */
    void addTenantConnection(String microserviceName, TenantConnection tenantConnection) throws JpaMultitenantException;

    /**
     * Enable a tenant connection
     *
     * @param microserviceName
     *            related microservice
     * @param tenant
     *            related tenant
     * @throws JpaMultitenantException
     *             implementation exception
     */
    void enableTenantConnection(String microserviceName, String tenant) throws JpaMultitenantException;

    /**
     * Disable a tenant connection
     *
     * @param microserviceName
     *            related microservice
     * @param tenant
     *            related tenant
     * @throws JpaMultitenantException
     *             implementation exception
     */
    void disableTenantConnection(String microserviceName, String tenant) throws JpaMultitenantException;
}
