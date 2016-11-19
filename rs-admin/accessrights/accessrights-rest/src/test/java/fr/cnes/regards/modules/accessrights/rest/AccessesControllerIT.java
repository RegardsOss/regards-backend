/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.rest;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.web.bind.annotation.RequestMethod;

import fr.cnes.regards.framework.security.endpoint.MethodAuthorizationService;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import fr.cnes.regards.modules.accessrights.dao.projects.IProjectUserRepository;
import fr.cnes.regards.modules.accessrights.domain.AccessRequestDTO;
import fr.cnes.regards.modules.accessrights.domain.UserStatus;
import fr.cnes.regards.modules.accessrights.domain.projects.AccessSettings;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;
import fr.cnes.regards.modules.accessrights.service.projectuser.IProjectUserService;

/**
 *
 * Class AccessesControllerIT
 *
 * Integration tests for the accesses functionalities.
 *
 * @author xbrochar
 * @author Sébastien Binda
 * @since 1.0-SNAPSHOT
 */
@EnableAutoConfiguration(exclude = DataSourceAutoConfiguration.class)
public class AccessesControllerIT extends AbstractAdministrationIT {

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(AccessesControllerIT.class);

    /**
     * An email
     */
    private static final String EMAIL = "email@test.com";

    /**
     * A project user.<br>
     * We ensure before each test to have only this exactly project user in db for convenience.
     */
    private ProjectUser projectUser;

    /**
     * The autowired authorization service.
     */
    @Autowired
    private MethodAuthorizationService authService;

    @Autowired
    private IProjectUserRepository projectUserRepository;

    /**
     * Root access request endpoint
     */
    private String apiAccesses;

    /**
     * Endpoint for a specific access request
     */
    private String apiAccessId;

    /**
     * Endpoint for accepting access request
     */
    private String apiAccessAccept;

    /**
     * Endpoint for denying access request
     */
    private String apiAccessDeny;

    /**
     * Endpoint for access request settings
     */
    private String apiAccessSettings;

    /**
     * The error message TODO: Remove this? We should not shadow the error message thrown by the caught exception.
     */
    private String errorMessage;

    /**
     * The autowired {@link IProjectUserService} implementation.
     */
    @Autowired
    private IProjectUserService projectUserService;

    /**
     * Do some setup before each test
     */
    @Override
    public void init() {

        apiAccesses = "/accesses";
        apiAccessId = apiAccesses + "/{access_id}";
        apiAccessAccept = apiAccessId + "/accept";
        apiAccessDeny = apiAccessId + "/deny";
        apiAccessSettings = apiAccesses + "/settings";

        authService.setAuthorities(PROJECT_TEST_NAME, apiAccesses, RequestMethod.GET, ROLE_TEST);
        authService.setAuthorities(PROJECT_TEST_NAME, apiAccesses, RequestMethod.POST, ROLE_TEST);
        authService.setAuthorities(PROJECT_TEST_NAME, apiAccessId, RequestMethod.GET, ROLE_TEST);
        authService.setAuthorities(PROJECT_TEST_NAME, apiAccessAccept, RequestMethod.PUT, ROLE_TEST);
        authService.setAuthorities(PROJECT_TEST_NAME, apiAccessDeny, RequestMethod.PUT, ROLE_TEST);
        authService.setAuthorities(PROJECT_TEST_NAME, apiAccessId, RequestMethod.DELETE, ROLE_TEST);
        authService.setAuthorities(PROJECT_TEST_NAME, apiAccessSettings, RequestMethod.GET, ROLE_TEST);
        authService.setAuthorities(PROJECT_TEST_NAME, apiAccessSettings, RequestMethod.PUT, ROLE_TEST);

        errorMessage = "Cannot reach model attributes";

        projectUser = projectUserRepository
                .save(new ProjectUser(EMAIL, roleTest, roleTest.getPermissions(), new ArrayList<>()));
    }

    /**
     * Check that the system allows to retrieve all access requests for a project.
     */
    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_310")
    @Purpose("Check that the system allows to retrieve all access requests for a project.")
    public void getAllAccesses() {
        final List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(MockMvcResultMatchers.status().isOk());
        performGet(apiAccesses, token, expectations, errorMessage);
    }

