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
package fr.cnes.regards.modules.access.services.client;

import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.modules.accessrights.client.IRolesClient;
import fr.cnes.regards.modules.accessrights.domain.projects.Role;
import org.springframework.context.annotation.Primary;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import javax.validation.Valid;
import java.util.List;
import java.util.Set;

/**
 * Provide a bean to replace the behavior of the {@link IRolesClient} while testing
 *
 * @author Léo Mieulet
 */
@Component
@Primary
public class RolesClientMock implements IRolesClient {

    @Override
    public ResponseEntity<List<EntityModel<Role>>> getAllRoles() {
        return null;
    }

    @Override
    public ResponseEntity<List<EntityModel<Role>>> getBorrowableRoles() {
        return null;
    }

    @Override
    public ResponseEntity<List<EntityModel<Role>>> getRolesAccesingResource(Long pResourceId) {
        return null;
    }

    @Override
    public ResponseEntity<EntityModel<Role>> createRole(Role pNewRole) {
        return null;
    }

    @Override
    public ResponseEntity<EntityModel<Role>> retrieveRole(String pRoleName) {
        return null;
    }

    @Override
    public ResponseEntity<Set<Role>> retrieveRoleDescendants(String roleName) {
        return null;
    }

    @Override
    public ResponseEntity<EntityModel<Role>> updateRole(String pRoleName, @Valid Role pUpdatedRole) {
        return null;
    }

    @Override
    public ResponseEntity<Void> removeRole(String pRoleName) {
        return null;
    }

    @Override
    public ResponseEntity<Boolean> shouldAccessToResourceRequiring(String roleName) throws EntityNotFoundException {
        return null;
    }
}
