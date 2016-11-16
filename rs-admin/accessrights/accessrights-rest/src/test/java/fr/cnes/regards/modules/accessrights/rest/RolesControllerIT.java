/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.rest;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
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
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.framework.security.utils.jwt.JWTService;
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
    private RoleService roleService;

    @Autowired
    private IRoleRepository roleRepository;

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
        final String tenant = AbstractAdministrationIT.PROJECT_TEST_NAME;
        jwt = jwtService.generateToken(tenant, "email", DefaultRole.PUBLIC.toString(), "USER");
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

        // Flush repos
        roleRepository.deleteAll();
        resourcesAccessRepository.deleteAll();
        // Reinit
        roleService.initDefaultRoles();
        rolePublic = roleService.getRolePublic();
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
        final Role newRole = new Role("NEW_ROLE", rolePublic);

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
        List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(status().isOk());
        performGet(apiRolesName, jwt, expectations, "TODO Error message", DefaultRole.REGISTERED_USER);

        final String wrongRoleName = "WRONG_ROLE";
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
        // Grab a role a change something
        rolePublic.setCorsRequestsAuthorizationEndDate(LocalDateTime.now().plusDays(2));

        // Regular case
        List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(status().isOk());
        performPut(apiRolesId, jwt, rolePublic, expectations, "TODO Error message", rolePublic.getId());

        // Fail case: ids differ
        expectations = new ArrayList<>(1);
        expectations.add(MockMvcResultMatchers.status().isBadRequest());
        performPut(apiRolesId, jwt, rolePublic, expectations, "TODO Error message", 99L);
    }

    /**
     * Check that the system prevents from deleting a native role.
     */
    @Test
    @DirtiesContext
    @Requirement("REGARDS_DSL_ADM_ADM_210")
    @Purpose("Check that the system prevents from deleting a native role.")
    public void removeRoleNative() {
        // Role public is native, we use this one

        final List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(status().isForbidden());
        performDelete(apiRolesId, jwt, expectations, "TODO Error message", rolePublic.getId());
    }

    /**
     * Check that the system allows to delete a role.
     *
     * @throws AlreadyExistingException
     */
    @Test
    @DirtiesContext
    @Requirement("REGARDS_DSL_ADM_ADM_210")
    @Purpose("Check that the system allows to delete a role.")
    public void removeRole() throws AlreadyExistingException {
        // Create a non-native role
        final Role role = new Role("CUSTOM", rolePublic);
        roleService.createRole(role);

        final List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(status().isOk());
        performDelete(apiRolesId, jwt, expectations, "TODO Error message", role.getId());
    }

    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_210")
    @Purpose("Check that the system allows to retrieve all resources accesses of a role.")
    public void retrieveRoleResourcesAccessList() {
        final List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(status().isOk());
        performGet(apiRolesPermissions, jwt, expectations, "TODO Error message", rolePublic.getId());
    }

    @Test
    @DirtiesContext
    @Requirement("REGARDS_DSL_ADM_ADM_210")
    @Purpose("Check that the system allows to update resources accesses of a role.")
    public void updateRoleResourcesAccess() {

        final List<ResourcesAccess> newPermissionList = new ArrayList<>();
        newPermissionList
                .add(resourcesAccessRepository.save(new ResourcesAccess(0L, "new", "new", "new", HttpVerb.PUT)));
        newPermissionList
                .add(resourcesAccessRepository.save(new ResourcesAccess(1L, "neww", "neww", "neww", HttpVerb.DELETE)));

        final List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(status().isOk());
        performPut(apiRolesPermissions, jwt, newPermissionList, expectations, "TODO Error message", rolePublic.getId());
    }

    @Test
    @DirtiesContext
    @Requirement("REGARDS_DSL_ADM_ADM_210")
    @Purpose("Check that the system allows to remove all resources accesses of a role.")
    public void clearRoleResourcesAccess() {
        final List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(status().isOk());
        performDelete(apiRolesPermissions, jwt, expectations, "TODO Error message", rolePublic.getId());
    }

    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_210")
    @Purpose("Check that the system allows to retrieve all users of a role.")
    public void retrieveRoleProjectUserList() {
        final List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(status().isOk());
        performGet(apiRolesUsers, jwt, expectations, "TODO Error message", rolePublic.getId());
    }

    @Override
    protected Logger getLogger() {
        return LOG;
    }

}
