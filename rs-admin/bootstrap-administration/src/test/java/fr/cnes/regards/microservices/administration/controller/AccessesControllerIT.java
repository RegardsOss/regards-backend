/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.microservices.administration.controller;

import static org.junit.Assert.assertFalse;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
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
import fr.cnes.regards.modules.accessRights.domain.Account;
import fr.cnes.regards.modules.accessRights.domain.ProjectUser;
import fr.cnes.regards.modules.accessRights.service.IAccessRequestService;
import fr.cnes.regards.security.utils.jwt.JWTService;

public class AccessesControllerIT extends RegardsIntegrationTest {

    @Autowired
    private JWTService jwtService_;

    @Autowired
    private MethodAuthorizationService authService_;

    private String jwt_;

    private String apiAccesses;

    private String apiAccessId;

    private String apiAccessAccept;

    private String apiAccessDeny;

    private String errorMessage;

    @Autowired
    private IAccessRequestService accessRequestService_;

    @Before
    public void init() {
        setLogger(LoggerFactory.getLogger(AccessesControllerIT.class));
        jwt_ = jwtService_.generateToken("PROJECT", "email", "SVG", "USER");
        authService_.setAuthorities("/accesses", RequestMethod.GET, "USER");
        authService_.setAuthorities("/accesses", RequestMethod.POST, "USER");
        authService_.setAuthorities("/accesses/{access_id}", RequestMethod.GET, "USER");
        authService_.setAuthorities("/accesses/{access_id}/accept", RequestMethod.PUT, "USER");
        authService_.setAuthorities("/accesses/{access_id}/deny", RequestMethod.PUT, "USER");
        authService_.setAuthorities("/accesses/{access_id}", RequestMethod.DELETE, "USER");
        authService_.setAuthorities("/accesses/settings", RequestMethod.GET, "USER");
        authService_.setAuthorities("/accesses/settings", RequestMethod.PUT, "USER");
        errorMessage = "Cannot reach model attributes";
        apiAccesses = "/accesses";
        apiAccessId = apiAccesses + "/{access_id}";
        apiAccessAccept = apiAccessId + "/accept";
        apiAccessDeny = apiAccessId + "/deny";
    }

    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_310")
    @Purpose("Check that the system allows to retrieve all users for a project.")
    public void getAllAccesses() throws IOException {
        List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(status().isOk());
        performGet(apiAccesses, jwt_, expectations, errorMessage);
    }

    @Test
    @DirtiesContext
    @Requirement("REGARDS_DSL_ADM_ADM_510")
    @Purpose("Check that the system allows the user to request a registration.")
    public void requestAccess() {
        Account newAccountRequesting;
        ProjectUser newAccessRequest;
        // to be accepted
        newAccountRequesting = new Account("fd5f4e5f84@new.new", "firstName", "lastName", "password");
        newAccessRequest = new ProjectUser(newAccountRequesting);

        List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(status().isCreated());
        performPost(apiAccesses, jwt_, newAccessRequest, expectations, errorMessage);

        expectations.clear();
        expectations.add(status().isConflict());
        performPost(apiAccesses, jwt_, newAccessRequest, expectations, errorMessage);

    }

    @Test
    @DirtiesContext
    @Requirement("REGARDS_DSL_ADM_ADM_520")
    @Purpose("Check that the system allows to validate a registration request.")
    public void acceptAccessRequest() {
        Long accessRequestId = accessRequestService_.retrieveAccessRequestList().get(0).getId();
        assertFalse(!accessRequestService_.existAccessRequest(accessRequestId));

        List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(status().isOk());
        performPut(apiAccessAccept, jwt_, null, expectations, errorMessage, accessRequestId);

        // once it's accepted it's no longer a request so next time i try to accept it it cannot be found
        expectations.clear();
        expectations.add(status().isNotFound());
        performPut(apiAccessAccept, jwt_, null, expectations, errorMessage, accessRequestId);

        // something that does not exist
        expectations.clear();
        expectations.add(status().isNotFound());
        performPut(apiAccessAccept, jwt_, null, expectations, errorMessage, Long.MAX_VALUE);
    }

    @Test
    @DirtiesContext
    @Requirement("REGARDS_DSL_ADM_ADM_520")
    @Purpose("Check that the system allows to deny a registration request.")
    public void denyAccessRequest() {
        Long accessRequestId = accessRequestService_.retrieveAccessRequestList().get(0).getId();

        List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(status().isOk());
        performPut(apiAccessDeny, jwt_, null, expectations, errorMessage, accessRequestId);

        expectations.clear();
        expectations.add(status().isNotFound());
        performPut(apiAccessDeny, jwt_, null, expectations, errorMessage, accessRequestId);

        expectations.clear();
        expectations.add(status().isNotFound());
        performPut(apiAccessDeny, jwt_, null, expectations, errorMessage, Long.MAX_VALUE);
    }

    @Test
    @DirtiesContext
    @Requirement("REGARDS_DSL_ADM_ADM_520")
    @Purpose("Check that the system allows to delete a registration request.")
    public void deleteAccessRequest() {
        List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(status().isNotFound());
        performDelete(apiAccessId, jwt_, expectations, errorMessage, Long.MAX_VALUE);

        Long accessRequestId = accessRequestService_.retrieveAccessRequestList().get(0).getId();

        expectations.clear();
        expectations.add(status().isOk());
        performDelete(apiAccessId, jwt_, expectations, errorMessage, accessRequestId);

        expectations.clear();
        expectations.add(status().isNotFound());
        performDelete(apiAccessId, jwt_, expectations, errorMessage, accessRequestId);

    }
}
