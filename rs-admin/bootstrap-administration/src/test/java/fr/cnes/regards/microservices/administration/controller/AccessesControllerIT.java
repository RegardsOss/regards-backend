/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.microservices.administration.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.web.bind.annotation.RequestMethod;

import fr.cnes.regards.framework.security.autoconfigure.endpoint.DefaultMethodAuthorizationServiceImpl;
import fr.cnes.regards.framework.security.utils.jwt.JWTService;
import fr.cnes.regards.framework.test.integration.AbstractRegardsIntegrationTest;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import fr.cnes.regards.modules.accessRights.domain.AccessRequestDTO;
import fr.cnes.regards.modules.accessRights.domain.projects.Role;
import fr.cnes.regards.modules.accessRights.service.IAccessRequestService;

/**
 * Integration tests for the accesses functionalities.
 *
 * @author xbrochar
 */
public class AccessesControllerIT extends AbstractRegardsIntegrationTest {

    /**
     * The autowired service handling the jwt token.
     */
    @Autowired
    private JWTService jwtService;

    /**
     * The autowired authorization service.
     */
    @Autowired
    private DefaultMethodAuthorizationServiceImpl authService;

    /**
     * The jwt token
     */
    private String jwt;

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
     * The error message TODO: Remove this? We should not shadow the error message thrown by the caght exception.
     */
    private String errorMessage;

    /**
     * The autowired {@link IAccessRequestService} implementation.
     */
    @Autowired
    private IAccessRequestService accessRequestService;

    /**
     * Do some setup before each test
     */
    @Before
    public void init() {
        setLogger(LoggerFactory.getLogger(AccessesControllerIT.class));

        apiAccesses = "/accesses";
        apiAccessId = apiAccesses + "/{access_id}";
        apiAccessAccept = apiAccessId + "/accept";
        apiAccessDeny = apiAccessId + "/deny";
        apiAccessSettings = apiAccessId + "/settings";

        final String roleName = "USER";
        jwt = jwtService.generateToken("PROJECT", "email", "SVG", roleName);

        authService.setAuthorities(apiAccesses, RequestMethod.GET, roleName);
        authService.setAuthorities(apiAccesses, RequestMethod.POST, roleName);
        authService.setAuthorities(apiAccessId, RequestMethod.GET, roleName);
        authService.setAuthorities(apiAccessAccept, RequestMethod.PUT, roleName);
        authService.setAuthorities(apiAccessDeny, RequestMethod.PUT, roleName);
        authService.setAuthorities(apiAccessId, RequestMethod.DELETE, roleName);
        authService.setAuthorities(apiAccessSettings, RequestMethod.GET, roleName);
        authService.setAuthorities(apiAccessSettings, RequestMethod.PUT, roleName);

        errorMessage = "Cannot reach model attributes";
    }

    /**
     * Check that the system allows to retrieve all users for a project.
     *
     * @throws IOException
     */
    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_310")
    @Purpose("Check that the system allows to retrieve all users for a project.")
    public void getAllAccesses() {
        final List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(MockMvcResultMatchers.status().isOk());
        performGet(apiAccesses, jwt, expectations, errorMessage);
    }

    /**
     * Check that the system allows the user to request a registration.
     */
    @Test
    @DirtiesContext
    @Requirement("REGARDS_DSL_ADM_ADM_510")
    @Purpose("Check that the system allows the user to request a registration.")
    public void requestAccess() {
        final AccessRequestDTO newAccessRequest = new AccessRequestDTO();
        newAccessRequest.setEmail("login@test.com");
        newAccessRequest.setFirstName("Firstname");
        newAccessRequest.setLastName("Lastname");
        newAccessRequest.setLogin("login");
        newAccessRequest.setPassword("password");
        newAccessRequest.setRole(new Role());
        newAccessRequest.setPermissions(new ArrayList<>());

        final List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(MockMvcResultMatchers.status().isCreated());
        performPost(apiAccesses, jwt, newAccessRequest, expectations, errorMessage);

        expectations.clear();
        expectations.add(MockMvcResultMatchers.status().isConflict());
        performPost(apiAccesses, jwt, newAccessRequest, expectations, errorMessage);
    }

    /**
     * Check that the system allows to validate a registration request.
     */
    @Test
    @DirtiesContext
    @Requirement("REGARDS_DSL_ADM_ADM_520")
    @Purpose("Check that the system allows to validate a registration request.")
    public void acceptAccessRequest() {
        final Long accessRequestId = accessRequestService.retrieveAccessRequestList().get(0).getId();
        Assert.assertFalse(!accessRequestService.existAccessRequest(accessRequestId));

        final List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(MockMvcResultMatchers.status().isOk());
        performPut(apiAccessAccept, jwt, null, expectations, errorMessage, accessRequestId);

        // once it's accepted it's no longer a request so next time i try to accept it it cannot be found
        expectations.clear();
        expectations.add(MockMvcResultMatchers.status().isNotFound());
        performPut(apiAccessAccept, jwt, null, expectations, errorMessage, accessRequestId);

        // something that does not exist
        expectations.clear();
        expectations.add(MockMvcResultMatchers.status().isNotFound());
        performPut(apiAccessAccept, jwt, null, expectations, errorMessage, Long.MAX_VALUE);
    }

    /**
     * Check that the system allows to deny a registration request.
     */
    @Test
    @DirtiesContext
    @Requirement("REGARDS_DSL_ADM_ADM_520")
    @Purpose("Check that the system allows to deny a registration request.")
    public void denyAccessRequest() {
        final Long accessRequestId = accessRequestService.retrieveAccessRequestList().get(0).getId();

        final List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(MockMvcResultMatchers.status().isOk());
        performPut(apiAccessDeny, jwt, null, expectations, errorMessage, accessRequestId);

        expectations.clear();
        expectations.add(MockMvcResultMatchers.status().isNotFound());
        performPut(apiAccessDeny, jwt, null, expectations, errorMessage, accessRequestId);

        expectations.clear();
        expectations.add(MockMvcResultMatchers.status().isNotFound());
        performPut(apiAccessDeny, jwt, null, expectations, errorMessage, Long.MAX_VALUE);
    }

    /**
     * Check that the system allows to delete a registration request.
     */
    @Test
    @DirtiesContext
    @Requirement("REGARDS_DSL_ADM_ADM_520")
    @Purpose("Check that the system allows to delete a registration request.")
    public void deleteAccessRequest() {
        final List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(MockMvcResultMatchers.status().isNotFound());
        performDelete(apiAccessId, jwt, expectations, errorMessage, Long.MAX_VALUE);

        final Long accessRequestId = accessRequestService.retrieveAccessRequestList().get(0).getId();

        expectations.clear();
        expectations.add(MockMvcResultMatchers.status().isOk());
        performDelete(apiAccessId, jwt, expectations, errorMessage, accessRequestId);

        expectations.clear();
        expectations.add(MockMvcResultMatchers.status().isNotFound());
        performDelete(apiAccessId, jwt, expectations, errorMessage, accessRequestId);

    }
}
