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

import fr.cnes.regards.microservices.core.security.jwt.JWTService;
import fr.cnes.regards.microservices.core.test.RegardsIntegrationTest;
import fr.cnes.regards.modules.accessRights.domain.Account;
import fr.cnes.regards.modules.accessRights.domain.ProjectUser;
import fr.cnes.regards.modules.accessRights.service.IAccessRequestService;
import fr.cnes.regards.modules.accessRights.service.IAccountService;

public class AccessesControllerIT extends RegardsIntegrationTest {

    @Autowired
    private JWTService jwtService_;

    private String jwt_;

    private String apiAccesses;

    private String apiAccessId;

    private String apiAccessAccept;

    private String apiAccessDeny;

    private String errorMessage;

    @Autowired
    private IAccessRequestService accessRequestService_;

    @Autowired
    private IAccountService accountService_;

    @Before
    public void init() {
        setLogger(LoggerFactory.getLogger(AccessesControllerIT.class));
        jwt_ = jwtService_.generateToken("PROJECT", "email", "SVG", "USER");
        errorMessage = "Cannot reach model attributes";
        apiAccesses = "/accesses";
        apiAccessId = apiAccesses + "/{access_id}";
        apiAccessAccept = apiAccessId + "/accept";
        apiAccessDeny = apiAccessId + "/deny";
    }

    @Test
    public void getAllAccesses() throws IOException {

        List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(status().isOk());
        performGet(apiAccesses, jwt_, expectations, errorMessage);
    }

    @Test
    @DirtiesContext
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
