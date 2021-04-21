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
package fr.cnes.regards.modules.accessrights.rest;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.web.bind.annotation.RequestMethod;

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.framework.test.integration.AbstractRegardsTransactionalIT;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import fr.cnes.regards.modules.accessrights.dao.projects.IProjectUserRepository;
import fr.cnes.regards.modules.accessrights.dao.projects.IResourcesAccessRepository;
import fr.cnes.regards.modules.accessrights.dao.projects.IRoleRepository;
import fr.cnes.regards.modules.accessrights.domain.UserStatus;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;
import fr.cnes.regards.modules.accessrights.domain.projects.ResourcesAccess;
import fr.cnes.regards.modules.accessrights.domain.projects.Role;
import fr.cnes.regards.modules.accessrights.service.role.RoleService;

/**
 * Integration tests for ProjectUsers REST Controller.
 * @author svissier
 * @author Sébastien Binda
 * @author Xavier-Alexandre Brochard
 */
@MultitenantTransactional
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=account" })
public class ProjectUsersControllerIT extends AbstractRegardsTransactionalIT {

    /**
     * An email
     */
    private static final String EMAIL = "email@test.com";

    private static final String ROLE_TEST = "TEST_ROLE";

    private static final String ERROR_MESSAGE = "Cannot reach model attributes";

    @Autowired
    private IProjectUserRepository projectUserRepository;

    @Autowired
    private IRoleRepository roleRepository;

    @Autowired
    private RoleService roleService;

    @Autowired
    private IResourcesAccessRepository resourcesAccessRepository;

    /**
     * A project user.<br>
     * We ensure before each test to have only this exactly project user in db for convenience.
     */
    private ProjectUser projectUser;

    private Role publicRole;

    private Role roleTest;

    @Before
    public void setUp() {
        publicRole = roleRepository.findOneByName(DefaultRole.PUBLIC.toString()).get();
        projectUser = projectUserRepository
                .save(new ProjectUser(EMAIL, publicRole, new ArrayList<>(), new ArrayList<>()));

        // Insert some authorizations
        setAuthorities(RegistrationController.REQUEST_MAPPING_ROOT + RegistrationController.ACCEPT_ACCESS_RELATIVE_PATH,
                       RequestMethod.PUT,
                       DEFAULT_ROLE);
        setAuthorities(RegistrationController.REQUEST_MAPPING_ROOT + RegistrationController.DENY_ACCESS_RELATIVE_PATH,
                       RequestMethod.PUT,
                       DEFAULT_ROLE);

        roleRepository.findOneByName(ROLE_TEST).ifPresent(role -> roleRepository.delete(role));
        Role aNewRole = roleRepository.save(new Role(ROLE_TEST, publicRole));

        Set<ResourcesAccess> resourcesAccess = new HashSet<>();
        ResourcesAccess aResourcesAccess = new ResourcesAccess("",
                                                               "aMicroservice",
                                                               "the resource",
                                                               "Controller",
                                                               RequestMethod.GET,
                                                               DefaultRole.ADMIN);
        aResourcesAccess = resourcesAccessRepository.save(aResourcesAccess);
        ResourcesAccess bResourcesAccess = new ResourcesAccess("",
                                                               "aMicroservice",
                                                               "the resource",
                                                               "Controller",
                                                               RequestMethod.DELETE,
                                                               DefaultRole.ADMIN);
        bResourcesAccess = resourcesAccessRepository.save(bResourcesAccess);

        resourcesAccess.add(aResourcesAccess);
        resourcesAccess.add(bResourcesAccess);
        aNewRole.setPermissions(resourcesAccess);
        roleTest = roleRepository.save(aNewRole);
    }

    @Override
    protected String getDefaultRole() {
        return DefaultRole.PROJECT_ADMIN.toString();
    }

    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_310")
    @Purpose("Check that the system allows to retrieve all user on a project.")
    public void getAllUsers() {
        performDefaultGet(ProjectUsersController.TYPE_MAPPING, customizer().expectStatusOk(), ERROR_MESSAGE);
    }

    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_310")
    @Purpose("Check that the system allows to retrieve a single user on a project.")
    public void getUser() {
        String apiUserEmail = ProjectUsersController.TYPE_MAPPING + "/email/{user_email}";

        performDefaultGet(apiUserEmail, customizer().expectStatusNotFound(), ERROR_MESSAGE, "user@invalid.fr");

        performDefaultGet(apiUserEmail, customizer().expectStatusOk(), ERROR_MESSAGE, EMAIL);
    }

    /**
     * Check that the system allows a user to connect using a hierarchically inferior role.
     * @throws EntityNotFoundException not found
     */
    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_270")
    @Purpose("Check that the system allows a user to connect using a hierarchically inferior role.")
    public void getUserPermissions_withBorrowedRoleInferior() throws EntityNotFoundException {

        String apiUserPermissionsBorrowedRole = UserResourceController.TYPE_MAPPING + "?borrowedRoleName=";

        // Prepare a project user with role admin
        Role roleAdmin = roleService.retrieveRole(DefaultRole.ADMIN.toString());
        projectUser.setRole(roleAdmin);
        projectUserRepository.save(projectUser);

        // Get the borrowed role
        String borrowedRoleName = DefaultRole.REGISTERED_USER.toString();
        Role borrowedRole = roleService.retrieveRole(borrowedRoleName);

        // Borrowing a hierarchically inferior role
        Assert.assertTrue(roleService.isHierarchicallyInferior(borrowedRole, roleAdmin));
        performDefaultGet(apiUserPermissionsBorrowedRole + borrowedRoleName,
                          customizer().expectStatusOk(),
                          ERROR_MESSAGE,
                          projectUser.getEmail());
    }

