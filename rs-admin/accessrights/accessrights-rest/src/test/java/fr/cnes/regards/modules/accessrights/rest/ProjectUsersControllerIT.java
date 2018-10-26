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
package fr.cnes.regards.modules.accessrights.rest;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.web.bind.annotation.RequestMethod;

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.framework.test.integration.AbstractRegardsTransactionalIT;
import fr.cnes.regards.framework.test.integration.RequestBuilderCustomizer;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import fr.cnes.regards.modules.accessrights.dao.projects.IMetaDataRepository;
import fr.cnes.regards.modules.accessrights.dao.projects.IProjectUserRepository;
import fr.cnes.regards.modules.accessrights.dao.projects.IResourcesAccessRepository;
import fr.cnes.regards.modules.accessrights.dao.projects.IRoleRepository;
import fr.cnes.regards.modules.accessrights.domain.UserStatus;
import fr.cnes.regards.modules.accessrights.domain.projects.MetaData;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;
import fr.cnes.regards.modules.accessrights.domain.projects.ResourcesAccess;
import fr.cnes.regards.modules.accessrights.domain.projects.Role;
import fr.cnes.regards.modules.accessrights.service.role.RoleService;

/**
 * Integration tests for ProjectUsers REST Controller.
 *
 * @author svissier
 * @author Sébastien Binda
 * @author Xavier-Alexandre Brochard
 * @since 1.0-SNAPSHOT
 */
@MultitenantTransactional
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=account" })
public class ProjectUsersControllerIT extends AbstractRegardsTransactionalIT {

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(ProjectUsersControllerIT.class);

    /**
     * An email
     */
    private static final String EMAIL = "email@test.com";

    private String errorMessage;

    @Autowired
    private IProjectUserRepository projectUserRepository;

    @Autowired
    private IRoleRepository roleRepository;

    @Autowired
    private RoleService roleService;

    @Autowired
    private IResourcesAccessRepository resourcesAccessRepository;

    @Autowired
    private IMetaDataRepository metaDataRepository;

    /**
     * A project user.<br>
     * We ensure before each test to have only this exactly project user in db for convenience.
     */
    private ProjectUser projectUser;

    private Role publicRole;

    @Before
    public void setUp() {
        errorMessage = "Cannot reach model attributes";
        publicRole = roleRepository.findOneByName(DefaultRole.PUBLIC.toString()).get();
        projectUser = projectUserRepository
                .save(new ProjectUser(EMAIL, publicRole, new ArrayList<>(), new ArrayList<>()));

        // Insert some authorizations
        setAuthorities(RegistrationController.REQUEST_MAPPING_ROOT + RegistrationController.ACCEPT_ACCESS_RELATIVE_PATH,
                       RequestMethod.PUT, DEFAULT_ROLE);
        setAuthorities(RegistrationController.REQUEST_MAPPING_ROOT + RegistrationController.DENY_ACCESS_RELATIVE_PATH,
                       RequestMethod.PUT, DEFAULT_ROLE);
    }

    @Override
    protected String getDefaultRole() {
        return DefaultRole.PROJECT_ADMIN.toString();
    }

    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_310")
    @Purpose("Check that the system allows to retrieve all user on a project.")
    public void getAllUsers() {
        RequestBuilderCustomizer requestBuilder = getNewRequestBuilderCustomizer();
        requestBuilder.addExpectation(MockMvcResultMatchers.status().isOk());
        performDefaultGet(ProjectUsersController.TYPE_MAPPING, requestBuilder, errorMessage);
    }

    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_310")
    @Purpose("Check that the system allows to retrieve a single user on a project.")
    public void getUser() throws UnsupportedEncodingException {
        String apiUserEmail = ProjectUsersController.TYPE_MAPPING + "/email/{user_email}";

        RequestBuilderCustomizer requestBuilder = getNewRequestBuilderCustomizer();
        requestBuilder.addExpectation(MockMvcResultMatchers.status().isOk());
        performDefaultGet(apiUserEmail, requestBuilder, errorMessage, EMAIL);

        requestBuilder = getNewRequestBuilderCustomizer();
        requestBuilder.addExpectation(MockMvcResultMatchers.status().isNotFound());
        performDefaultGet(apiUserEmail, requestBuilder, errorMessage, "user@invalid.fr");
    }

    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_330")
    @Purpose("Check that the system allows to retrieve a user's metadata.")
    public void getUserMetaData() {
        RequestBuilderCustomizer requestBuilder = getNewRequestBuilderCustomizer();
        requestBuilder.addExpectation(MockMvcResultMatchers.status().isOk());
        performDefaultGet(ProjectUserMetadataController.REQUEST_MAPPING_ROOT, requestBuilder, errorMessage,
                          projectUser.getId());
    }

