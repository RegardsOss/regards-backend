/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.microservices.administration;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.ResultMatcher;

import fr.cnes.regards.microservices.core.security.jwt.JWTService;
import fr.cnes.regards.microservices.core.test.RegardsIntegrationTest;
import fr.cnes.regards.modules.accessRights.domain.HttpVerb;
import fr.cnes.regards.modules.accessRights.domain.ResourcesAccess;
import fr.cnes.regards.modules.accessRights.domain.Role;
import fr.cnes.regards.modules.accessRights.service.IRoleService;

/**
 * Just Test the REST API so status code. Correction is left to others.
 *
 * @author xbrochar
 *
 */
public class RoleControllerIT extends RegardsIntegrationTest {

    @Autowired
    private JWTService jwtService_;

    private String jwt_;

    private String apiRoles;

    private String apiRolesId;

    private String apiRolesPermissions;

    private String apiRolesUsers;

    @Autowired
    private IRoleService roleService_;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Value("${root.admin.login:admin}")
    private String rootAdminLogin;

    @Value("${root.admin.password:admin}")
    private String rootAdminPassword;

    @Before
    public void init() {
        setLogger(LoggerFactory.getLogger(ProjectControllerIT.class));
        jwt_ = jwtService_.generateToken("PROJECT", "email", "SVG", "USER");
        apiRoles = "/roles";
        apiRolesId = apiRoles + "/{role_id}";
        apiRolesPermissions = apiRolesId + "/permissions";
        apiRolesUsers = apiRolesId + "/users";
    }

    @Test
    public void retrieveRoleList() {
        List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(status().isOk());
        performGet(apiRoles, jwt_, expectations, "TODO Error message");
    }

    @Test
    public void createRole() {
        Role newRole = new Role(15464L, "new role", null, null, null);

        List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(status().isCreated());
        performPost(apiRoles, jwt_, newRole, expectations, "TODO Error message");

        expectations = new ArrayList<>(1);
        expectations.add(status().isConflict());
        performPost(apiRoles, jwt_, newRole, expectations, "TODO Error message");
    }

    @Test
    public void retrieveRole() {
        Long roleId = 0L;
        assertTrue(roleService_.existRole(roleId));

        List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(status().isOk());
        performGet(apiRolesId, jwt_, expectations, "TODO Error message", roleId);

        Long wrongRoleId = 46453L;
        assertFalse(this.roleService_.existRole(wrongRoleId));
        expectations = new ArrayList<>(1);
        expectations.add(status().isNotFound());
        performGet(apiRolesId, jwt_, expectations, "TODO Error message", wrongRoleId);
    }

    @Test
    @DirtiesContext
    public void updateRole() {
        Long roleId = 0L;
        assertTrue(roleService_.existRole(roleId));
        Role updated = roleService_.retrieveRole(roleId);
        updated.setName("newName");

        List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(status().isOk());
        performPut(apiRolesId, jwt_, updated, expectations, "TODO Error message", roleId);

        Long notSameID = 41554L;
        Role notUpdated = new Role(notSameID, null, null, null, null);

        expectations = new ArrayList<>(1);
        expectations.add(status().isBadRequest());
        performPut(apiRolesId, jwt_, notUpdated, expectations, "TODO Error message", roleId);
    }

    @Test
    @DirtiesContext
    public void removeRole() {
        Long roleId = 0L;

        List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(status().isOk());
        performDelete(apiRolesId, jwt_, expectations, "TODO Error message", roleId);
    }

    @Test
    public void retrieveRoleResourcesAccessList() {
        Long roleId = 0L;

        List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(status().isOk());
        performGet(apiRolesPermissions, jwt_, expectations, "TODO Error message", roleId);
    }

    @Test
    @DirtiesContext
    public void updateRoleResourcesAccess() {
        Long roleId = 0L;

        List<ResourcesAccess> newPermissionList = new ArrayList<>();
        newPermissionList.add(new ResourcesAccess(463L, "new", "new", "new", HttpVerb.PUT));
        newPermissionList.add(new ResourcesAccess(350L, "neww", "neww", "neww", HttpVerb.DELETE));

        List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(status().isOk());
        performPut(apiRolesPermissions, jwt_, newPermissionList, expectations, "TODO Error message", roleId);
    }

    @Test
    @DirtiesContext
    public void clearRoleResourcesAccess() {
        Long roleId = 0L;

        List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(status().isOk());
        performDelete(apiRolesPermissions, jwt_, expectations, "TODO Error message", roleId);
    }

    @Test
    public void retrieveRoleProjectUserList() {
        Long roleId = 0L;

        List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(status().isOk());
        performGet(apiRolesUsers, jwt_, expectations, "TODO Error message", roleId);
    }

}
