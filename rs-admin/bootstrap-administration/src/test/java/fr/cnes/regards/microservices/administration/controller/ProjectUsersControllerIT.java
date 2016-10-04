/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.microservices.administration.controller;

import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.web.bind.annotation.RequestMethod;

import fr.cnes.regards.microservices.core.security.endpoint.MethodAuthorizationService;
import fr.cnes.regards.microservices.core.test.RegardsIntegrationTest;
import fr.cnes.regards.microservices.core.test.report.annotation.Purpose;
import fr.cnes.regards.microservices.core.test.report.annotation.Requirement;
import fr.cnes.regards.modules.accessRights.dao.IRoleRepository;
import fr.cnes.regards.modules.accessRights.domain.HttpVerb;
import fr.cnes.regards.modules.accessRights.domain.MetaData;
import fr.cnes.regards.modules.accessRights.domain.ProjectUser;
import fr.cnes.regards.modules.accessRights.domain.ResourcesAccess;
import fr.cnes.regards.modules.accessRights.domain.Role;
import fr.cnes.regards.modules.accessRights.service.IAccountService;
import fr.cnes.regards.modules.accessRights.service.IProjectUserService;
import fr.cnes.regards.modules.accessRights.service.IRoleService;
import fr.cnes.regards.security.utils.jwt.JWTService;

/**
 * @author svissier
 *
 */
public class ProjectUsersControllerIT extends RegardsIntegrationTest {

    @Autowired
    private JWTService jwtService_;

    @Autowired
    private MethodAuthorizationService authService_;

    private String jwt_;

    private String apiUsers_;

    private String apiUserId_;

    private String apiUserLogin_;

    private String apiUserPermissions_;

    private String apiUserPermissionsBorrowedRole_;

    private String apiUserMetaData_;

    private String errorMessage_;

    @Autowired
    private IProjectUserService projectUserService_;

    @Autowired
    private IAccountService accountService_;

    @Autowired
    private IRoleRepository roleRepository_;

    @Autowired
    private IRoleService roleService_;

    @Before
    public void init() {
        setLogger(LoggerFactory.getLogger(ProjectUsersControllerIT.class));
        jwt_ = jwtService_.generateToken("PROJECT", "email", "SVG", "USER");
        authService_.setAuthorities("/users", RequestMethod.GET, "USER");
        authService_.setAuthorities("/users", RequestMethod.POST, "USER");
        authService_.setAuthorities("/users/{user_id}", RequestMethod.GET, "USER");
        authService_.setAuthorities("/users/{user_id}", RequestMethod.PUT, "USER");
        authService_.setAuthorities("/users/{user_id}", RequestMethod.DELETE, "USER");
        authService_.setAuthorities("/users/{user_id}/metadata", RequestMethod.GET, "USER");
        authService_.setAuthorities("/users/{user_id}/metadata", RequestMethod.PUT, "USER");
        authService_.setAuthorities("/users/{user_id}/metadata", RequestMethod.DELETE, "USER");
        authService_.setAuthorities("/users/{user_login}/permissions", RequestMethod.GET, "USER");
        authService_.setAuthorities("/users/{user_login}/permissions", RequestMethod.PUT, "USER");
        authService_.setAuthorities("/users/{user_login}/permissions", RequestMethod.DELETE, "USER");
        apiUsers_ = "/users";
        apiUserId_ = apiUsers_ + "/{user_id}";
        apiUserLogin_ = apiUsers_ + "/{user_login}";
        apiUserPermissions_ = apiUserLogin_ + "/permissions";
        apiUserPermissionsBorrowedRole_ = apiUserPermissions_ + "?borrowedRoleName=";
        apiUserMetaData_ = apiUserId_ + "/metadata";
        errorMessage_ = "Cannot reach model attributes";
    }

    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_310")
    @Purpose("Check that the system allows to retrieve all user on a project.")
    public void getAllUsers() {
        List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(status().isOk());
        performGet(apiUsers_, jwt_, expectations, errorMessage_);

    }

    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_310")
    @Purpose("Check that the system allows to retrieve a single user on a project.")
    public void getUser() {
        Long userId = projectUserService_.retrieveUserList().get(0).getId();

        List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(status().isOk());
        performGet(apiUserId_, jwt_, expectations, errorMessage_, userId);

        expectations.clear();
        expectations.add(status().isNotFound());
        performGet(apiUserId_, jwt_, expectations, errorMessage_, Long.MAX_VALUE);

    }

    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_330")
    @Purpose("Check that the system allows to retrieve a user's metadata.")
    public void getUserMetaData() {
        Long userId = projectUserService_.retrieveUserList().get(0).getId();

        List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(status().isOk());
        performGet(apiUserMetaData_, jwt_, expectations, errorMessage_, userId);
    }

    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_230")
    @Purpose("Check that the system allows to retrieve a user's permissions.")
    public void getUserPermissions() {
        String login = accountService_.retrieveAccountList().get(0).getLogin();

        List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(status().isOk());
        performGet(apiUserPermissions_, jwt_, expectations, errorMessage_, login);

        expectations.clear();
        expectations.add(status().isNotFound());
        performGet(apiUserPermissions_, jwt_, expectations, errorMessage_, "wrongLogin");
    }

    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_270")
    @Purpose("Check that the system allows a user to connect using a hierarchically inferior role to its own and handle fail cases.")
    public void getUserPermissionsWithBorrowedRole() {
        ProjectUser projectUser = projectUserService_.retrieveUserList().get(0);
        String projectUserLogin = projectUser.getAccount().getLogin();

        Role projectUserRole = projectUser.getRole();
        List<ResultMatcher> expectations = new ArrayList<>(1);

        // Borrowing a hierarchically inferior role
        String borrowedRoleName = "Registered User";
        assertTrue(roleRepository_.findOneByName(borrowedRoleName) != null);
        Role borrowedRole = roleRepository_.findOneByName(borrowedRoleName);
        assertTrue(roleService_.isHierarchicallyInferior(borrowedRole, projectUserRole));
        expectations.add(status().isOk());
        performGet(apiUserPermissionsBorrowedRole_ + borrowedRoleName, jwt_, expectations, errorMessage_,
                   projectUserLogin);

        // Borrowing a hierarchically superior role
        borrowedRoleName = "Instance Admin";
        assertTrue(roleRepository_.findOneByName(borrowedRoleName) != null);
        borrowedRole = roleRepository_.findOneByName(borrowedRoleName);
        assertTrue(!roleService_.isHierarchicallyInferior(borrowedRole, projectUserRole));
        expectations.clear();
        expectations.add(status().isBadRequest());
        performGet(apiUserPermissionsBorrowedRole_ + borrowedRoleName, jwt_, expectations, errorMessage_,
                   projectUserLogin);
    }

