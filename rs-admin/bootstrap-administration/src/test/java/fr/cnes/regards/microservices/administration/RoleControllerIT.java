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
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.web.servlet.ResultMatcher;

import fr.cnes.regards.microservices.core.security.jwt.JWTService;
import fr.cnes.regards.microservices.modules.test.RegardsIntegrationTest;
import fr.cnes.regards.modules.accessRights.dao.IRoleRepository;
import fr.cnes.regards.modules.accessRights.domain.HttpVerb;
import fr.cnes.regards.modules.accessRights.domain.ResourcesAccess;
import fr.cnes.regards.modules.accessRights.domain.Role;
import fr.cnes.regards.modules.accessRights.service.IRoleService;
import fr.cnes.regards.modules.accessRights.service.RoleService;

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
    @Qualifier("roleRepositoryStub")
    private IRoleRepository roleRepository_;

    private IRoleService roleService_;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Value("${root.admin.login:admin}")
    private String rootAdminLogin;

    @Value("${root.admin.password:admin}")
    private String rootAdminPassword;

    @Before
    public void setup() {
        setLogger(LoggerFactory.getLogger(ProjectControllerIT.class));
        jwt_ = jwtService_.generateToken("PROJECT", "email", "SVG", "USER");
        roleService_ = new RoleService(roleRepository_);
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
        // // we have to use exchange instead of getForEntity as long as we use List otherwise the response body is not
        // // well cast.
        // ParameterizedTypeReference<List<Role>> typeRef = new ParameterizedTypeReference<List<Role>>() {
        // };
        // ResponseEntity<List<Role>> response = restTemplate.exchange(apiRoles, HttpMethod.GET, null, typeRef);
        // assertEquals(HttpStatus.OK, response.getStatusCode());
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

        // ResponseEntity<Role> response = restTemplate.postForEntity(apiRoles, newRole, Role.class);
        // assertEquals(HttpStatus.CREATED, response.getStatusCode());
        //
        // ResponseEntity<Role> responseConflict = restTemplate.postForEntity(apiRoles, newRole, Role.class);
        // assertEquals(HttpStatus.CONFLICT, responseConflict.getStatusCode());
    }

    @Test
    public void retrieveRole() {
        Long roleId = 0L;
        assertTrue(roleService_.existRole(roleId));

        List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(status().isOk());
        performGet(apiRolesId, jwt_, expectations, "TODO Error message", roleId);

        // ParameterizedTypeReference<Role> typeRef = new ParameterizedTypeReference<Role>() {
        // };
        // ResponseEntity<Role> response = restTemplate.exchange(this.apiRolesId, HttpMethod.GET, null, typeRef,
        // roleId);
        // assertEquals(HttpStatus.OK, response.getStatusCode());

        Long wrongRoleId = 46453L;
        assertFalse(this.roleService_.existRole(wrongRoleId));
        expectations = new ArrayList<>(1);
        expectations.add(status().isNotFound());
        performGet(apiRolesId, jwt_, expectations, "TODO Error message", wrongRoleId);

        // ResponseEntity<Object> responseNotFound = restTemplate.exchange(this.apiRolesId, HttpMethod.GET, null,
        // Object.class, wrongRoleId);
        // assertEquals(HttpStatus.NOT_FOUND, responseNotFound.getStatusCode());
    }

    @Test
    public void updateRole() {
        Long roleId = 0L;
        assertTrue(roleService_.existRole(roleId));
        Role updated = roleService_.retrieveRole(roleId);
        updated.setName("newName");

        List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(status().isOk());
        performPut(apiRolesId, jwt_, updated, expectations, "TODO Error message", roleId);

        // ParameterizedTypeReference<Void> typeRef = new ParameterizedTypeReference<Void>() {
        // };
        // HttpEntity<Role> request = new HttpEntity<>(updated);
        // ResponseEntity<Void> response = restTemplate.exchange(apiRolesId, HttpMethod.PUT, request, typeRef, roleId);
        // // if that's the same functional ID and the parameter is valid:
        // assertEquals(HttpStatus.OK, response.getStatusCode());
        //
        // // if that's not the same functional ID and the parameter is valid:
        Long notSameID = 41554L;
        Role notUpdated = new Role(notSameID, null, null, null, null);

        expectations = new ArrayList<>(1);
        expectations.add(status().isBadRequest());
        performPut(apiRolesId, jwt_, notUpdated, expectations, "TODO Error message", roleId);
        // HttpEntity<Role> requestOperationNotAllowed = new HttpEntity<>(notUpdated);
        // ResponseEntity<Void> responseOperationNotAllowed = restTemplate
        // .exchange(apiRolesId, HttpMethod.PUT, requestOperationNotAllowed, typeRef, roleId);
        // assertEquals(HttpStatus.BAD_REQUEST, responseOperationNotAllowed.getStatusCode());
    }

    @Test
    public void removeRole() {
        Long roleId = 0L;

        List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(status().isOk());
        performDelete(apiRolesId, jwt_, expectations, "TODO Error message", roleId);
        // ParameterizedTypeReference<Void> typeRef = new ParameterizedTypeReference<Void>() {
        // };
        // ResponseEntity<Void> response = restTemplate.exchange(this.apiRolesId, HttpMethod.DELETE, null, typeRef,
        // roleId);
        // assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    public void retrieveRoleResourcesAccessList() {
        Long roleId = 0L;

        List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(status().isOk());
        performGet(apiRolesPermissions, jwt_, expectations, "TODO Error message", roleId);
        // ParameterizedTypeReference<List<ResourcesAccess>> typeRef = new
        // ParameterizedTypeReference<List<ResourcesAccess>>() {
        // };
        // ResponseEntity<List<ResourcesAccess>> response = restTemplate.exchange(apiRolesPermissions, HttpMethod.GET,
        // null, typeRef, roleId);
        // assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    public void updateRoleResourcesAccess() {
        Long roleId = 0L;

        List<ResourcesAccess> newPermissionList = new ArrayList<>();
        newPermissionList.add(new ResourcesAccess(463L, "new", "new", "new", HttpVerb.PUT));
        newPermissionList.add(new ResourcesAccess(350L, "neww", "neww", "neww", HttpVerb.DELETE));

        List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(status().isOk());
        performPut(apiRolesPermissions, jwt_, newPermissionList, expectations, "TODO Error message", roleId);

        // ParameterizedTypeReference<Void> typeRef = new ParameterizedTypeReference<Void>() {
        // };
        // HttpEntity<List<ResourcesAccess>> request = new HttpEntity<>(newPermissionList);
        // ResponseEntity<Void> response = restTemplate.exchange(apiRolesPermissions, HttpMethod.PUT, request, typeRef,
        // roleId);
        // assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    public void clearRoleResourcesAccess() {
        Long roleId = 0L;

        List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(status().isOk());
        performDelete(apiRolesPermissions, jwt_, expectations, "TODO Error message", roleId);
        // ParameterizedTypeReference<Void> typeRef = new ParameterizedTypeReference<Void>() {
        // };
        // ResponseEntity<Void> response = restTemplate.exchange(this.apiRolesPermissions, HttpMethod.DELETE, null,
        // typeRef, roleId);
        // assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    public void retrieveRoleProjectUserList() {
        Long roleId = 0L;

        List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(status().isOk());
        performGet(apiRolesUsers, jwt_, expectations, "TODO Error message", roleId);
        // ParameterizedTypeReference<List<ProjectUser>> typeRef = new ParameterizedTypeReference<List<ProjectUser>>() {
        // };
        // ResponseEntity<List<ProjectUser>> response = restTemplate.exchange(apiRolesUsers, HttpMethod.GET, null,
        // typeRef,
        // roleId);
        // assertEquals(HttpStatus.OK, response.getStatusCode());
        //
    }

}
