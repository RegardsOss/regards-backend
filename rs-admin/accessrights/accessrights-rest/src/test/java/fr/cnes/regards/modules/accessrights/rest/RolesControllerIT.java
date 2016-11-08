/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.rest;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.web.bind.annotation.RequestMethod;

import fr.cnes.regards.framework.module.rest.exception.AlreadyExistingException;
import fr.cnes.regards.framework.security.endpoint.MethodAuthorizationService;
import fr.cnes.regards.framework.security.utils.jwt.JWTService;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import fr.cnes.regards.modules.accessrights.domain.HttpVerb;
import fr.cnes.regards.modules.accessrights.domain.projects.DefaultRoleNames;
import fr.cnes.regards.modules.accessrights.domain.projects.ResourcesAccess;
import fr.cnes.regards.modules.accessrights.domain.projects.Role;
import fr.cnes.regards.modules.accessrights.service.IRoleService;

/**
 * Integration tests for Roles REST Controller.
 *
 * @author sbinda
 * @author Xavier-Alexandre Brochard
 * @since 1.0-SNAPSHOT
 */
@EnableAutoConfiguration(exclude = DataSourceAutoConfiguration.class)
public class RolesControllerIT extends AbstractAdministrationIT {

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(RolesControllerIT.class);

    /**
     * The role PUBLIC, usefull for creating new roles
     */
    private static Role rolePublic;

    @Autowired
    private JWTService jwtService;

    @Autowired
    private MethodAuthorizationService authService;

    private String jwt;

    private String apiRoles;

    private String apiRolesId;

    private String apiRolesName;

    private String apiRolesPermissions;

    private String apiRolesUsers;

    @Autowired
    private IRoleService roleService;

    @Rule
    public ExpectedException thrown_ = ExpectedException.none();

    @Value("${root.admin.login:admin}")
    private String rootAdminLogin_;

    @Value("${root.admin.password:admin}")
    private String rootAdminPassword_;

    @Override
    public void init() {
        final String tenant = AbstractAdministrationIT.PROJECT_TEST_NAME;
        jwt = jwtService.generateToken(tenant, "email", DefaultRoleNames.PUBLIC.toString(), "USER");
        authService.setAuthorities(tenant, "/roles", RequestMethod.GET, "USER");
        authService.setAuthorities(tenant, "/roles", RequestMethod.POST, "USER");
        authService.setAuthorities(tenant, "/roles/{role_name}", RequestMethod.GET, "USER");
        authService.setAuthorities(tenant, "/roles/{role_id}", RequestMethod.PUT, "USER");
        authService.setAuthorities(tenant, "/roles/{role_id}", RequestMethod.DELETE, "USER");
        authService.setAuthorities(tenant, "/roles/{role_id}/permissions", RequestMethod.GET, "USER");
        authService.setAuthorities(tenant, "/roles/{role_id}/permissions", RequestMethod.PUT, "USER");
        authService.setAuthorities(tenant, "/roles/{role_id}/permissions", RequestMethod.DELETE, "USER");
        authService.setAuthorities(tenant, "/roles/{role_id}/users", RequestMethod.GET, "USER");
        apiRoles = "/roles";
        apiRolesId = apiRoles + "/{role_id}";
        apiRolesName = apiRoles + "/{role_name}";
        apiRolesPermissions = apiRolesId + "/permissions";
        apiRolesUsers = apiRolesId + "/users";

        rolePublic = roleService.retrieveRole(DefaultRoleNames.PUBLIC.toString());
    }

    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_210")
    @Purpose("Check that the allows to retrieve roles.")
    public void retrieveRoleList() {
        final List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(status().isOk());
        performGet(apiRoles, jwt, expectations, "TODO Error message");
    }

    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_210")
    @Purpose("Check that the system allows to create a role and handle fail cases.")
    public void createRole() {
        final Role newRole = new Role(15464L, "NEW_ROLE", rolePublic, null, null);

        List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(status().isCreated());
        performPost(apiRoles, jwt, newRole, expectations, "TODO Error message");

        expectations = new ArrayList<>(1);
        expectations.add(status().isConflict());
        performPost(apiRoles, jwt, newRole, expectations, "TODO Error message");
    }

    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_210")
    @Purpose("Check that the allows to retrieve a single role and handle fail cases.")
    public void retrieveRole() {
        final Long roleId = 0L;
        assertTrue(roleService.existRole(roleId));

        List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(status().isOk());
        performGet(apiRolesName, jwt, expectations, "TODO Error message", DefaultRoleNames.REGISTERED_USER);

        final Long wrongRoleId = 46453L;
        final String wrongRoleName = "WRONG_ROLE";
        assertFalse(roleService.existRole(wrongRoleId));
        expectations = new ArrayList<>(1);
        expectations.add(status().isNotFound());
        performGet(apiRolesName, jwt, expectations, "TODO Error message", wrongRoleName);
    }