    @Test
    @DirtiesContext
    @Requirement("REGARDS_DSL_ADM_ADM_330")
    @Purpose("Check that the system allows to update a user's metadata.")
    public void updateUserMetaData() {
        Long userId = projectUserService_.retrieveUserList().get(0).getId();
        List<MetaData> newPermissionList = new ArrayList<>();
        newPermissionList.add(new MetaData());
        newPermissionList.add(new MetaData());

        List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(status().isOk());
        performPut(apiUserMetaData_, jwt_, newPermissionList, expectations, errorMessage_, userId);
    }

    @Test
    @DirtiesContext
    @Requirement("REGARDS_DSL_ADM_ADM_230")
    @Purpose("Check that the system allows to update a user's permissions.")
    public void updateUserPermissions() {
        String userLogin = accountService_.retrieveAccountList().get(0).getLogin();

        List<ResourcesAccess> newPermissionList = new ArrayList<>();
        newPermissionList.add(new ResourcesAccess(463L, "new", "new", "new", HttpVerb.PUT));
        newPermissionList.add(new ResourcesAccess(350L, "neww", "neww", "neww", HttpVerb.DELETE));

        List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(status().isOk());
        performPut(apiUserPermissions_, jwt_, newPermissionList, expectations, errorMessage_, userLogin);
    }

    @Test
    @DirtiesContext
    @Requirement("REGARDS_DSL_ADM_ADM_330")
    @Purpose("Check that the system allows to delete a user's metadata.")
    public void deleteUserMetaData() {
        Long userId = projectUserService_.retrieveUserList().get(0).getId();

        List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(status().isOk());
        performDelete(apiUserMetaData_, jwt_, expectations, errorMessage_, userId);
    }

    @Test
    @DirtiesContext
    @Requirement("REGARDS_DSL_ADM_ADM_230")
    @Purpose("Check that the system allows to delete a user's permissions.")
    public void deleteUserPermissions() {
        String userLogin = accountService_.retrieveAccountList().get(0).getLogin();

        List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(status().isOk());
        performDelete(apiUserPermissions_, jwt_, expectations, errorMessage_, userLogin);
    }

    @Test
    @DirtiesContext
    @Requirement("REGARDS_DSL_ADM_ADM_310")
    @Purpose("Check that the system allows to update a project user and handle fail cases.")
    public void updateUser() {
        Long userId = projectUserService_.retrieveUserList().get(0).getId();
        ProjectUser updated = projectUserService_.retrieveUser(userId);
        updated.setLastConnection(LocalDateTime.now());

        // if that's the same functional ID and the parameter is valid:
        List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(status().isOk());
        performPut(apiUserId_, jwt_, updated, expectations, errorMessage_, userId);

        // if that's not the same functional ID and the parameter is valid:
        ProjectUser notSameID = new ProjectUser();

        expectations.clear();
        expectations.add(status().isBadRequest());
        performPut(apiUserId_, jwt_, notSameID, expectations, errorMessage_, userId);
    }

    @Test
    @DirtiesContext
    @Requirement("REGARDS_DSL_ADM_ADM_310")
    @Purpose("Check that the system allows to delete a project user.")
    public void deleteUser() {
        Long userId = projectUserService_.retrieveUserList().get(0).getId();

        List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(status().isOk());
        performDelete(apiUserId_, jwt_, expectations, errorMessage_, userId);
    }
}
