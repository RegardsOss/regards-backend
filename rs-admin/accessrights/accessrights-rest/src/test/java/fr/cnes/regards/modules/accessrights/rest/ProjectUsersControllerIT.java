/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.rest;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.web.bind.annotation.RequestMethod;

import fr.cnes.regards.framework.module.rest.exception.ModuleEntityNotFoundException;
import fr.cnes.regards.framework.security.endpoint.MethodAuthorizationService;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import fr.cnes.regards.modules.accessrights.dao.projects.IMetaDataRepository;
import fr.cnes.regards.modules.accessrights.dao.projects.IProjectUserRepository;
import fr.cnes.regards.modules.accessrights.dao.projects.IResourcesAccessRepository;
import fr.cnes.regards.modules.accessrights.domain.HttpVerb;
import fr.cnes.regards.modules.accessrights.domain.projects.MetaData;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;
import fr.cnes.regards.modules.accessrights.domain.projects.ResourcesAccess;
import fr.cnes.regards.modules.accessrights.domain.projects.Role;
import fr.cnes.regards.modules.accessrights.service.role.RoleService;

/**
 *
 * Class ProjectUsersControllerIT
 *
 * Integration tests for ProjectUsers REST Controller.
 *
 * @author svissier
 * @author Sébastien Binda
 * @author Xavier-Alexandre Brochard
 * @since 1.0-SNAPSHOT
 */
public class ProjectUsersControllerIT extends AbstractAdministrationIT {

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(ProjectUsersControllerIT.class);

    @Autowired
    private MethodAuthorizationService authService;

    /**
     * An email
     */
    private static final String EMAIL = "email@test.com";

    /**
     * The users endpoint
     */
    private String apiUsers;

    /**
     * Specific user endpoint
     */
    private String apiUserId;

    private String apiUserEmail;

    private String apiUserLogin;

    private String apiUserPermissions;

    private String apiUserPermissionsBorrowedRole;

    private String apiUserMetaData;

    private String errorMessage;

    @Autowired
    private IProjectUserRepository projectUserRepository;

    @Autowired
    private RoleService roleService;

    @Autowired
    private IResourcesAccessRepository resourcesAccessRepository;

    @Autowired
    private IMetaDataRepository metaDataRepository;

    /**
     * A project user.<br>
     * We ensure before each test to have only this exactly project user in db for convenience.
     */
    private ProjectUser projectUser;

    @Override
    public void init() {
        authService.setAuthorities(PROJECT_TEST_NAME, "/users", RequestMethod.GET, ROLE_TEST);
        authService.setAuthorities(PROJECT_TEST_NAME, "/users", RequestMethod.POST, ROLE_TEST);
        authService.setAuthorities(PROJECT_TEST_NAME, "/users/{user_email}", RequestMethod.GET, ROLE_TEST);
        authService.setAuthorities(PROJECT_TEST_NAME, "/users/{user_id}", RequestMethod.PUT, ROLE_TEST);
        authService.setAuthorities(PROJECT_TEST_NAME, "/users/{user_id}", RequestMethod.DELETE, ROLE_TEST);
        authService.setAuthorities(PROJECT_TEST_NAME, "/users/{user_id}/metadata", RequestMethod.GET, ROLE_TEST);
        authService.setAuthorities(PROJECT_TEST_NAME, "/users/{user_id}/metadata", RequestMethod.PUT, ROLE_TEST);
        authService.setAuthorities(PROJECT_TEST_NAME, "/users/{user_id}/metadata", RequestMethod.DELETE, ROLE_TEST);
        authService.setAuthorities(PROJECT_TEST_NAME, "/users/{user_login}/permissions", RequestMethod.GET, ROLE_TEST);
        authService.setAuthorities(PROJECT_TEST_NAME, "/users/{user_login}/permissions", RequestMethod.PUT, ROLE_TEST);
        authService.setAuthorities(PROJECT_TEST_NAME, "/users/{user_login}/permissions", RequestMethod.DELETE,
                                   ROLE_TEST);
        apiUsers = "/users";
        apiUserId = apiUsers + "/{user_id}";
        apiUserEmail = apiUsers + "/{user_email}";
        apiUserLogin = apiUsers + "/{user_login}";
        apiUserPermissions = apiUserLogin + "/permissions";
        apiUserPermissionsBorrowedRole = apiUserPermissions + "?borrowedRoleName=";
        apiUserMetaData = apiUserId + "/metadata";
        errorMessage = "Cannot reach model attributes";

        projectUser = projectUserRepository
                .save(new ProjectUser(EMAIL, roleTest, roleTest.getPermissions(), new ArrayList<>()));
    }

    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_310")
    @Purpose("Check that the system allows to retrieve all user on a project.")
    public void getAllUsers() {
        final List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(MockMvcResultMatchers.status().isOk());
        performGet(apiUsers, token, expectations, errorMessage);
    }

    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_310")
    @Purpose("Check that the system allows to retrieve a single user on a project.")
    public void getUser() throws UnsupportedEncodingException {
        final List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(MockMvcResultMatchers.status().isOk());
        performGet(apiUserEmail, token, expectations, errorMessage, EMAIL);

        expectations.clear();
        expectations.add(MockMvcResultMatchers.status().isNotFound());
        performGet(apiUserEmail, token, expectations, errorMessage, "user@invalid.fr");
    }

    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_330")
    @Purpose("Check that the system allows to retrieve a user's metadata.")
    public void getUserMetaData() {
        final List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(MockMvcResultMatchers.status().isOk());
        performGet(apiUserMetaData, token, expectations, errorMessage, projectUser.getId());
    }

    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_230")
    @Purpose("Check that the system allows to retrieve a user's permissions.")
    public void getUserPermissions() {
        final List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(MockMvcResultMatchers.status().isOk());
        performGet(apiUserPermissions, token, expectations, errorMessage, projectUser.getEmail());

        expectations.clear();
        expectations.add(MockMvcResultMatchers.status().isNotFound());
        performGet(apiUserPermissions, token, expectations, errorMessage, "wrongEmail");
    }

