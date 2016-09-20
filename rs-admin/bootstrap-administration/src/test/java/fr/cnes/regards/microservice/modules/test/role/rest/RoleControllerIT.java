/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.microservice.modules.test.role.rest;

import org.junit.Rule;
import org.junit.rules.ExpectedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.web.client.TestRestTemplate;

import fr.cnes.regards.microservices.modules.test.RegardsIntegrationTest;
import fr.cnes.regards.modules.accessRights.service.IRoleService;

/**
 * Just Test the REST API so status code. Correction is left to others.
 *
 * @author xbrochar
 *
 */
public class RoleControllerIT extends RegardsIntegrationTest {

    private TestRestTemplate restTemplate;

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

    // @Before
    // public void init() {
    // if (restTemplate == null) {
    // restTemplate = buildOauth2RestTemplate("acme", "acmesecret", "admin", "admin", "");
    // }
    // apiRoles = getApiEndpoint().concat("roles");
    // apiRolesId = apiRoles + URL_SEPARATOR + "{role_id}";
    // apiRolesPermissions = apiRolesId + URL_SEPARATOR + "permissions";
    // apiRolesUsers = apiRolesId + URL_SEPARATOR + "users";
    //
    // // Reset the items before each test
    // // roleService_ = new Ro
    // }
    //
    // @Test
    // public void retrieveRoleList() {
    // // we have to use exchange instead of getForEntity as long as we use List otherwise the response body is not
    // // well cast.
    // ParameterizedTypeReference<List<Role>> typeRef = new ParameterizedTypeReference<List<Role>>() {
    // };
    // ResponseEntity<List<Role>> response = restTemplate.exchange(apiRoles, HttpMethod.GET, null, typeRef);
    // assertEquals(HttpStatus.OK, response.getStatusCode());
    // }
    //
    // @Test
    // public void createRole() {
    // Role newRole = new Role(15464L, "new role", null, null, null);
    //
    // ResponseEntity<Role> response = restTemplate.postForEntity(apiRoles, newRole, Role.class);
    // assertEquals(HttpStatus.CREATED, response.getStatusCode());
    //
    // ResponseEntity<Role> responseConflict = restTemplate.postForEntity(apiRoles, newRole, Role.class);
    // assertEquals(HttpStatus.CONFLICT, responseConflict.getStatusCode());
    // }
    //
    // @Test
    // public void retrieveRole() {
    // Long roleId = 0L;
    // assertFalse(!this.roleService_.existRole(roleId));
    //
    // ParameterizedTypeReference<Role> typeRef = new ParameterizedTypeReference<Role>() {
    // };
    // ResponseEntity<Role> response = restTemplate.exchange(this.apiRolesId, HttpMethod.GET, null, typeRef, roleId);
    // assertEquals(HttpStatus.OK, response.getStatusCode());
    //
    // Long wrongRoleId = 46453L;
    // assertFalse(this.roleService_.existRole(wrongRoleId));
    // ResponseEntity<Object> responseNotFound = restTemplate.exchange(this.apiRolesId, HttpMethod.GET, null,
    // Object.class, wrongRoleId);
    // assertEquals(HttpStatus.NOT_FOUND, responseNotFound.getStatusCode());
    // }
    //
    // @Test
    // public void updateRole() {
    // Long roleId = 0L;
    // assertTrue(roleService_.existRole(roleId));
    // Role updated = roleService_.retrieveRole(roleId);
    // updated.setName("newName");
    // ParameterizedTypeReference<Void> typeRef = new ParameterizedTypeReference<Void>() {
    // };
    // HttpEntity<Role> request = new HttpEntity<>(updated);
    // ResponseEntity<Void> response = restTemplate.exchange(apiRolesId, HttpMethod.PUT, request, typeRef, roleId);
    // // if that's the same functional ID and the parameter is valid:
    // assertEquals(HttpStatus.OK, response.getStatusCode());
    //
    // // if that's not the same functional ID and the parameter is valid:
    // Long notSameID = 41554L;
    // Role notUpdated = new Role(notSameID, null, null, null, null);
    // HttpEntity<Role> requestOperationNotAllowed = new HttpEntity<>(notUpdated);
    // ResponseEntity<Void> responseOperationNotAllowed = restTemplate
    // .exchange(apiRolesId, HttpMethod.PUT, requestOperationNotAllowed, typeRef, roleId);
    // assertEquals(HttpStatus.BAD_REQUEST, responseOperationNotAllowed.getStatusCode());
    // }
    //
    // @Test
    // public void removeRole() {
    // Long roleId = 0L;
    // ParameterizedTypeReference<Void> typeRef = new ParameterizedTypeReference<Void>() {
    // };
    // ResponseEntity<Void> response = restTemplate.exchange(this.apiRolesId, HttpMethod.DELETE, null, typeRef,
    // roleId);
    // assertEquals(HttpStatus.OK, response.getStatusCode());
    // }
    //
    // @Test
    // public void retrieveRoleResourcesAccessList() {
    // Long roleId = 0L;
    // ParameterizedTypeReference<List<ResourcesAccess>> typeRef = new
    // ParameterizedTypeReference<List<ResourcesAccess>>() {
    // };
    // ResponseEntity<List<ResourcesAccess>> response = restTemplate.exchange(apiRolesPermissions, HttpMethod.GET,
    // null, typeRef, roleId);
    // assertEquals(HttpStatus.OK, response.getStatusCode());
    // }
    //
    // @Test
    // public void updateRoleResourcesAccess() {
    // Long roleId = 0L;
    //
    // List<ResourcesAccess> newPermissionList = new ArrayList<>();
    // newPermissionList.add(new ResourcesAccess(463L, "new", "new", "new", HttpVerb.PUT));
    // newPermissionList.add(new ResourcesAccess(350L, "neww", "neww", "neww", HttpVerb.DELETE));
    //
    // ParameterizedTypeReference<Void> typeRef = new ParameterizedTypeReference<Void>() {
    // };
    // HttpEntity<List<ResourcesAccess>> request = new HttpEntity<>(newPermissionList);
    // ResponseEntity<Void> response = restTemplate.exchange(apiRolesPermissions, HttpMethod.PUT, request, typeRef,
    // roleId);
    // assertEquals(HttpStatus.OK, response.getStatusCode());
    // }
    //
    // @Test
    // public void clearRoleResourcesAccess() {
    // Long roleId = 0L;
    //
    // ParameterizedTypeReference<Void> typeRef = new ParameterizedTypeReference<Void>() {
    // };
    // ResponseEntity<Void> response = restTemplate.exchange(this.apiRolesPermissions, HttpMethod.DELETE, null,
    // typeRef, roleId);
    // assertEquals(HttpStatus.OK, response.getStatusCode());
    // }
    //
    // @Test
    // public void retrieveRoleProjectUserList() {
    // Long roleId = 0L;
    //
    // ParameterizedTypeReference<List<ProjectUser>> typeRef = new ParameterizedTypeReference<List<ProjectUser>>() {
    // };
    // ResponseEntity<List<ProjectUser>> response = restTemplate.exchange(apiRolesUsers, HttpMethod.GET, null, typeRef,
    // roleId);
    // assertEquals(HttpStatus.OK, response.getStatusCode());
    //
    // }

}
