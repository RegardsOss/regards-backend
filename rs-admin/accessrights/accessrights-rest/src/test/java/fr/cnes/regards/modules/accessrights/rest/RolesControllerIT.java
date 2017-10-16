/*
 * Copyright 2017 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.web.bind.annotation.RequestMethod;

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
import fr.cnes.regards.modules.accessrights.service.role.RoleService;

/**
 * Integration tests for Roles REST Controller.
 *
 * @author Sébastien Binda
 * @author Xavier-Alexandre Brochard
 * @author Christophe Mertz
 * @since 1.0-SNAPSHOT
 */
@MultitenantTransactional
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=account" })
public class RolesControllerIT extends AbstractRegardsTransactionalIT {

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(RolesControllerIT.class);

    private String apiRolesUsers;

    @Autowired
    private RoleService roleService;

    @Autowired
    private IResourcesAccessRepository resourcesAccessRepository;

    /**
     * Role repository
     */
    @Autowired
    private IRoleRepository roleRepository;

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

        apiRolesUsers = ProjectUsersController.TYPE_MAPPING + "/roles/{role_id}";

        // Init roles
        publicRole = roleRepository.findOneByName(DefaultRole.PUBLIC.toString()).get();
        final Set<ResourcesAccess> resourcesAccessPublic = new HashSet<>();
        resourceAccessPublic = new ResourcesAccess("", "aMicroservice", "the public resource", "Controller",
                RequestMethod.GET, DefaultRole.ADMIN);
        resourceAccessPublic = resourcesAccessRepository.save(resourceAccessPublic);
        resourcesAccessPublic.add(resourceAccessPublic);
        publicRole.setPermissions(resourcesAccessPublic);
        roleRepository.save(publicRole);

        // Create a new Role
        roleRepository.findOneByName(ROLE_TEST).ifPresent(role -> roleRepository.delete(role));
        final Role aNewRole = roleRepository.save(new Role(ROLE_TEST, publicRole));

        final Set<ResourcesAccess> resourcesAccess = new HashSet<>();
        ResourcesAccess aResourcesAccess = new ResourcesAccess("", "aMicroservice", "the resource", "Controller",
                RequestMethod.GET, DefaultRole.ADMIN);
        aResourcesAccess = resourcesAccessRepository.save(aResourcesAccess);
        ResourcesAccess bResourcesAccess = new ResourcesAccess("", "aMicroservice", "the resource", "Controller",
                RequestMethod.DELETE, DefaultRole.ADMIN);
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
        final String newRoleName = "NEW_ROLE";
        if (roleService.existByName(newRoleName)) {
            final Role toDelete = roleService.retrieveRole(newRoleName);
            roleService.removeRole(toDelete.getId());
        }
        final Role newRole = new Role(newRoleName, publicRole);

        List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(status().isCreated());
        performDefaultPost(RoleController.TYPE_MAPPING, newRole, expectations, "TODO Error message");