    /**
     * Check that the system allows a user to connect using a hierarchically inferior role.
     *
     * @throws EntityNotFoundException
     *             not found
     */
    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_270")
    @Purpose("Check that the system allows a user to connect using a hierarchically inferior role.")
    public void getUserPermissions_withBorrowedRoleInferior() throws EntityNotFoundException {

        String apiUserPermissionsBorrowedRole = UserResourceController.TYPE_MAPPING + "?borrowedRoleName=";

        // Prepare a project user with role admin
        final Role roleAdmin = roleService.retrieveRole(DefaultRole.ADMIN.toString());
        projectUser.setRole(roleAdmin);
        projectUserRepository.save(projectUser);

        // Get the borrowed role
        final String borrowedRoleName = DefaultRole.REGISTERED_USER.toString();
        final Role borrowedRole = roleService.retrieveRole(borrowedRoleName);

        // Borrowing a hierarchically inferior role
        Assert.assertTrue(roleService.isHierarchicallyInferior(borrowedRole, roleAdmin));
        RequestBuilderCustomizer requestBuilder = getNewRequestBuilderCustomizer();
        requestBuilder.addExpectation(MockMvcResultMatchers.status().isOk());
        performDefaultGet(apiUserPermissionsBorrowedRole + borrowedRoleName, requestBuilder, errorMessage,
                          projectUser.getEmail());
    }

    /**
     * Check that the system prevents a user to connect using a hierarchically superior role.
     *
     * @throws EntityNotFoundException
     *             not found
     */
    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_270")
    @Purpose("Check that the system prevents a user to connect using a hierarchically superior role.")
    public void getUserPermissions_withBorrowedRoleSuperior() throws EntityNotFoundException {

        String apiUserPermissionsBorrowedRole = UserResourceController.TYPE_MAPPING + "?borrowedRoleName=";

        // Prepare a project user with role admin
        final Role roleAdmin = roleService.retrieveRole(DefaultRole.ADMIN.toString());
        projectUser.setRole(roleAdmin);
        projectUserRepository.save(projectUser);

        // Get the borrowed role
        final String borrowedRoleName = DefaultRole.INSTANCE_ADMIN.toString();
        final Role borrowedRole = roleService.retrieveRole(borrowedRoleName);

        // Borrowing a hierarchically superior role
        Assert.assertTrue(!roleService.isHierarchicallyInferior(borrowedRole, roleAdmin));
        RequestBuilderCustomizer requestBuilder = getNewRequestBuilderCustomizer();
        requestBuilder.addExpectation(MockMvcResultMatchers.status().isForbidden());
        performDefaultGet(apiUserPermissionsBorrowedRole + borrowedRoleName, requestBuilder, errorMessage,
                          projectUser.getEmail());
    }

    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_330")
    @Purpose("Check that the system allows to update a user's metadata.")
    public void updateUserMetaData() {
        final List<MetaData> newPermissionList = new ArrayList<>();
        newPermissionList.add(metaDataRepository.save(new MetaData()));
        newPermissionList.add(metaDataRepository.save(new MetaData()));

        RequestBuilderCustomizer requestBuilder = getNewRequestBuilderCustomizer();
        requestBuilder.addExpectation(MockMvcResultMatchers.status().isOk());
        performDefaultPut(ProjectUserMetadataController.REQUEST_MAPPING_ROOT, newPermissionList, requestBuilder,
                          errorMessage, projectUser.getId());
    }

    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_230")
    @Purpose("Check that the system allows to update a user's permissions.")
    public void updateUserPermissions() {
        final List<ResourcesAccess> newPermissionList = new ArrayList<>();
        newPermissionList.add(resourcesAccessRepository
                .save(new ResourcesAccess("desc0", "ms0", "res0", "Controller", RequestMethod.GET, DefaultRole.ADMIN)));
        newPermissionList.add(resourcesAccessRepository.save(new ResourcesAccess("desc1", "ms1", "res1", "Controller",
                RequestMethod.DELETE, DefaultRole.ADMIN)));

        RequestBuilderCustomizer requestBuilder = getNewRequestBuilderCustomizer();
        requestBuilder.addExpectation(MockMvcResultMatchers.status().isOk());
        performDefaultPut(UserResourceController.TYPE_MAPPING, newPermissionList, requestBuilder, errorMessage, EMAIL);
    }

    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_330")
    @Purpose("Check that the system allows to delete a user's metadata.")
    public void deleteUserMetaData() {
        RequestBuilderCustomizer requestBuilder = getNewRequestBuilderCustomizer();
        requestBuilder.addExpectation(MockMvcResultMatchers.status().isOk());
        performDefaultDelete(ProjectUserMetadataController.REQUEST_MAPPING_ROOT, requestBuilder, errorMessage,
                             projectUser.getId());
    }

    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_230")
    @Purpose("Check that the system allows to delete a user's permissions.")
    public void deleteUserPermissions() {
        RequestBuilderCustomizer requestBuilder = getNewRequestBuilderCustomizer();
        requestBuilder.addExpectation(MockMvcResultMatchers.status().isOk());
        performDefaultDelete(UserResourceController.TYPE_MAPPING, requestBuilder, errorMessage, projectUser.getEmail());
    }

    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_310")
    @Purpose("Check that the system allows to update a project user and handles fail cases.")
    public void updateUser() {
        String apiUserId = ProjectUsersController.TYPE_MAPPING + ProjectUsersController.USER_ID_RELATIVE_PATH;

        projectUser.setEmail("new@email.com");

        // Same id
        RequestBuilderCustomizer requestBuilder = getNewRequestBuilderCustomizer();
        requestBuilder.addExpectation(MockMvcResultMatchers.status().isOk());
        performDefaultPut(apiUserId, projectUser, requestBuilder, errorMessage, projectUser.getId());

        // Wrong id (99L)
        requestBuilder = getNewRequestBuilderCustomizer();
        requestBuilder.addExpectation(MockMvcResultMatchers.status().isBadRequest());
        performDefaultPut(apiUserId, projectUser, requestBuilder, errorMessage, 99L);
    }

    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_310")
    @Purpose("Check that the system allows to delete a project user.")
    public void deleteUser() {
        String apiUserId = ProjectUsersController.TYPE_MAPPING + "/{user_id}";

        RequestBuilderCustomizer requestBuilder = getNewRequestBuilderCustomizer();
        requestBuilder.addExpectation(MockMvcResultMatchers.status().isOk());
        performDefaultDelete(apiUserId, requestBuilder, errorMessage, projectUser.getId());
    }

