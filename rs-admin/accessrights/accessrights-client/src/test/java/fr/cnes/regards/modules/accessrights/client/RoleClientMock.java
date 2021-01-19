/*
 * Copyright 2017-2021 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.accessrights.client;

import java.util.List;
import java.util.Set;

import javax.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.ResponseEntity;

import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.modules.accessrights.domain.projects.Role;

/**
 * @author sbinda
 *
 */
public class RoleClientMock implements IRolesClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(RoleClientMock.class);

    private final MockCounter counter;

    public RoleClientMock(MockCounter counter) {
        super();
        this.counter = counter;
    }

    @Override
    public ResponseEntity<List<EntityModel<Role>>> getAllRoles() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ResponseEntity<List<EntityModel<Role>>> getBorrowableRoles() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ResponseEntity<List<EntityModel<Role>>> getRolesAccesingResource(Long pResourceId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ResponseEntity<EntityModel<Role>> createRole(Role pNewRole) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ResponseEntity<EntityModel<Role>> retrieveRole(String pRoleName) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ResponseEntity<Set<Role>> retrieveRoleDescendants(String roleName) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ResponseEntity<EntityModel<Role>> updateRole(String pRoleName, @Valid Role pUpdatedRole) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ResponseEntity<Void> removeRole(String pRoleName) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ResponseEntity<Boolean> shouldAccessToResourceRequiring(String roleName) throws EntityNotFoundException {
        LOGGER.info("REACHED !!!");
        counter.inc();
        return ResponseEntity.ok(Boolean.TRUE);
    }

}
