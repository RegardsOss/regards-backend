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
package fr.cnes.regards.modules.accessrights.rest;

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.module.rest.exception.EntityException;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.framework.security.utils.jwt.exception.JwtException;
import fr.cnes.regards.framework.test.integration.AbstractRegardsTransactionalIT;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import fr.cnes.regards.modules.accessrights.dao.projects.IResourcesAccessRepository;
import fr.cnes.regards.modules.accessrights.dao.projects.IRoleRepository;
import fr.cnes.regards.modules.accessrights.domain.projects.ResourcesAccess;
import fr.cnes.regards.modules.accessrights.domain.projects.Role;
import fr.cnes.regards.modules.accessrights.service.projectuser.QuotaHelperService;
import fr.cnes.regards.modules.accessrights.service.role.RoleService;
import org.junit.*;
import org.junit.rules.ExpectedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.HashSet;
import java.util.Set;

/**
 * Integration tests for Roles REST Controller.
 *
 * @author Sébastien Binda
 * @author Xavier-Alexandre Brochard
 * @author Christophe Mertz
 */
@MultitenantTransactional
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=account" })
public class RolesControllerIT extends AbstractRegardsTransactionalIT {

    @Autowired
    private RoleService roleService;

    @Autowired
    private IResourcesAccessRepository resourcesAccessRepository;

    /**
     * Role repository
     */
    @Autowired
    private IRoleRepository roleRepository;

    @MockBean
    private QuotaHelperService quotaHelperService;

    @Rule
    public ExpectedException thrown_ = ExpectedException.none();

    @Value("${root.admin.login:admin}")
    private String rootAdminLogin;

    @Value("${root.admin.password:admin}")
    private String rootAdminPassword;

    private static final String ROLE_TEST = "TEST_ROLE";

    private Role roleTest;

    private Role publicRole;

    private ResourcesAccess resourceAccessPublic;

