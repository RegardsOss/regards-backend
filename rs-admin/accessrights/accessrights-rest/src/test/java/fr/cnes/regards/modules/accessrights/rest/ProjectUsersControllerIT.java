/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.rest;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.web.bind.annotation.RequestMethod;

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.framework.test.integration.AbstractRegardsTransactionalIT;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import fr.cnes.regards.modules.accessrights.dao.projects.IMetaDataRepository;
import fr.cnes.regards.modules.accessrights.dao.projects.IProjectUserRepository;
import fr.cnes.regards.modules.accessrights.dao.projects.IResourcesAccessRepository;
import fr.cnes.regards.modules.accessrights.dao.projects.IRoleRepository;
import fr.cnes.regards.modules.accessrights.domain.projects.MetaData;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;
import fr.cnes.regards.modules.accessrights.domain.projects.ResourcesAccess;
import fr.cnes.regards.modules.accessrights.domain.projects.Role;
import fr.cnes.regards.modules.accessrights.service.role.RoleService;

/**
 * Integration tests for ProjectUsers REST Controller.
 *
 * @author svissier
 * @author Sébastien Binda
 * @author Xavier-Alexandre Brochard
 * @since 1.0-SNAPSHOT
 */
@MultitenantTransactional
public class ProjectUsersControllerIT extends AbstractRegardsTransactionalIT {

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(ProjectUsersControllerIT.class);

    /**
     * An email
     */
    private static final String EMAIL = "email@test.com";

    /**
     * Specific user endpoint
     */
    private String apiUserId;

    private String apiUserEmail;

    private String apiUserPermissions;

    private String apiUserPermissionsBorrowedRole;

    private String errorMessage;

    @Autowired
    private IProjectUserRepository projectUserRepository;

    @Autowired
    private IRoleRepository roleRepository;

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

    private Role publicRole;

    @Before
    public void setUp() {
        apiUserId = ProjectUsersController.REQUEST_MAPPING_ROOT + "/{user_id}";
        apiUserEmail = ProjectUsersController.REQUEST_MAPPING_ROOT + "/email/{user_email}";

        apiUserPermissions = ResourceController.REQUEST_MAPPING_ROOT + apiUserId;
        apiUserPermissionsBorrowedRole = apiUserPermissions + "?borrowedRoleName=";

        errorMessage = "Cannot reach model attributes";

        publicRole = roleRepository.findOneByName(DefaultRole.PUBLIC.toString()).get();

        projectUser = projectUserRepository
                .save(new ProjectUser(EMAIL, publicRole, new ArrayList<>(), new ArrayList<>()));
    }

    @Override
    protected String getDefaultRole() {
        return DefaultRole.PROJECT_ADMIN.toString();
    }

    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_310")
    @Purpose("Check that the system allows to retrieve all user on a project.")
    public void getAllUsers() {
        final List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(MockMvcResultMatchers.status().isOk());
        performDefaultGet(ProjectUsersController.REQUEST_MAPPING_ROOT, expectations, errorMessage);
    }

    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_310")
    @Purpose("Check that the system allows to retrieve a single user on a project.")
    public void getUser() throws UnsupportedEncodingException {
        final List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(MockMvcResultMatchers.status().isOk());
        performDefaultGet(apiUserEmail, expectations, errorMessage, EMAIL);

        expectations.clear();
        expectations.add(MockMvcResultMatchers.status().isNotFound());
        performDefaultGet(apiUserEmail, expectations, errorMessage, "user@invalid.fr");
    }

    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_330")
    @Purpose("Check that the system allows to retrieve a user's metadata.")
    public void getUserMetaData() {
        final List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(MockMvcResultMatchers.status().isOk());
        performDefaultGet(ProjectUserMetadataController.REQUEST_MAPPING_ROOT, expectations, errorMessage,
                          projectUser.getId());
    }

    /**
     * Check that the system allows a user to connect using a hierarchically inferior role.
     *
     * @throws EntityNotFoundException
     *             not found
     */
    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_270")
    @Purpose("Check that the system allows a user to connect using a hierarchically inferior role.")
    public void getUserPermissions_withBorrowedRoleInferior() throws EntityNotFoundException {
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
        performDefaultGet(apiUserPermissionsBorrowedRole + borrowedRoleName, expectations, errorMessage,
                          projectUser.getEmail());
    }

    /**
     * Check that the system prevents a user to connect using a hierarchically superior role.
     *
     * @throws EntityNotFoundException
     *             not found
     */
    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_270")
    @Purpose("Check that the system prevents a user to connect using a hierarchically superior role.")
    public void getUserPermissions_withBorrowedRoleSuperior() throws EntityNotFoundException {
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
        expectations.add(MockMvcResultMatchers.status().isForbidden());
        performDefaultGet(apiUserPermissionsBorrowedRole + borrowedRoleName, expectations, errorMessage,
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
        performDefaultPut(ProjectUserMetadataController.REQUEST_MAPPING_ROOT, newPermissionList, expectations,
                          errorMessage, projectUser.getId());
    }

    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_230")
    @Purpose("Check that the system allows to update a user's permissions.")
    public void updateUserPermissions() {
        final List<ResourcesAccess> newPermissionList = new ArrayList<>();
        newPermissionList.add(resourcesAccessRepository
                .save(new ResourcesAccess("desc0", "ms0", "res0", "Controller", RequestMethod.GET, DefaultRole.ADMIN)));
        newPermissionList.add(resourcesAccessRepository.save(new ResourcesAccess("desc1", "ms1", "res1", "Controller",
                RequestMethod.DELETE, DefaultRole.ADMIN)));

        final List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(MockMvcResultMatchers.status().isOk());
        performDefaultPut(apiUserPermissions, newPermissionList, expectations, errorMessage, EMAIL);
    }

    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_330")
    @Purpose("Check that the system allows to delete a user's metadata.")
    public void deleteUserMetaData() {
        final List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(MockMvcResultMatchers.status().isOk());
        performDefaultDelete(ProjectUserMetadataController.REQUEST_MAPPING_ROOT, expectations, errorMessage,
                             projectUser.getId());
    }

    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_230")
    @Purpose("Check that the system allows to delete a user's permissions.")
    public void deleteUserPermissions() {
        final List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(MockMvcResultMatchers.status().isOk());
        performDefaultDelete(apiUserPermissions, expectations, errorMessage, projectUser.getEmail());
    }

    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_310")
    @Purpose("Check that the system allows to update a project user and handles fail cases.")
    public void updateUser() {
        projectUser.setEmail("new@email.com");

        // Same id
        final List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(MockMvcResultMatchers.status().isOk());
        performDefaultPut(apiUserId, projectUser, expectations, errorMessage, projectUser.getId());

        // Wrong id (99L)
        expectations.clear();
        expectations.add(MockMvcResultMatchers.status().isBadRequest());
        performDefaultPut(apiUserId, projectUser, expectations, errorMessage, 99L);
    }

    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_310")
    @Purpose("Check that the system allows to delete a project user.")
    public void deleteUser() {
        final List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(MockMvcResultMatchers.status().isOk());
        performDefaultDelete(apiUserId, expectations, errorMessage, projectUser.getId());
    }

    @Override
    protected Logger getLogger() {
        return LOG;
    }
}