    @Test
    @Purpose("Check we add 'accept' HATEOAS link")
    public void checkHateoasLinks_shouldAddAcceptLink() {
        // Set project user in WAITING_ACCESS
        projectUser.setStatus(UserStatus.WAITING_ACCESS);
        projectUserRepository.save(projectUser);

        String apiUserId = ProjectUsersController.TYPE_MAPPING + ProjectUsersController.USER_ID_RELATIVE_PATH;

        RequestBuilderCustomizer requestBuilder = getNewRequestBuilderCustomizer();
        requestBuilder.addExpectation(MockMvcResultMatchers.status().isOk());
        requestBuilder.addExpectation(MockMvcResultMatchers.jsonPath("$.links.[4].rel", Matchers.is("accept")));
        performDefaultGet(apiUserId, requestBuilder, errorMessage, projectUser.getId());
    }

    @Test
    @Purpose("Check we add 'deny' HATEOAS link")
    public void checkHateoasLinks_shouldAddDenyLink() {
        // Set project user in WAITING_ACCESS
        projectUser.setStatus(UserStatus.WAITING_ACCESS);
        projectUserRepository.save(projectUser);

        String apiUserId = ProjectUsersController.TYPE_MAPPING + ProjectUsersController.USER_ID_RELATIVE_PATH;

        RequestBuilderCustomizer requestBuilder = getNewRequestBuilderCustomizer();
        requestBuilder.addExpectation(MockMvcResultMatchers.status().isOk());
        requestBuilder.addExpectation(MockMvcResultMatchers.jsonPath("$.links.[5].rel", Matchers.is("deny")));
        performDefaultGet(apiUserId, requestBuilder, errorMessage, projectUser.getId());
    }

    @Override
    protected Logger getLogger() {
        return LOG;
    }
}