        expectations = new ArrayList<>(1);
        expectations.add(status().isConflict());
        performDefaultPost(RoleController.TYPE_MAPPING, newRole, expectations, "TODO Error message");
    }

    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_210")
    @Purpose("Check that the allows to retrieve a single role and handle fail cases.")
    public void retrieveRole() {
        List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(status().isOk());
        performDefaultGet(RoleController.TYPE_MAPPING + RoleController.ROLE_MAPPING, expectations, "TODO Error message",
                          DefaultRole.REGISTERED_USER);

        final String wrongRoleName = "WRONG_ROLE";
        expectations = new ArrayList<>(1);
        expectations.add(status().isNotFound());
        performDefaultGet(RoleController.TYPE_MAPPING + RoleController.ROLE_MAPPING, expectations, "TODO Error message",
                          wrongRoleName);
    }

    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_210")
    @Purpose("Check that the system allows to update a role and handle fail cases.")
    public void updateRole() {
        // Grab a role and change something
        roleTest.setName("newValue");

        // Regular case
        List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(status().isOk());
        performDefaultPut(RoleController.TYPE_MAPPING + RoleController.ROLE_MAPPING, roleTest, expectations,
                          "TODO Error message", roleTest.getName());

        // Fail case: ids differ
        expectations = new ArrayList<>(1);
        expectations.add(MockMvcResultMatchers.status().isBadRequest());
        performDefaultPut(RoleController.TYPE_MAPPING + RoleController.ROLE_MAPPING, roleTest, expectations,
                          "TODO Error message", 99L);
    }

    /**
     * Check that the system prevents from deleting a native role.
     *
     * @throws JwtException
     */
    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_210")
    @Purpose("Check that the system prevents from deleting a native role.")
    public void removeRoleNative() throws JwtException {
        // Role public is native, we use this one
        final long nRole = roleRepository.count();
        final List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(status().isForbidden());
        performDefaultDelete(RoleController.TYPE_MAPPING + RoleController.ROLE_MAPPING, expectations,
                             "TODO Error message", publicRole.getName());

        jwtService.injectToken(DEFAULT_TENANT, DefaultRole.PROJECT_ADMIN.toString(), "");
        Assert.assertEquals(nRole, roleRepository.count());
    }

    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_210")
    @Purpose("Check that the allows to retrieve roles.")
    public void retrieveRoleList() throws JwtException {
        Assert.assertEquals(roleRepository.count(), 6);
        final List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(status().isOk());
        expectations.add(MockMvcResultMatchers.jsonPath("$.*.content.id", hasSize(5)));
        // 6 = 5 roles and the added role TEST_ROLE has two permissions
        // Updated : Permissions are ignore in roles results requests to avoid lazy load.
        // expectations.add(MockMvcResultMatchers.jsonPath("$.*.content.permissions", hasSize(6)));
        // 3 = 3 roles has a parent (public, project_admin, instance_admin has no parent)
        expectations.add(MockMvcResultMatchers.jsonPath("$.*.content.parentRole", hasSize(3)));

        // Use PROJECT ADMIN
        String projectAdminJwt = manageSecurity(RoleController.TYPE_MAPPING, RequestMethod.GET, DEFAULT_USER_EMAIL,
                                                DefaultRole.PROJECT_ADMIN.name());
        performGet(RoleController.TYPE_MAPPING, projectAdminJwt, expectations, "TODO Error message");
    }

    @Test
    @Purpose("Check that the microservice allow to retrieve all roles associated to a given resource.")
    public void retrieveRoleAsswoatedToAResourceAccess() throws JwtException {
        final List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(status().isOk());
        expectations.add(MockMvcResultMatchers.jsonPath("$.*.content.id", hasSize(1)));
        performDefaultGet(RoleController.TYPE_MAPPING + RoleController.ROLE_WITH_RESOURCE_MAPPING, expectations,
                          "TODO Error message", resourceAccessPublic.getId());
    }

    /**
     * Check that the system allows to delete a role.
     *
     * @throws JwtException
     */
    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_210")
    @Purpose("Check that the system allows to delete a role.")
    public void removeRole() throws JwtException {
        final long nRole = roleRepository.count();
        // Create a non-native role
        final List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(status().isOk());
        performDefaultDelete(RoleController.TYPE_MAPPING + RoleController.ROLE_MAPPING, expectations,
                             "TODO Error message", roleTest.getName());

        jwtService.injectToken(DEFAULT_TENANT, DefaultRole.PROJECT_ADMIN.toString(), "");
        Assert.assertEquals(nRole - 1, roleRepository.count());
    }

    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_210")
    @Purpose("Check that the system allows to retrieve all resources accesses of a role.")
    public void retrieveRoleResourcesAccessList() {
        final List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(status().isOk());
        performDefaultGet(RoleResourceController.TYPE_MAPPING, expectations, "TODO Error message", roleTest.getName());
    }

    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_210")
    @Purpose("Check that the system allows to retrieve all users of a role.")
    public void retrieveRoleProjectUserList() {
        final List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(status().isOk());
        performDefaultGet(apiRolesUsers, expectations, "TODO Error message", roleTest.getId());
    }

    /**
     * Check hierarchy of roles
     *
     * @since 1.0-SNAPSHOT
     */
    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_210")
    @Purpose("Check hierachy of roles")
    public void retrieveInheritedRoles() {
        final Set<Role> roles = roleService.retrieveInheritedRoles(publicRole);
        Assert.assertTrue(roles.size() == ((DefaultRole.values().length - 3) + 1));
        Assert.assertTrue(roles.stream().anyMatch(r -> r.getName().equals(DefaultRole.ADMIN.toString())));
        Assert.assertTrue(roles.stream().anyMatch(r -> r.getName().equals(DefaultRole.REGISTERED_USER.toString())));
        Assert.assertTrue(roles.stream().anyMatch(r -> r.getName().equals(ROLE_TEST.toString())));
    }

    @Override
    protected Logger getLogger() {
        return LOG;
    }

}