    /**
     * Check that the system allows the user to request a registration.
     */
    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_510")
    @Purpose("Check that the system allows the user to request a registration.")
    public void requestAccess() {
        final AccessRequestDTO newAccessRequest = new AccessRequestDTO();
        newAccessRequest.setEmail("login@test.com");
        newAccessRequest.setFirstName("Firstname");
        newAccessRequest.setLastName("Lastname");
        newAccessRequest.setPassword("password");
        newAccessRequest.setRoleName(DefaultRole.PUBLIC.toString());
        newAccessRequest.setPermissions(new ArrayList<>());

        final List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(MockMvcResultMatchers.status().isCreated());
        performPost(apiAccesses, token, newAccessRequest, expectations, errorMessage);

        expectations.clear();
        expectations.add(MockMvcResultMatchers.status().isConflict());
        performPost(apiAccesses, token, newAccessRequest, expectations, errorMessage);
    }

    /**
     * Check that the system allows to reactivate access to an access denied project user.
     */
    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_520")
    @Purpose("Check that the system allows to reactivate access to an access denied project user.")
    public void acceptAccessRequest() {
        // Prepare the test conditions
        projectUser.setStatus(UserStatus.ACCESS_DENIED);
        projectUserRepository.save(projectUser);

        final List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(MockMvcResultMatchers.status().isOk());
        performPut(apiAccessAccept, token, null, expectations, errorMessage, projectUser.getId());

        // Now the project user is ACCESS_GRANTED, so trying to re-accept will fail
        expectations.clear();
        expectations.add(MockMvcResultMatchers.status().isForbidden());
        performPut(apiAccessAccept, token, null, expectations, errorMessage, projectUser.getId());

        // something that does not exist
        expectations.clear();
        expectations.add(MockMvcResultMatchers.status().isNotFound());
        performPut(apiAccessAccept, token, null, expectations, errorMessage, Long.MAX_VALUE);
    }

    /**
     * Check that the system allows to deny access to an already access granted project user.
     */
    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_520")
    @Purpose("Check that the system allows to deny access to an already access granted project user.")
    public void denyAccessRequest() {
        // Prepare the test conditions
        projectUser.setStatus(UserStatus.ACCESS_GRANTED);
        projectUserRepository.save(projectUser);

        final List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(MockMvcResultMatchers.status().isOk());
        performPut(apiAccessDeny, token, null, expectations, errorMessage, projectUser.getId());

        // Now the project user is ACCESS_DENIED, so trying to re-deny it will fail
        expectations.clear();
        expectations.add(MockMvcResultMatchers.status().isForbidden());
        performPut(apiAccessDeny, token, null, expectations, errorMessage, projectUser.getId());

        expectations.clear();
        expectations.add(MockMvcResultMatchers.status().isNotFound());
        performPut(apiAccessDeny, token, null, expectations, errorMessage, Long.MAX_VALUE);
    }

    /**
     * Check that the system allows to delete a registration request.
     */
    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_520")
    @Purpose("Check that the system allows to delete a registration request.")
    public void deleteAccessRequest() {
        // Case not found
        final List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(MockMvcResultMatchers.status().isNotFound());
        performDelete(apiAccessId, token, expectations, errorMessage, Long.MAX_VALUE);

        expectations.clear();
        expectations.add(MockMvcResultMatchers.status().isOk());
        performDelete(apiAccessId, token, expectations, errorMessage, projectUser.getId());

        expectations.clear();
        expectations.add(MockMvcResultMatchers.status().isNotFound());
        performDelete(apiAccessId, token, expectations, errorMessage, projectUser.getId());
    }

    /**
     * Check that the system allows to retrieve the access settings.
     */
    @Test
    @Requirement("?")
    @Purpose("Check that the system allows to retrieve the access settings.")
    public void getAccessSettings() {
        final List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(MockMvcResultMatchers.status().isOk());
        performGet(apiAccessSettings, token, expectations, errorMessage);
    }

    /**
     * Check that the system fails when trying to update a non existing access settings.
     */
    @Test
    @Requirement("?")
    @Purpose("Check that the system fails when trying to update a non existing access settings.")
    public void updateAccessSettingsEntityNotFound() {
        final AccessSettings settings = new AccessSettings();
        settings.setId(999L);

        final List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(MockMvcResultMatchers.status().isNotFound());
        performPut(apiAccessSettings, token, settings, expectations, "TODO Error message");
    }

    /**
     * Check that the system allows to update access settings in regular case.
     */
    @Test
    @Requirement("?")
    @Purpose("Check that the system allows to update access settings in regular case.")
    public void updateAccessSettings() {
        final AccessSettings settings = new AccessSettings();
        settings.setId(0L);

        final List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(MockMvcResultMatchers.status().isOk());
        performPut(apiAccessSettings, token, settings, expectations, "TODO Error message");
    }

    @Override
    protected Logger getLogger() {
        return LOG;
    }
}
