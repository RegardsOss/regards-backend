/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.rest;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.web.bind.annotation.RequestMethod;

import fr.cnes.regards.framework.module.rest.exception.AlreadyExistingException;
import fr.cnes.regards.framework.module.rest.exception.ModuleEntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.OperationForbiddenException;
import fr.cnes.regards.framework.security.endpoint.MethodAuthorizationService;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import fr.cnes.regards.modules.accessrights.dao.projects.IResourcesAccessRepository;
import fr.cnes.regards.modules.accessrights.domain.HttpVerb;
import fr.cnes.regards.modules.accessrights.domain.projects.ResourcesAccess;
import fr.cnes.regards.modules.accessrights.domain.projects.Role;
import fr.cnes.regards.modules.accessrights.service.role.RoleService;

/**
 * Integration tests for Roles REST Controller.
 *
 * @author Sébastien Binda
 * @author Xavier-Alexandre Brochard
 * @since 1.0-SNAPSHOT
 */
@EnableAutoConfiguration(exclude = DataSourceAutoConfiguration.class)
public class RolesControllerIT extends AbstractAdministrationIT {

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(RolesControllerIT.class);

    @Autowired
    private MethodAuthorizationService authService;

    private String apiRoles;

    private String apiRolesId;

    private String apiRolesName;

    private String apiRolesPermissions;

    private String apiRolesUsers;

    @Autowired
    private RoleService roleService;

    @Autowired
    private IResourcesAccessRepository resourcesAccessRepository;

    @Rule
    public ExpectedException thrown_ = ExpectedException.none();

    @Value("${root.admin.login:admin}")
    private String rootAdminLogin;

    @Value("${root.admin.password:admin}")
    private String rootAdminPassword;

    @Override
    public void init() {
        authService.setAuthorities(PROJECT_TEST_NAME, "/roles", RequestMethod.GET, ROLE_TEST);
        authService.setAuthorities(PROJECT_TEST_NAME, "/roles", RequestMethod.POST, ROLE_TEST);
        authService.setAuthorities(PROJECT_TEST_NAME, "/roles/{role_name}", RequestMethod.GET, ROLE_TEST);
        authService.setAuthorities(PROJECT_TEST_NAME, "/roles/{role_id}", RequestMethod.PUT, ROLE_TEST);
        authService.setAuthorities(PROJECT_TEST_NAME, "/roles/{role_id}", RequestMethod.DELETE, ROLE_TEST);
        authService.setAuthorities(PROJECT_TEST_NAME, "/roles/{role_id}/permissions", RequestMethod.GET, ROLE_TEST);
        authService.setAuthorities(PROJECT_TEST_NAME, "/roles/{role_id}/permissions", RequestMethod.PUT, ROLE_TEST);
        authService.setAuthorities(PROJECT_TEST_NAME, "/roles/{role_id}/permissions", RequestMethod.DELETE, ROLE_TEST);
        authService.setAuthorities(PROJECT_TEST_NAME, "/roles/{role_id}/users", RequestMethod.GET, ROLE_TEST);
        apiRoles = "/roles";
        apiRolesId = apiRoles + "/{role_id}";
        apiRolesName = apiRoles + "/{role_name}";
        apiRolesPermissions = apiRolesId + "/permissions";
        apiRolesUsers = apiRolesId + "/users";
    }

    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_210")
    @Purpose("Check that the allows to retrieve roles.")
    public void retrieveRoleList() {
        final List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(status().isOk());
        performGet(apiRoles, token, expectations, "TODO Error message");
    }

    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_210")
    @Purpose("Check that the system allows to create a role and handle fail cases.")
    public void createRole() throws OperationForbiddenException, ModuleEntityNotFoundException {
        final String newRoleName = "NEW_ROLE";
        if (roleService.existByName(newRoleName)) {
            final Role toDelete = roleService.retrieveRole(newRoleName);
            roleService.removeRole(toDelete.getId());
        }
        final Role newRole = new Role(newRoleName, publicRole);

        List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(status().isCreated());
        performPost(apiRoles, token, newRole, expectations, "TODO Error message");

        expectations = new ArrayList<>(1);
        expectations.add(status().isConflict());
        performPost(apiRoles, token, newRole, expectations, "TODO Error message");
    }

    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_210")
    @Purpose("Check that the allows to retrieve a single role and handle fail cases.")
    public void retrieveRole() {
        List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(status().isOk());
        performGet(apiRolesName, token, expectations, "TODO Error message", DefaultRole.REGISTERED_USER);

        final String wrongRoleName = "WRONG_ROLE";
        expectations = new ArrayList<>(1);
        expectations.add(status().isNotFound());
        performGet(apiRolesName, token, expectations, "TODO Error message", wrongRoleName);
    }

    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_210")
    @Purpose("Check that the system allows to update a role and handle fail cases.")
    public void updateRole() throws AlreadyExistingException {
        // Grab a role and change something
        roleTest.setCorsRequestsAuthorizationEndDate(LocalDateTime.now().plusDays(2));

        // Regular case
        List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(status().isOk());
        performPut(apiRolesId, token, roleTest, expectations, "TODO Error message", roleTest.getId());

        // Fail case: ids differ
        expectations = new ArrayList<>(1);
        expectations.add(MockMvcResultMatchers.status().isBadRequest());
        performPut(apiRolesId, token, roleTest, expectations, "TODO Error message", 99L);
    }