    @Before
    public void init() {

        // Init roles
        publicRole = roleRepository.findOneByName(DefaultRole.PUBLIC.toString()).get();
        Set<ResourcesAccess> resourcesAccessPublic = new HashSet<>();
        resourceAccessPublic = new ResourcesAccess("",
                                                   "aMicroservice",
                                                   "the public resource",
                                                   "Controller",
                                                   RequestMethod.GET,
                                                   DefaultRole.ADMIN);
        resourceAccessPublic = resourcesAccessRepository.save(resourceAccessPublic);
        resourcesAccessPublic.add(resourceAccessPublic);
        publicRole.setPermissions(resourcesAccessPublic);
        roleRepository.save(publicRole);

        // Create a new Role
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

    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_210")
    @Purpose("Check that the system allows to create a role and handle fail cases.")
    public void createRole() throws EntityException {
        String newRoleName = "NEW_ROLE";
        if (roleService.existByName(newRoleName)) {
            Role toDelete = roleService.retrieveRole(newRoleName);
            roleService.removeRole(toDelete.getId());
        }
        Role newRole = new Role(newRoleName, publicRole);

        performDefaultPost(RoleController.TYPE_MAPPING,
                           newRole,
                           customizer().expectStatusCreated(),
                           "TODO Error message");

        performDefaultPost(RoleController.TYPE_MAPPING,
                           newRole,
                           customizer().expectStatusConflict(),
                           "TODO Error message");
    }

    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_210")
    @Purpose("Check that the allows to retrieve a single role and handle fail cases.")
    public void retrieveRole() {
        performDefaultGet(RoleController.TYPE_MAPPING + RoleController.ROLE_MAPPING,
                          customizer().expectStatusOk(),
                          "TODO Error message",
                          DefaultRole.REGISTERED_USER);

        String wrongRoleName = "WRONG_ROLE";
        performDefaultGet(RoleController.TYPE_MAPPING + RoleController.ROLE_MAPPING,
                          customizer().expectStatusNotFound(),
                          "TODO Error message",
                          wrongRoleName);
    }

    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_210")
    @Purpose("Check that the system allows to update a role and handle fail cases.")
    public void updateRole() {
        // Grab a role and change something
        roleTest.setName("newValue");

        // Regular case
        performDefaultPut(RoleController.TYPE_MAPPING + RoleController.ROLE_MAPPING,
                          roleTest,
                          customizer().expectStatusOk(),
                          "TODO Error message",
                          roleTest.getName());

        // Fail case: ids differ
        performDefaultPut(RoleController.TYPE_MAPPING + RoleController.ROLE_MAPPING,
                          roleTest,
                          customizer().expectStatusBadRequest(),
                          "TODO Error message",
                          99L);
    }

    /**
     * Check that the system prevents from deleting a native role.
     */
    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_210")
    @Purpose("Check that the system prevents from deleting a native role.")
    public void removeRoleNative() throws JwtException {
        // Role public is native, we use this one
        long nRole = roleRepository.count();
        performDefaultDelete(RoleController.TYPE_MAPPING + RoleController.ROLE_MAPPING,
                             customizer().expectStatusForbidden(),
                             "TODO Error message",
                             publicRole.getName());

        jwtService.injectToken(getDefaultTenant(), DefaultRole.PROJECT_ADMIN.toString(), "", "");
        Assert.assertEquals(nRole, roleRepository.count());
    }

    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_210")
    @Purpose("Check that the allows to retrieve roles.")
    public void retrieveRoleList() {
        Assert.assertEquals(7, roleRepository.count());

        // Use PROJECT ADMIN
        String projectAdminJwt = manageSecurity(getDefaultTenant(),
                                                RoleResourceController.TYPE_MAPPING,
                                                RequestMethod.GET,
                                                getDefaultUserEmail(),
                                                DefaultRole.PROJECT_ADMIN.name());
        performGet(RoleController.TYPE_MAPPING,
                   projectAdminJwt,
                   customizer().expectStatusOk().expectToHaveSize("$.*.content.id", 6)
                               // 6 = 5 roles and the added role TEST_ROLE has two permissions
                               // Updated : Permissions are ignore in roles results requests to avoid lazy load.
                               // expectations.add(MockMvcResultMatchers.jsonPath("$.*.content.permissions", hasSize(6)));
                               // 3 = 3 roles has a parent (public, project_admin, instance_admin has no parent)
                               .expectToHaveSize("$.*.content.parentRole", 4),
                   "TODO Error message");
    }

    @Test
    @Purpose("Check that the microservice allow to retrieve all roles associated to a given resource.")
    public void retrieveRoleAsswoatedToAResourceAccess() {
        performDefaultGet(RoleController.TYPE_MAPPING + RoleController.ROLE_WITH_RESOURCE_MAPPING,
                          customizer().expectStatusOk().expectToHaveSize("$.*.content.id", 1),
                          "TODO Error message",
                          resourceAccessPublic.getId());
    }

    /**
     * Check that the system allows to delete a role.
     */
    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_210")
    @Purpose("Check that the system allows to delete a role.")
    public void removeRole() throws JwtException {
        long nRole = roleRepository.count();
        // Create a non-native role
        performDefaultDelete(RoleController.TYPE_MAPPING + RoleController.ROLE_MAPPING,
                             customizer().expectStatusOk(),
                             "TODO Error message",
                             roleTest.getName());

        jwtService.injectToken(getDefaultTenant(), DefaultRole.PROJECT_ADMIN.toString(), "", "");
        Assert.assertEquals(nRole - 1, roleRepository.count());
    }

    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_210")
    @Purpose("Check that the system allows to retrieve all resources accesses of a role.")
    public void retrieveRoleResourcesAccessList() {
        performDefaultGet(RoleResourceController.TYPE_MAPPING,
                          customizer().expectStatusOk(),
                          "TODO Error message",
                          roleTest.getName());
    }

    /**
     * Check hierarchy of roles
     */
    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_210")
    @Purpose("Check hierachy of roles")
    public void retrieveInheritedRoles() {
        Set<Role> roles = roleService.retrieveInheritedRoles(publicRole);
        Assert.assertEquals(roles.size(), DefaultRole.values().length - 3 + 1);
        Assert.assertTrue(roles.stream().anyMatch(r -> r.getName().equals(DefaultRole.ADMIN.toString())));
        Assert.assertTrue(roles.stream().anyMatch(r -> r.getName().equals(DefaultRole.REGISTERED_USER.toString())));
        Assert.assertTrue(roles.stream().anyMatch(r -> r.getName().equals(ROLE_TEST)));
    }

    @Ignore("This test does not work on CI platform but works well everywhere else")
    @Test
    public void testShouldAccessToResourceRequiring() {
        // Send the request with an user having role REGISTERED_USER
        String userJwt = manageSecurity(getDefaultTenant(),
                                        RoleController.SHOULD_ACCESS_TO_RESOURCE,
                                        RequestMethod.GET,
                                        getDefaultUserEmail(),
                                        DefaultRole.REGISTERED_USER.name());

        performGet(RoleController.TYPE_MAPPING + RoleController.SHOULD_ACCESS_TO_RESOURCE,
                   userJwt,
                   customizer().expectStatusOk().expectToHaveToString("$", "true"),
                   "Failed to validate role hierarchie",
                   DefaultRole.PUBLIC.toString());

        performGet(RoleController.TYPE_MAPPING + RoleController.SHOULD_ACCESS_TO_RESOURCE,
                   userJwt,
                   customizer().expectStatusOk().expectToHaveToString("$", "false"),
                   "users is not above instance admin",
                   DefaultRole.ADMIN.toString());
    }

}
