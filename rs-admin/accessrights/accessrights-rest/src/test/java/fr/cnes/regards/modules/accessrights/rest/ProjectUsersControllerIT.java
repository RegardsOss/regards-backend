/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.rest;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.web.bind.annotation.RequestMethod;

import fr.cnes.regards.framework.security.endpoint.MethodAuthorizationService;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.framework.security.utils.jwt.JWTService;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import fr.cnes.regards.modules.accessrights.dao.projects.IProjectUserRepository;
import fr.cnes.regards.modules.accessrights.dao.projects.IRoleRepository;
import fr.cnes.regards.modules.accessrights.domain.HttpVerb;
import fr.cnes.regards.modules.accessrights.domain.UserStatus;
import fr.cnes.regards.modules.accessrights.domain.projects.MetaData;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;
import fr.cnes.regards.modules.accessrights.domain.projects.ResourcesAccess;
import fr.cnes.regards.modules.accessrights.domain.projects.Role;
import fr.cnes.regards.modules.accessrights.service.IProjectUserService;
import fr.cnes.regards.modules.accessrights.service.IRoleService;

/**
 *
 * Class ProjectUsersControllerIT
 *
 * Integration tests for ProjectUsers REST Controller.
 *
 * @author svissier
 * @author sbinda
 * @author xbrochar
 * @since 1.0-SNAPSHOT
 */
@EnableAutoConfiguration(exclude = DataSourceAutoConfiguration.class)
public class ProjectUsersControllerIT extends AbstractAdministrationIT {

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(ProjectUsersControllerIT.class);

    @Autowired
    private JWTService jwtService;

    @Autowired
    private MethodAuthorizationService authService;

    private String jwt;

    private String apiUsers;

    private String apiUserId;

    private String apiUserEmail;

    private String apiUserLogin;

    private String apiUserPermissions;

    private String apiUserPermissionsBorrowedRole;

    private String apiUserMetaData;

    private String errorMessage;

    @Autowired
    private IProjectUserService projectUserService;

    @Autowired
    private IProjectUserRepository projectUserRepository;

    @Autowired
    private IRoleRepository roleRepository;

    @Autowired
    private IRoleService roleService;

    @Override
    public void init() {
        final String tenant = AbstractAdministrationIT.PROJECT_TEST_NAME;
        jwt = jwtService.generateToken(tenant, "email", "SVG", "USER");
        authService.setAuthorities(tenant, "/users", RequestMethod.GET, "USER");
        authService.setAuthorities(tenant, "/users", RequestMethod.POST, "USER");
        authService.setAuthorities(tenant, "/users/{user_email}", RequestMethod.GET, "USER");
        authService.setAuthorities(tenant, "/users/{user_id}", RequestMethod.PUT, "USER");
        authService.setAuthorities(tenant, "/users/{user_id}", RequestMethod.DELETE, "USER");
        authService.setAuthorities(tenant, "/users/{user_id}/metadata", RequestMethod.GET, "USER");
        authService.setAuthorities(tenant, "/users/{user_id}/metadata", RequestMethod.PUT, "USER");
        authService.setAuthorities(tenant, "/users/{user_id}/metadata", RequestMethod.DELETE, "USER");
        authService.setAuthorities(tenant, "/users/{user_login}/permissions", RequestMethod.GET, "USER");
        authService.setAuthorities(tenant, "/users/{user_login}/permissions", RequestMethod.PUT, "USER");
        authService.setAuthorities(tenant, "/users/{user_login}/permissions", RequestMethod.DELETE, "USER");
        apiUsers = "/users";
        apiUserId = apiUsers + "/{user_id}";
        apiUserEmail = apiUsers + "/{user_email}";
        apiUserLogin = apiUsers + "/{user_login}";
        apiUserPermissions = apiUserLogin + "/permissions";
        apiUserPermissionsBorrowedRole = apiUserPermissions + "?borrowedRoleName=";
        apiUserMetaData = apiUserId + "/metadata";
        errorMessage = "Cannot reach model attributes";
    }

    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_310")
    @Purpose("Check that the system allows to retrieve all user on a project.")
    public void getAllUsers() {
        final List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(status().isOk());
        performGet(apiUsers, jwt, expectations, errorMessage);

    }

    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_310")
    @Purpose("Check that the system allows to retrieve a single user on a project.")
    public void getUser() {
        final String userMail = projectUserService.retrieveUserList().get(0).getEmail();

        final List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(status().isOk());
        performGet(apiUserEmail, jwt, expectations, errorMessage, userMail);

        expectations.clear();
        expectations.add(status().isNotFound());
        performGet(apiUserEmail, jwt, expectations, errorMessage, "user@invalid.fr");

    }

    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_330")
    @Purpose("Check that the system allows to retrieve a user's metadata.")
    public void getUserMetaData() {
        final Long userId = projectUserService.retrieveUserList().get(0).getId();

        final List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(status().isOk());
        performGet(apiUserMetaData, jwt, expectations, errorMessage, userId);
    }

    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_230")
    @Purpose("Check that the system allows to retrieve a user's permissions.")
    public void getUserPermissions() {
        final String email = projectUserService.retrieveUserList().get(0).getEmail();

        final List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(status().isOk());
        performGet(apiUserPermissions, jwt, expectations, errorMessage, email);

        expectations.clear();
        expectations.add(status().isNotFound());
        performGet(apiUserPermissions, jwt, expectations, errorMessage, "wrongEmail");
    }

    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_270")
    @Purpose("Check that the system allows a user to connect using a hierarchically inferior role to its own and handle fail cases.")
    public void getUserPermissionsWithBorrowedRole() {
        // Initiate a specific project user for the test
        Assert.assertTrue(roleRepository.findOneByName(DefaultRole.ADMIN.toString()) != null);
        final Role role = roleRepository.findOneByName(DefaultRole.ADMIN.toString());
        final ProjectUser projectUser = new ProjectUser(4824L, null, null, UserStatus.ACCESS_GRANTED, new ArrayList<>(),
                role, new ArrayList<>(), "email@test.com");
        // Save it
        projectUserRepository.save(projectUser);

        // Init the list of test expectations
        final List<ResultMatcher> expectations = new ArrayList<>(1);

        // Borrowing a hierarchically inferior role
        String borrowedRoleName = DefaultRole.REGISTERED_USER.toString();
        Assert.assertTrue(roleRepository.findOneByName(borrowedRoleName) != null);
        Role borrowedRole = roleRepository.findOneByName(borrowedRoleName);
        Assert.assertTrue(roleService.isHierarchicallyInferior(borrowedRole, role));
        expectations.add(status().isOk());
        performGet(apiUserPermissionsBorrowedRole + borrowedRoleName, jwt, expectations, errorMessage,
                   projectUser.getEmail());

        // Borrowing a hierarchically superior role
        borrowedRoleName = DefaultRole.INSTANCE_ADMIN.toString();
        Assert.assertTrue(roleRepository.findOneByName(borrowedRoleName) != null);
        borrowedRole = roleRepository.findOneByName(borrowedRoleName);
        Assert.assertTrue(!roleService.isHierarchicallyInferior(borrowedRole, role));
        expectations.clear();
        expectations.add(status().isBadRequest());
        performGet(apiUserPermissionsBorrowedRole + borrowedRoleName, jwt, expectations, errorMessage,
                   projectUser.getEmail());
    }