    /**
     * Check that the system prevents from deleting a native role.
     */
    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_210")
    @Purpose("Check that the system prevents from deleting a native role.")
    public void removeRoleNative() {
        // Role public is native, we use this one

        final List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(status().isForbidden());
        performDelete(apiRolesId, token, expectations, "TODO Error message", publicRole.getId());
    }

    /**
     * Check that the system allows to delete a role.
     *
     * @throws AlreadyExistingException
     */
    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_210")
    @Purpose("Check that the system allows to delete a role.")
    public void removeRole() throws AlreadyExistingException {
        // Create a non-native role
        final List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(status().isOk());
        performDelete(apiRolesId, token, expectations, "TODO Error message", roleTest.getId());
    }

    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_210")
    @Purpose("Check that the system allows to retrieve all resources accesses of a role.")
    public void retrieveRoleResourcesAccessList() {
        final List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(status().isOk());
        performGet(apiRolesPermissions, token, expectations, "TODO Error message", roleTest.getId());
    }

    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_210")
    @Purpose("Check that the system allows to update resources accesses of a role.")
    public void updateRoleResourcesAccess() throws ModuleEntityNotFoundException {

        final List<ResourcesAccess> newPermissionList = roleService.retrieveRoleResourcesAccessList(roleTest.getId());

        newPermissionList
                .add(resourcesAccessRepository.save(new ResourcesAccess(0L, "new", "new", "new", HttpVerb.PUT)));
        newPermissionList
                .add(resourcesAccessRepository.save(new ResourcesAccess(1L, "neww", "neww", "neww", HttpVerb.DELETE)));

        final List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(status().isOk());
        performPut(apiRolesPermissions, token, newPermissionList, expectations, "TODO Error message", roleTest.getId());
    }

    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_210")
    @Purpose("Check that the system allows to remove all resources accesses of a role.")
    public void clearRoleResourcesAccess() {
        final List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(status().isOk());
        performDelete(apiRolesPermissions, token, expectations, "TODO Error message", roleTest.getId());
    }

    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_210")
    @Purpose("Check that the system allows to retrieve all users of a role.")
    public void retrieveRoleProjectUserList() {
        final List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(status().isOk());
        performGet(apiRolesUsers, token, expectations, "TODO Error message", roleTest.getId());
    }

    @Override
    protected Logger getLogger() {
        return LOG;
    }

}