    @Ignore
    @Test
    @DirtiesContext
    @Requirement("REGARDS_DSL_ADM_ADM_210")
    @Purpose("Check that the system allows to update a role and handle fail cases.")
    public void updateRole() throws AlreadyExistingException {
        // Create a new role in order to update it later
        final Long id = 99L;
        final Role toUpdate = new Role();
        toUpdate.setId(id);
        toUpdate.setName("NAME_TO_UPDATE");
        toUpdate.setDefault(false);
        toUpdate.setNative(false);
        toUpdate.setParentRole(rolePublic);
        toUpdate.setPermissions(new ArrayList<>());
        toUpdate.setProjectUsers(new ArrayList<>());
        roleService.createRole(toUpdate);

        // Update the role
        toUpdate.setName("UPDATED_NAME");

        List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(status().isOk());
        performPut(apiRolesId, jwt, toUpdate, expectations, "TODO Error message", id);

        final Long notSameID = 41554L;
        final Role notUpdated = new Role(notSameID, null, null, null, null);

        expectations = new ArrayList<>(1);
        expectations.add(MockMvcResultMatchers.status().isBadRequest());
        performPut(apiRolesId, jwt, notUpdated, expectations, "TODO Error message", id);
    }

    /**
     * Check that the system prevents from deleting a native role.
     */
    @Test
    @DirtiesContext
    @Requirement("REGARDS_DSL_ADM_ADM_210")
    @Purpose("Check that the system prevents from deleting a native role.")
    public void removeRoleNative() {
        // In RoleRepositoryStub, role of id 0 is native
        final Long roleId = 0L;

        final List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(status().isForbidden());
        performDelete(apiRolesId, jwt, expectations, "TODO Error message", roleId);
    }

    /**
     * Check that the system allows to delete a role.
     */
    @Test
    @DirtiesContext
    @Requirement("REGARDS_DSL_ADM_ADM_210")
    @Purpose("Check that the system allows to delete a role.")
    public void removeRole() {
        // In RoleRepositoryStub, role of id 5 is not native
        final Long roleId = 5L;

        final List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(status().isOk());
        performDelete(apiRolesId, jwt, expectations, "TODO Error message", roleId);
    }

    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_210")
    @Purpose("Check that the system allows to retrieve all resources accesses of a role.")
    public void retrieveRoleResourcesAccessList() {
        final Long roleId = 0L;

        final List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(status().isOk());
        performGet(apiRolesPermissions, jwt, expectations, "TODO Error message", roleId);
    }

    @Test
    @DirtiesContext
    @Requirement("REGARDS_DSL_ADM_ADM_210")
    @Purpose("Check that the system allows to update resources accesses of a role.")
    public void updateRoleResourcesAccess() {
        final Long roleId = 0L;

        final List<ResourcesAccess> newPermissionList = new ArrayList<>();
        newPermissionList.add(new ResourcesAccess(463L, "new", "new", "new", HttpVerb.PUT));
        newPermissionList.add(new ResourcesAccess(350L, "neww", "neww", "neww", HttpVerb.DELETE));

        final List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(status().isOk());
        performPut(apiRolesPermissions, jwt, newPermissionList, expectations, "TODO Error message", roleId);
    }

    @Test
    @DirtiesContext
    @Requirement("REGARDS_DSL_ADM_ADM_210")
    @Purpose("Check that the system allows to remove all resources accesses of a role.")
    public void clearRoleResourcesAccess() {
        final Long roleId = 0L;

        final List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(status().isOk());
        performDelete(apiRolesPermissions, jwt, expectations, "TODO Error message", roleId);
    }

    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_210")
    @Purpose("Check that the system allows to retrieve all users of a role.")
    public void retrieveRoleProjectUserList() {
        final Long roleId = 0L;

        final List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(status().isOk());
        performGet(apiRolesUsers, jwt, expectations, "TODO Error message", roleId);
    }

    @Override
    protected Logger getLogger() {
        return LOG;
    }

}