    /**
     * Check that the system prevents a user to connect using a hierarchically superior role.
     * @throws EntityNotFoundException not found
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
        performDefaultGet(apiUserPermissionsBorrowedRole + borrowedRoleName,
                          customizer().expect(MockMvcResultMatchers.status().isForbidden()),
                          ERROR_MESSAGE,
                          projectUser.getEmail());
    }

    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_230")
    @Purpose("Check that the system allows to delete a user's permissions.")
    public void deleteUserPermissions() {
        performDefaultDelete(UserResourceController.TYPE_MAPPING,
                             customizer().expectStatusOk(),
                             ERROR_MESSAGE,
                             projectUser.getEmail());
    }

    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_310")
    @Purpose("Check that the system allows to update a project user and handles fail cases.")
    public void updateUser() {
        String apiUserId = ProjectUsersController.TYPE_MAPPING + ProjectUsersController.USER_ID_RELATIVE_PATH;

        projectUser.setEmail("new@email.com");

        // Same id
        performDefaultPut(apiUserId, projectUser, customizer().expectStatusOk(), ERROR_MESSAGE, projectUser.getId());

        // Wrong id (99L)
        performDefaultPut(apiUserId, projectUser, customizer().expectStatusBadRequest(), ERROR_MESSAGE, 99L);
    }

    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_310")
    @Purpose("Check that the system allows to delete a project user.")
    public void deleteUser() {
        String apiUserId = ProjectUsersController.TYPE_MAPPING + "/{user_id}";

        performDefaultDelete(apiUserId, customizer().expectStatusOk(), ERROR_MESSAGE, projectUser.getId());
    }

    @Test
    @Purpose("Check we add 'accept' HATEOAS link")
    public void checkHateoasLinks_shouldAddAcceptLink() {
        // Set project user in WAITING_ACCESS
        projectUser.setStatus(UserStatus.WAITING_ACCESS);
        projectUserRepository.save(projectUser);

        String apiUserId = ProjectUsersController.TYPE_MAPPING + ProjectUsersController.USER_ID_RELATIVE_PATH;

        performDefaultGet(apiUserId,
                          customizer().expectStatusOk().expectValue("$.links.[4].rel", "accept"),
                          ERROR_MESSAGE,
                          projectUser.getId());
    }

    @Test
    @Purpose("Check we add 'deny' HATEOAS link")
    public void checkHateoasLinks_shouldAddDenyLink() {
        // Set project user in WAITING_ACCESS
        projectUser.setStatus(UserStatus.WAITING_ACCESS);
        projectUserRepository.save(projectUser);

        String apiUserId = ProjectUsersController.TYPE_MAPPING + ProjectUsersController.USER_ID_RELATIVE_PATH;

        performDefaultGet(apiUserId,
                          customizer().expectStatusOk().expectValue("$.links.[5].rel", "deny"),
                          ERROR_MESSAGE,
                          projectUser.getId());
    }

    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_210")
    @Purpose("Check that the system allows to retrieve all users of a role.")
    public void retrieveRoleProjectUserList() {
        performDefaultGet(ProjectUsersController.TYPE_MAPPING + ProjectUsersController.ROLES_ROLE_ID,
                          customizer().expectStatusOk(),
                          "TODO Error message",
                          roleTest.getId());
    }

    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_230")
    @Purpose("Check that the system allows to update a user's permissions.")
    public void updateUserPermissions() {
        final List<ResourcesAccess> newPermissionList = new ArrayList<>();
        newPermissionList.add(resourcesAccessRepository.save(new ResourcesAccess("desc0",
                                                                                 "ms0",
                                                                                 "res0",
                                                                                 "Controller",
                                                                                 RequestMethod.GET,
                                                                                 DefaultRole.ADMIN)));
        newPermissionList.add(resourcesAccessRepository.save(new ResourcesAccess("desc1",
                                                                                 "ms1",
                                                                                 "res1",
                                                                                 "Controller",
                                                                                 RequestMethod.DELETE,
                                                                                 DefaultRole.ADMIN)));

        performDefaultPut(UserResourceController.TYPE_MAPPING,
                          newPermissionList,
                          customizer().expectStatusOk(),
                          ERROR_MESSAGE,
                          EMAIL);
    }

    /**
     * Check that the system allows to retrieve all access requests for a project.
     */
    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_310")
    @Purpose("Check that the system allows to retrieve all access requests for a project.")
    public void getAllAccesses() {
        performDefaultGet(ProjectUsersController.TYPE_MAPPING + ProjectUsersController.PENDINGACCESSES,
                          customizer().expectStatusOk(),
                          ERROR_MESSAGE);
    }

}