    /**
     * Check that the system allows a user to connect using a hierarchically inferior role.
     *
     * @throws ModuleEntityNotFoundException
     */
    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_270")
    @Purpose("Check that the system allows a user to connect using a hierarchically inferior role.")
    public void getUserPermissions_withBorrowedRoleInferior() throws ModuleEntityNotFoundException {
        // Prepare a project user with role admin
        final Role roleAdmin = roleService.retrieveRole(DefaultRole.ADMIN.toString());
        projectUser.setRole(roleAdmin);
        projectUserRepository.save(projectUser);

        // Get the borrowed role
        final String borrowedRoleName = DefaultRole.REGISTERED_USER.toString();
        final Role borrowedRole = roleService.retrieveRole(borrowedRoleName);

        // Borrowing a hierarchically inferior role
        final List<ResultMatcher> expectations = new ArrayList<>(1);
        Assert.assertTrue(roleService.isHierarchicallyInferior(borrowedRole, roleAdmin));
        expectations.add(MockMvcResultMatchers.status().isOk());
        performGet(apiUserPermissionsBorrowedRole + borrowedRoleName, token, expectations, errorMessage,
                   projectUser.getEmail());
    }

    /**
     * Check that the system prevents a user to connect using a hierarchically superior role.
     *
     * @throws EntityNotFoundException
     */
    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_270")
    @Purpose("Check that the system prevents a user to connect using a hierarchically superior role.")
    public void getUserPermissions_withBorrowedRoleSuperior() throws ModuleEntityNotFoundException {
        // Prepare a project user with role admin
        final Role roleAdmin = roleService.retrieveRole(DefaultRole.ADMIN.toString());
        projectUser.setRole(roleAdmin);
        projectUserRepository.save(projectUser);

        // Get the borrowed role
        final String borrowedRoleName = DefaultRole.INSTANCE_ADMIN.toString();
        final Role borrowedRole = roleService.retrieveRole(borrowedRoleName);

        // Borrowing a hierarchically superior role
        final List<ResultMatcher> expectations = new ArrayList<>(1);
        Assert.assertTrue(!roleService.isHierarchicallyInferior(borrowedRole, roleAdmin));
        expectations.clear();
        expectations.add(MockMvcResultMatchers.status().isBadRequest());
        performGet(apiUserPermissionsBorrowedRole + borrowedRoleName, token, expectations, errorMessage,
                   projectUser.getEmail());
    }

    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_330")
    @Purpose("Check that the system allows to update a user's metadata.")
    public void updateUserMetaData() {
        final List<MetaData> newPermissionList = new ArrayList<>();
        newPermissionList.add(metaDataRepository.save(new MetaData()));
        newPermissionList.add(metaDataRepository.save(new MetaData()));

        final List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(MockMvcResultMatchers.status().isOk());
        performPut(apiUserMetaData, token, newPermissionList, expectations, errorMessage, projectUser.getId());
    }

    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_230")
    @Purpose("Check that the system allows to update a user's permissions.")
    public void updateUserPermissions() {
        final List<ResourcesAccess> newPermissionList = new ArrayList<>();
        newPermissionList
                .add(resourcesAccessRepository.save(new ResourcesAccess("desc0", "ms0", "res0", HttpVerb.GET)));
        newPermissionList
                .add(resourcesAccessRepository.save(new ResourcesAccess("desc1", "ms1", "res1", HttpVerb.DELETE)));

        final List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(MockMvcResultMatchers.status().isOk());
        performPut(apiUserPermissions, token, newPermissionList, expectations, errorMessage, EMAIL);
    }

    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_330")
    @Purpose("Check that the system allows to delete a user's metadata.")
    public void deleteUserMetaData() {
        final List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(MockMvcResultMatchers.status().isOk());
        performDelete(apiUserMetaData, token, expectations, errorMessage, projectUser.getId());
    }

    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_230")
    @Purpose("Check that the system allows to delete a user's permissions.")
    public void deleteUserPermissions() {
        final List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(MockMvcResultMatchers.status().isOk());
        performDelete(apiUserPermissions, token, expectations, errorMessage, projectUser.getEmail());
    }

    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_310")
    @Purpose("Check that the system allows to update a project user and handles fail cases.")
    public void updateUser() {
        projectUser.setEmail("new@email.com");

        // Same id
        final List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(MockMvcResultMatchers.status().isOk());
        performPut(apiUserId, token, projectUser, expectations, errorMessage, projectUser.getId());

        // Wrong id (99L)
        expectations.clear();
        expectations.add(MockMvcResultMatchers.status().isBadRequest());
        performPut(apiUserId, token, projectUser, expectations, errorMessage, 99L);
    }

    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_310")
    @Purpose("Check that the system allows to delete a project user.")
    public void deleteUser() {
        final List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(MockMvcResultMatchers.status().isOk());
        performDelete(apiUserId, token, expectations, errorMessage, projectUser.getId());
    }

    @Override
    protected Logger getLogger() {
        return LOG;
    }
}
