/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.microservices.administration.controller;

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
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.web.bind.annotation.RequestMethod;

import fr.cnes.regards.framework.security.endpoint.MethodAuthorizationService;
import fr.cnes.regards.framework.security.utils.jwt.JWTService;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import fr.cnes.regards.modules.accessrights.domain.HttpVerb;
import fr.cnes.regards.modules.accessrights.domain.projects.ResourcesAccess;
import fr.cnes.regards.modules.accessrights.domain.projects.Role;
import fr.cnes.regards.modules.accessrights.service.IRoleService;

/**
 * Just Test the REST API so status code. Correction is left to others.
 *
 * @author xbrochar
 *
 */
public class RolesControllerIT extends AbstractAdministrationIT {

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(RolesControllerIT.class);

    @Autowired
    private JWTService jwtService;

    @Autowired
    private MethodAuthorizationService authService;

    private String jwt;

    private String apiRoles;

    private String apiRolesId;

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
        jwt = jwtService.generateToken(tenant, "email", "SVG", "USER");
        authService.setAuthorities(tenant, "/roles", RequestMethod.GET, "USER");
        authService.setAuthorities(tenant, "/roles", RequestMethod.POST, "USER");
        authService.setAuthorities(tenant, "/roles/{role_id}", RequestMethod.GET, "USER");
        authService.setAuthorities(tenant, "/roles/{role_id}", RequestMethod.PUT, "USER");
        authService.setAuthorities(tenant, "/roles/{role_id}", RequestMethod.DELETE, "USER");
        authService.setAuthorities(tenant, "/roles/{role_id}/permissions", RequestMethod.GET, "USER");
        authService.setAuthorities(tenant, "/roles/{role_id}/permissions", RequestMethod.PUT, "USER");
        authService.setAuthorities(tenant, "/roles/{role_id}/permissions", RequestMethod.DELETE, "USER");
        authService.setAuthorities(tenant, "/roles/{role_id}/users", RequestMethod.GET, "USER");
        apiRoles = "/roles";
        apiRolesId = apiRoles + "/{role_id}";
        apiRolesPermissions = apiRolesId + "/permissions";
        apiRolesUsers = apiRolesId + "/users";
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
        final Role newRole = new Role(15464L, "new role", null, null, null);

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
        performGet(apiRolesId, jwt, expectations, "TODO Error message", roleId);

        final Long wrongRoleId = 46453L;
        assertFalse(roleService.existRole(wrongRoleId));
        expectations = new ArrayList<>(1);
        expectations.add(status().isNotFound());
        performGet(apiRolesId, jwt, expectations, "TODO Error message", wrongRoleId);
    }

    @Ignore
    @Test
    @DirtiesContext
    @Requirement("REGARDS_DSL_ADM_ADM_210")
    @Purpose("Check that the system allows to update a role and handle fail cases.")
    public void updateRole() {
        final Long roleId = 0L;
        assertTrue(roleService.existRole(roleId));
        final Role updated = roleService.retrieveRole(roleId);
        updated.setName("newName");

        List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(status().isOk());
        performPut(apiRolesId, jwt, updated, expectations, "TODO Error message", roleId);

        final Long notSameID = 41554L;
        final Role notUpdated = new Role(notSameID, null, null, null, null);

        expectations = new ArrayList<>(1);
        expectations.add(status().isBadRequest());
        performPut(apiRolesId, jwt, notUpdated, expectations, "TODO Error message", roleId);
    }

    @Test
    @DirtiesContext
    @Requirement("REGARDS_DSL_ADM_ADM_210")
    @Purpose("Check that the system allows to delete a role.")
    public void removeRole() {
        final Long roleId = 0L;

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
