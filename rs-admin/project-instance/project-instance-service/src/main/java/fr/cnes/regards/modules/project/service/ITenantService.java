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
package fr.cnes.regards.modules.project.service;

import fr.cnes.regards.modules.project.dao.IProjectRepository;

import java.util.Set;

/**
 * This interface is used to retrieve all tenants from {@link IProjectRepository}.<br/>
 * The advantage of this service is that it only depends on the single repository and no other bean that may cause a
 * circular dependency.<br/>
 * Thus, a circular dependency may occurs at starting point when a starter or module try to retrieve all tenants to
 * initialize itself.
 *
 * @author Marc Sordi
 */
public interface ITenantService {

    /**
     * @return all tenant managed by the current instance. Tenants are equivalents to projects.
     */
    Set<String> getAllTenants();

    /**
     * @return all tenant managed by the current instance and fully configured. Tenants are equivalents to projects.
     */
    Set<String> getAllActiveTenants(String pMicroserviceName);
}
