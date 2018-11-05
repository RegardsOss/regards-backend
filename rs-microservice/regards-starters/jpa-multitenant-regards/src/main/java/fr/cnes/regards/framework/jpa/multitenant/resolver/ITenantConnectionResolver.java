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
package fr.cnes.regards.framework.jpa.multitenant.resolver;

import java.util.List;
import java.util.Optional;

import fr.cnes.regards.framework.jpa.multitenant.exception.JpaMultitenantException;
import fr.cnes.regards.framework.jpa.multitenant.properties.TenantConnection;
import fr.cnes.regards.framework.jpa.multitenant.properties.TenantConnectionState;

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
     * @param microservice
     *            related microservice
     * @return List of existing {@link TenantConnection}
     * @since 1.0-SNAPSHOT
     */
    List<TenantConnection> getTenantConnections(String microservice) throws JpaMultitenantException;

    /**
     *
     * Add a new tenant connection
     *
     * @param microservice
     *            related microservice
     * @param tenantConnection
     *            tenant connection for specified microservice
     * @throws JpaMultitenantException
     *             implementation exception
     */
    void addTenantConnection(String microservice, TenantConnection tenantConnection) throws JpaMultitenantException;

    /**
     * Update connection state giving optional error cause
     * @param microservice target microservice
     * @param tenant target tenant
     * @param state new connection state
     * @param errorCause optional error cause (useful when {@link TenantConnectionState#ERROR}!)
     * @return updated connection
     */
    void updateState(String microservice, String tenant, TenantConnectionState state, Optional<String> errorCause)
            throws JpaMultitenantException;
}
