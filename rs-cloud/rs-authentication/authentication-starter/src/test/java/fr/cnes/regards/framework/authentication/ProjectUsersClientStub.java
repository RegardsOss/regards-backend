/*
 * Copyright 2017-2020 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.framework.authentication;

import java.util.ArrayList;
import java.util.List;

import org.springframework.context.annotation.Primary;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.hateoas.PagedModel.PageMetadata;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import fr.cnes.regards.framework.hateoas.HateoasUtils;
import fr.cnes.regards.modules.accessrights.client.IProjectUsersClient;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;
import fr.cnes.regards.modules.accessrights.domain.registration.AccessRequestDto;

/**
 * Class ProjectUsersClientStub
 *
 * Stub to simulate the responses from the administration services for the ProjectUsers entities.
 * @author Sébastien Binda
 */
@Component
@Primary
public class ProjectUsersClientStub implements IProjectUsersClient {

    /**
     * Locale list of users
     */
    private static List<ProjectUser> users = new ArrayList<>();

    @Override
    public ResponseEntity<PagedModel<EntityModel<ProjectUser>>> retrieveProjectUserList(final int pPage,
            final int pSize) {
        final PageMetadata metadata = new PageMetadata(pSize, pPage, users.size());
        final PagedModel<EntityModel<ProjectUser>> resource = new PagedModel<>(HateoasUtils.wrapList(users), metadata,
                new ArrayList<>());
        return new ResponseEntity<>(resource, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<EntityModel<ProjectUser>> retrieveProjectUserByEmail(final String pUserEmail) {
        ProjectUser result = null;
        for (final ProjectUser user : users) {
            if (user.getEmail().equals(pUserEmail)) {
                result = user;
                break;
            }
        }
        if (result == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity<>(HateoasUtils.wrap(result), HttpStatus.OK);
    }

    @Override
    public ResponseEntity<EntityModel<ProjectUser>> updateProjectUser(final Long pUserId,
            final ProjectUser pUpdatedProjectUser) {
        return null;
    }

    @Override
    public ResponseEntity<Void> removeProjectUser(final Long pUserId) {
        return null;
    }

    @Override
    public ResponseEntity<PagedModel<EntityModel<ProjectUser>>> retrieveAccessRequestList(final int pPage,
            final int pSize) {
        return null;
    }

    @Override
    public ResponseEntity<PagedModel<EntityModel<ProjectUser>>> retrieveRoleProjectUserList(final Long pRoleId,
            final int pPage, final int pSize) {
        return null;
    }

    @Override
    public ResponseEntity<PagedModel<EntityModel<ProjectUser>>> retrieveRoleProjectUsersList(String pRole, int pPage,
            int pSize) {
        return null;
    }

    @Override
    public ResponseEntity<EntityModel<ProjectUser>> retrieveProjectUser(final Long pUserId) {
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see fr.cnes.regards.modules.accessrights.client.IProjectUsersClient#isAdmin(java.lang.String)
     */
    @Override
    public ResponseEntity<Boolean> isAdmin(final String pUserEmail) {
        return null;
    }

    @Override
    public ResponseEntity<EntityModel<ProjectUser>> createUser(final AccessRequestDto pDto) {
        return null;
    }

}
