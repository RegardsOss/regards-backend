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
package fr.cnes.regards.microservices.administration;

import java.util.List;
import java.util.Optional;

import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.http.ResponseEntity;

import fr.cnes.regards.framework.feign.security.FeignSecurityManager;
import fr.cnes.regards.framework.jpa.multitenant.exception.JpaMultitenantException;
import fr.cnes.regards.framework.jpa.multitenant.properties.TenantConnection;
import fr.cnes.regards.framework.jpa.multitenant.properties.TenantConnectionState;
import fr.cnes.regards.framework.jpa.multitenant.resolver.ITenantConnectionResolver;
import fr.cnes.regards.modules.project.client.rest.ITenantConnectionClient;

/**
 *
 * Class DefaultMultitenantConnectionsReader
 *
 * Default tenants connections configuration reader. Reads tenants from the microservice "rs-admin". Enabled, only if
 * the microservice is Eureka client.
 *
 * @author Sébastien Binda
 * @author Marc Sordi
 * @since 1.0-SNAPSHOT
 */
public class RemoteTenantConnectionResolver extends AbstractInstanceDiscoveryClientChecker
        implements ITenantConnectionResolver {

    /**
     * Tenant connection client
     */
    private final ITenantConnectionClient tenantConnectionClient;

    public RemoteTenantConnectionResolver(final DiscoveryClient discoveryClient,
            ITenantConnectionClient tenantConnectionClient) {
        super(discoveryClient);
        this.tenantConnectionClient = tenantConnectionClient;
    }

    @Override
    public List<TenantConnection> getTenantConnections(String microserviceName) throws JpaMultitenantException {

        try {
            FeignSecurityManager.asSystem();
            ResponseEntity<List<TenantConnection>> response = tenantConnectionClient
                    .getTenantConnections(microserviceName);
            return response.getBody();
        } finally {
            FeignSecurityManager.reset();
        }
    }

    @Override
    public void addTenantConnection(String microserviceName, final TenantConnection pTenantConnection)
            throws JpaMultitenantException {
        try {
            FeignSecurityManager.asSystem();
            tenantConnectionClient.addTenantConnection(microserviceName, pTenantConnection);
        } finally {
            FeignSecurityManager.reset();
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see fr.cnes.regards.framework.jpa.multitenant.resolver.ITenantConnectionResolver#updateState(java.lang.String,
     * java.lang.String, fr.cnes.regards.framework.jpa.multitenant.properties.TenantConnectionState, java.util.Optional)
     */
    @Override
    public void updateState(String microservice, String tenant, TenantConnectionState state,
            Optional<String> errorCause) throws JpaMultitenantException {
        try {
            FeignSecurityManager.asSystem();
            TenantConnection connection = new TenantConnection();
            connection.setTenant(tenant);
            connection.setState(state);
            connection.setErrorCause(errorCause.orElse(null));
            tenantConnectionClient.updateState(microservice, connection);
        } finally {
            FeignSecurityManager.reset();
        }
    }

}
