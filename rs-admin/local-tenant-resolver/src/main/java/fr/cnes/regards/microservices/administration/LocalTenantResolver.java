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
package fr.cnes.regards.microservices.administration;

import fr.cnes.regards.framework.multitenant.ITenantResolver;
import fr.cnes.regards.modules.project.service.ITenantService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.util.Set;

/**
 * Class LocalTenantResolver
 * <p>
 * Administration microservice local tenant resolver.
 *
 * @author CS
 */
public class LocalTenantResolver implements ITenantResolver {

    /**
     * Microservice name
     */
    @Value("${spring.application.name}")
    private String microserviceName;

    /**
     * Administration project service
     */
    @Autowired
    private ITenantService tenantService;

    @Override
    public Set<String> getAllTenants() {
        return tenantService.getAllTenants();
    }

    @Override
    public Set<String> getAllActiveTenants() {
        return tenantService.getAllActiveTenants(microserviceName);
    }
}
