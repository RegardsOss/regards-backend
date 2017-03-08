/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.rest;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
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
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.module.rest.exception.EntityException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.framework.security.utils.jwt.exception.JwtException;
import fr.cnes.regards.framework.test.integration.AbstractRegardsTransactionalIT;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import fr.cnes.regards.modules.accessrights.dao.projects.IResourcesAccessRepository;
import fr.cnes.regards.modules.accessrights.dao.projects.IRoleRepository;
import fr.cnes.regards.modules.accessrights.domain.HttpVerb;
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
public class RolesControllerIT extends AbstractRegardsTransactionalIT {

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(RolesControllerIT.class);

    private String apiRoles;

    private String apiRolesId;

    private String apiRolesName;

    private String apiRolesPermissions;

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

    @Before
    public void init() {
        apiRoles = RolesController.REQUEST_MAPPING_ROOT;
        apiRolesId = apiRoles + "/{role_id}";
        apiRolesName = apiRoles + "/{role_name}";

        apiRolesPermissions = ResourcesController.REQUEST_MAPPING_ROOT + "/roles/{role_id}";

        apiRolesUsers = ProjectUsersController.REQUEST_MAPPING_ROOT + "/roles/{role_id}";

        // Init roles
        publicRole = roleRepository.findOneByName(DefaultRole.PUBLIC.toString()).get();
        final Set<ResourcesAccess> resourcesAccessPublic = new HashSet<>();
        final ResourcesAccess aResourcesAccessPublic = new ResourcesAccess("", "aMicroservice", "the public resource",
                "Controller", HttpVerb.GET);
        resourcesAccessPublic.add(aResourcesAccessPublic);
        publicRole.setPermissions(resourcesAccessPublic);
        roleRepository.save(publicRole);

        // Create a new Role
        // roleRepository.findOneByName(ROLE_TEST).ifPresent(role -> roleRepository.delete(role));
        final Role aNewRole = roleRepository.save(new Role(ROLE_TEST, publicRole));

        final Set<ResourcesAccess> resourcesAccess = new HashSet<>();
        final ResourcesAccess aResourcesAccess = new ResourcesAccess("", "aMicroservice", "the resource", "Controller",
                HttpVerb.GET);
        final ResourcesAccess bResourcesAccess = new ResourcesAccess("", "aMicroservice", "the resource", "Controller",
                HttpVerb.DELETE);

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
        performDefaultPost(apiRoles, newRole, expectations, "TODO Error message");

        expectations = new ArrayList<>(1);
        expectations.add(status().isConflict());
        performDefaultPost(apiRoles, newRole, expectations, "TODO Error message");
    }

    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_210")
    @Purpose("Check that the allows to retrieve a single role and handle fail cases.")
    public void retrieveRole() {
        List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(status().isOk());
        performDefaultGet(apiRolesName, expectations, "TODO Error message", DefaultRole.REGISTERED_USER);

        final String wrongRoleName = "WRONG_ROLE";
        expectations = new ArrayList<>(1);
        expectations.add(status().isNotFound());
        performDefaultGet(apiRolesName, expectations, "TODO Error message", wrongRoleName);
    }

    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_210")
    @Purpose("Check that the system allows to update a role and handle fail cases.")
    public void updateRole() {
        // Grab a role and change something
        roleTest.setCorsRequestsAuthorizationEndDate(LocalDateTime.now().plusDays(2));

        // Regular case
        List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(status().isOk());
        performDefaultPut(apiRolesId, roleTest, expectations, "TODO Error message", roleTest.getId());

        // Fail case: ids differ
        expectations = new ArrayList<>(1);
        expectations.add(MockMvcResultMatchers.status().isBadRequest());
        performDefaultPut(apiRolesId, roleTest, expectations, "TODO Error message", 99L);
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
        performDefaultDelete(apiRolesId, expectations, "TODO Error message", publicRole.getId());

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
        expectations.add(MockMvcResultMatchers.jsonPath("$.*.content.id", hasSize(6)));
        // 6 = 5 roles and the added role TEST_ROLE has two permissions
        expectations.add(MockMvcResultMatchers.jsonPath("$.*.content.permissions", hasSize(6)));
        // 3 = 3 roles has a parent (public, project_admin, instance_admin has no parent)
        expectations.add(MockMvcResultMatchers.jsonPath("$.*.content.parentRole", hasSize(3)));
        performDefaultGet(apiRoles, expectations, "TODO Error message");
    }

    /**
     * Check that the system allows to delete a role.
     *
     * @throws JwtException
     *
     */
    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_210")
    @Purpose("Check that the system allows to delete a role.")
    public void removeRole() throws JwtException {
        final long nRole = roleRepository.count();
        // Create a non-native role
        final List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(status().isOk());
        performDefaultDelete(apiRolesId, expectations, "TODO Error message", roleTest.getId());

        jwtService.injectToken(DEFAULT_TENANT, DefaultRole.PROJECT_ADMIN.toString(), "");
        Assert.assertEquals(nRole - 1, roleRepository.count());
    }

    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_210")
    @Purpose("Check that the system allows to retrieve all resources accesses of a role.")
    public void retrieveRoleResourcesAccessList() {
        final List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(status().isOk());
        performDefaultGet(apiRolesPermissions, expectations, "TODO Error message", roleTest.getId());
    }

    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_210")
    @Purpose("Check that the system allows to update resources accesses of a role.")
    public void updateRoleResourcesAccess() throws EntityNotFoundException {

        final Set<ResourcesAccess> newPermissionList = roleService.retrieveRoleResourcesAccesses(roleTest.getId());

        newPermissionList.add(resourcesAccessRepository
                .save(new ResourcesAccess(0L, "new", "new", "new", "Controller", HttpVerb.PUT)));
        newPermissionList.add(resourcesAccessRepository
                .save(new ResourcesAccess(1L, "neww", "neww", "neww", "Controller", HttpVerb.DELETE)));

        final List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(status().isNoContent());
        performDefaultPut(apiRolesPermissions, newPermissionList, expectations, "TODO Error message", roleTest.getId());
    }

    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_210")
    @Purpose("Check that the system allows to remove all resources accesses of a role.")
    public void clearRoleResourcesAccess() {
        final List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(status().isNoContent());
        performDefaultDelete(apiRolesPermissions, expectations, "TODO Error message", roleTest.getId());
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
     *
     * Check hierarchy of roles
     *
     * @since 1.0-SNAPSHOT
     */
    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_210")
    @Purpose("Check hierachy of roles")
    public void retrieveInheritedRoles() {
        final Set<Role> roles = roleService.retrieveInheritedRoles(publicRole);
        // Number of roles should be all Default roles except PUBLIC(which is parent of the hierarchy wanted) and
        // PROJECT_ADMIN, INSTANCE_ADMIN which has no parent plus the default ROLE Create for those tests.
        int defaultRoleSize = DefaultRole.values().length;
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
