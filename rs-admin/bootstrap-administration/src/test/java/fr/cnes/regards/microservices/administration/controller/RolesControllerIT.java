/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.microservices.administration.controller;

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
import org.springframework.web.bind.annotation.RequestMethod;

import fr.cnes.regards.microservices.core.security.endpoint.MethodAuthorizationService;
import fr.cnes.regards.microservices.core.test.AbstractRegardsIntegrationTest;
import fr.cnes.regards.microservices.core.test.report.annotation.Purpose;
import fr.cnes.regards.microservices.core.test.report.annotation.Requirement;
import fr.cnes.regards.modules.accessRights.domain.HttpVerb;
import fr.cnes.regards.modules.accessRights.domain.projects.ResourcesAccess;
import fr.cnes.regards.modules.accessRights.domain.projects.Role;
import fr.cnes.regards.modules.accessRights.service.IRoleService;
import fr.cnes.regards.security.utils.jwt.JWTService;

/**
 * Just Test the REST API so status code. Correction is left to others.
 *
 * @author xbrochar
 *
 */
public class RolesControllerIT extends AbstractRegardsIntegrationTest {

    @Autowired
    private JWTService jwtService_;

    @Autowired
    private MethodAuthorizationService authService_;

    private String jwt_;

    private String apiRoles;

    private String apiRolesId;

    private String apiRolesPermissions;

    private String apiRolesUsers;

    @Autowired
    private IRoleService roleService_;

    @Rule
    public ExpectedException thrown_ = ExpectedException.none();

    @Value("${root.admin.login:admin}")
    private String rootAdminLogin_;

    @Value("${root.admin.password:admin}")
    private String rootAdminPassword_;

    @Before
    public void init() {
        setLogger(LoggerFactory.getLogger(ProjectsControllerIT.class));
        jwt_ = jwtService_.generateToken("PROJECT", "email", "SVG", "USER");
        authService_.setAuthorities("/roles", RequestMethod.GET, "USER");
        authService_.setAuthorities("/roles", RequestMethod.POST, "USER");
        authService_.setAuthorities("/roles/{role_id}", RequestMethod.GET, "USER");
        authService_.setAuthorities("/roles/{role_id}", RequestMethod.PUT, "USER");
        authService_.setAuthorities("/roles/{role_id}", RequestMethod.DELETE, "USER");
        authService_.setAuthorities("/roles/{role_id}/permissions", RequestMethod.GET, "USER");
        authService_.setAuthorities("/roles/{role_id}/permissions", RequestMethod.PUT, "USER");
        authService_.setAuthorities("/roles/{role_id}/permissions", RequestMethod.DELETE, "USER");
        authService_.setAuthorities("/roles/{role_id}/users", RequestMethod.GET, "USER");
        apiRoles = "/roles";
        apiRolesId = apiRoles + "/{role_id}";
        apiRolesPermissions = apiRolesId + "/permissions";
        apiRolesUsers = apiRolesId + "/users";
    }

    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_210")
    @Purpose("Check that the allows to retrieve roles.")
    public void retrieveRoleList() {
        List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(status().isOk());
        performGet(apiRoles, jwt_, expectations, "TODO Error message");
    }

    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_210")
    @Purpose("Check that the system allows to create a role and handle fail cases.")
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
    @Requirement("REGARDS_DSL_ADM_ADM_210")
    @Purpose("Check that the allows to retrieve a single role and handle fail cases.")
    public void retrieveRole() {
        Long roleId = 0L;
        assertTrue(roleService_.existRole(roleId));

        List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(status().isOk());
        performGet(apiRolesId, jwt_, expectations, "TODO Error message", roleId);

        Long wrongRoleId = 46453L;
        assertFalse(roleService_.existRole(wrongRoleId));
        expectations = new ArrayList<>(1);
        expectations.add(status().isNotFound());
        performGet(apiRolesId, jwt_, expectations, "TODO Error message", wrongRoleId);
    }

    @Test
    @DirtiesContext
    @Requirement("REGARDS_DSL_ADM_ADM_210")
    @Purpose("Check that the system allows to update a role and handle fail cases.")
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
    @Requirement("REGARDS_DSL_ADM_ADM_210")
    @Purpose("Check that the system allows to delete a role.")
    public void removeRole() {
        Long roleId = 0L;

        List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(status().isOk());
        performDelete(apiRolesId, jwt_, expectations, "TODO Error message", roleId);
    }

    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_210")
    @Purpose("Check that the system allows to retrieve all resources accesses of a role.")
    public void retrieveRoleResourcesAccessList() {
        Long roleId = 0L;

        List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(status().isOk());
        performGet(apiRolesPermissions, jwt_, expectations, "TODO Error message", roleId);
    }

    @Test
    @DirtiesContext
    @Requirement("REGARDS_DSL_ADM_ADM_210")
    @Purpose("Check that the system allows to update resources accesses of a role.")
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
    @Requirement("REGARDS_DSL_ADM_ADM_210")
    @Purpose("Check that the system allows to remove all resources accesses of a role.")
    public void clearRoleResourcesAccess() {
        Long roleId = 0L;

        List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(status().isOk());
        performDelete(apiRolesPermissions, jwt_, expectations, "TODO Error message", roleId);
    }

    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_210")
    @Purpose("Check that the system allows to retrieve all users of a role.")
    public void retrieveRoleProjectUserList() {
        Long roleId = 0L;

        List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(status().isOk());
        performGet(apiRolesUsers, jwt_, expectations, "TODO Error message", roleId);
    }

}