    @Test
    @DirtiesContext
    @Requirement("REGARDS_DSL_ADM_ADM_330")
    @Purpose("Check that the system allows to update a user's metadata.")
    public void updateUserMetaData() {
        final Long userId = projectUserService.retrieveUserList().get(0).getId();
        final List<MetaData> newPermissionList = new ArrayList<>();
        newPermissionList.add(new MetaData());
        newPermissionList.add(new MetaData());

        final List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(status().isOk());
        performPut(apiUserMetaData, jwt, newPermissionList, expectations, errorMessage, userId);
    }

    @Test
    @DirtiesContext
    @Requirement("REGARDS_DSL_ADM_ADM_230")
    @Purpose("Check that the system allows to update a user's permissions.")
    public void updateUserPermissions() {
        final String email = projectUserService.retrieveUserList().get(0).getEmail();

        final List<ResourcesAccess> newPermissionList = new ArrayList<>();
        newPermissionList.add(new ResourcesAccess(463L, "new", "new", "new", HttpVerb.PUT));
        newPermissionList.add(new ResourcesAccess(350L, "neww", "neww", "neww", HttpVerb.DELETE));

        final List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(status().isOk());
        performPut(apiUserPermissions, jwt, newPermissionList, expectations, errorMessage, email);
    }

    @Test
    @DirtiesContext
    @Requirement("REGARDS_DSL_ADM_ADM_330")
    @Purpose("Check that the system allows to delete a user's metadata.")
    public void deleteUserMetaData() {
        final Long userId = projectUserService.retrieveUserList().get(0).getId();

        final List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(status().isOk());
        performDelete(apiUserMetaData, jwt, expectations, errorMessage, userId);
    }

    @Test
    @DirtiesContext
    @Requirement("REGARDS_DSL_ADM_ADM_230")
    @Purpose("Check that the system allows to delete a user's permissions.")
    public void deleteUserPermissions() {
        final String email = projectUserService.retrieveUserList().get(0).getEmail();

        final List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(status().isOk());
        performDelete(apiUserPermissions, jwt, expectations, errorMessage, email);
    }

    @Test
    @DirtiesContext
    @Requirement("REGARDS_DSL_ADM_ADM_310")
    @Purpose("Check that the system allows to update a project user and handles fail cases.")
    public void updateUser() {
        final Long userId = projectUserService.retrieveUserList().get(0).getId();
        final ProjectUser updated = projectUserService.retrieveUser(userId);
        updated.setEmail("new@email.com");

        // if that's the same functional ID and the parameter is valid:
        final List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(status().isOk());
        performPut(apiUserId, jwt, updated, expectations, errorMessage, userId);

        // if that's not the same functional ID and the parameter is valid:
        final ProjectUser notSameID = new ProjectUser();

        expectations.clear();
        expectations.add(status().isBadRequest());
        performPut(apiUserId, jwt, notSameID, expectations, errorMessage, userId);
    }

    @Test
    @DirtiesContext
    @Requirement("REGARDS_DSL_ADM_ADM_310")
    @Purpose("Check that the system allows to delete a project user.")
    public void deleteUser() {
        final Long userId = projectUserService.retrieveUserList().get(0).getId();

        final List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(status().isOk());
        performDelete(apiUserId, jwt, expectations, errorMessage, userId);
    }

    @Override
    protected Logger getLogger() {
        return LOG;
    }
}
