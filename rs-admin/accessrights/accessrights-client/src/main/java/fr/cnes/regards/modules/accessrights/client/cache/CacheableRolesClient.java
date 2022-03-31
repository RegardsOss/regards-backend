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
package fr.cnes.regards.modules.accessrights.client.cache;

import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.modules.accessrights.client.IRolesClient;
import fr.cnes.regards.modules.accessrights.domain.projects.Role;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.ResponseEntity;

/**
 * Wrapper to IRolesClient (Fiegn client) to add a cache.
 *
 * @author Binda Sébastien
 */
public class CacheableRolesClient {

    private IRolesClient rolesClient;

    public CacheableRolesClient(IRolesClient rolesClient) {
        this.rolesClient = rolesClient;
    }

    /**
     * Define the endpoint to determine if the provided ${@link Role} is inferior to the one brought by the current request
     * @param roleName that should be inferior
     * @return true when the current role should have access to something requiring at least the provided role
     * @throws EntityNotFoundException if some role does not exists
     */
    @Cacheable(cacheNames = RolesHierarchyKeyGenerator.CACHE_NAME,
        keyGenerator = RolesHierarchyKeyGenerator.KEY_GENERATOR, sync = true)
    public ResponseEntity<Boolean> shouldAccessToResourceRequiring(String roleName)
        throws EntityNotFoundException {
        return rolesClient.shouldAccessToResourceRequiring(roleName);
    }

}
