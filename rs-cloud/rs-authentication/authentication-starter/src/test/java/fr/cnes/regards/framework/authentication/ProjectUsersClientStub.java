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
package fr.cnes.regards.framework.authentication;

import fr.cnes.regards.framework.hateoas.HateoasUtils;
import fr.cnes.regards.modules.accessrights.client.IProjectUsersClient;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUserSearchParameters;
import fr.cnes.regards.modules.accessrights.domain.registration.AccessRequestDto;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.hateoas.PagedModel.PageMetadata;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Class ProjectUsersClientStub
 * <p>
 * Stub to simulate the responses from the administration services for the ProjectUsers entities.
 *
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
    public ResponseEntity<PagedModel<EntityModel<ProjectUser>>> retrieveProjectUserList(ProjectUserSearchParameters parameters,
                                                                                        Pageable pageable) {
        final PageMetadata metadata = new PageMetadata(pageable.getPageSize(), pageable.getPageNumber(), users.size());
        final PagedModel<EntityModel<ProjectUser>> resource = PagedModel.of(HateoasUtils.wrapList(users),
                                                                            metadata,
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
    public ResponseEntity<PagedModel<EntityModel<ProjectUser>>> retrieveAccessRequestList(Pageable pageable) {
        return null;
    }

    @Override
    public ResponseEntity<PagedModel<EntityModel<ProjectUser>>> retrieveRoleProjectUserList(final Long pRoleId,
                                                                                            Pageable pageable) {
        return null;
    }

    @Override
    public ResponseEntity<PagedModel<EntityModel<ProjectUser>>> retrieveRoleProjectUsersList(String pRole,
                                                                                             Pageable page) {
        return null;
    }

    @Override
    public ResponseEntity<Void> linkAccessGroups(String email, List<String> groups) {
        return null;
    }

    @Override
    public ResponseEntity<Void> updateOrigin(String email, String origin) {
        return null;
    }

    @Override
    public ResponseEntity<Void> sendVerificationEmail(String email) {
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

    @Override
    public ResponseEntity<EntityModel<ProjectUser>> retrieveCurrentProjectUser() {
        return null;
    }

    @Override
    public ResponseEntity<EntityModel<ProjectUser>> updateCurrentProjectUser(ProjectUser updatedProjectUser) {
        return null;
    }

}
