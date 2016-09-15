/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.microservices.modules.test.accesses.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.io.IOException;
import java.util.List;

import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import fr.cnes.regards.microservices.modules.test.RegardsIntegrationTest;
import fr.cnes.regards.modules.accessRights.domain.Account;
import fr.cnes.regards.modules.accessRights.domain.ProjectUser;
import fr.cnes.regards.modules.accessRights.service.AccessRequestServiceStub;
import fr.cnes.regards.modules.accessRights.service.AccountServiceStub;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class AccessesControllerIT extends RegardsIntegrationTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(AccessesControllerIT.class);

    private TestRestTemplate restTemplate;

    private String apiAccesses;

    private String apiAccessId;

    @Autowired
    private AccessRequestServiceStub serviceStub;

    @Autowired
    private AccountServiceStub accountService;

    @Value("${root.admin.login:admin}")
    private String rootAdminLogin;

    @Value("${root.admin.password:admin}")
    private String rootAdminPassword;

    @Before
    public void init() {
        if (restTemplate == null) {
            restTemplate = buildOauth2RestTemplate("acme", "acmesecret", "admin", "admin", "");
        }
        this.apiAccesses = getApiEndpoint().concat("/accesses");
        this.apiAccessId = this.apiAccesses + "/{access_id}";
    }

    @Test
    public void aGetAllAccesses() throws IOException {

        // we have to use exchange instead of getForEntity as long as we use List otherwise the response body is not
        // well casted.
        ParameterizedTypeReference<List<ProjectUser>> typeRef = new ParameterizedTypeReference<List<ProjectUser>>() {
        };
        ResponseEntity<List<ProjectUser>> response = restTemplate.exchange(this.apiAccesses, HttpMethod.GET, null,
                                                                           typeRef);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    public void bRequestAccess() {
        Account newAccountRequesting;
        ProjectUser newAccessRequest;
        // to be accepted
        newAccountRequesting = this.accountService.createAccount("email");
        newAccessRequest = new ProjectUser(newAccountRequesting);

        ResponseEntity<ProjectUser> response = restTemplate.postForEntity(this.apiAccesses, newAccessRequest,
                                                                          ProjectUser.class);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());

        ResponseEntity<ProjectUser> responseConflict = restTemplate.postForEntity(this.apiAccesses, newAccessRequest,
                                                                                  ProjectUser.class);
        assertEquals(HttpStatus.CONFLICT, responseConflict.getStatusCode());

        // to be denied
        newAccountRequesting = this.accountService.createAccount("email2");
        newAccessRequest = new ProjectUser(newAccountRequesting);

        response = restTemplate.postForEntity(this.apiAccesses, newAccessRequest, ProjectUser.class);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());

        responseConflict = restTemplate.postForEntity(this.apiAccesses, newAccessRequest, ProjectUser.class);
        assertEquals(HttpStatus.CONFLICT, responseConflict.getStatusCode());

        // to be removed
        newAccountRequesting = this.accountService.createAccount("email3");
        newAccessRequest = new ProjectUser(newAccountRequesting);

        response = restTemplate.postForEntity(this.apiAccesses, newAccessRequest, ProjectUser.class);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());

        responseConflict = restTemplate.postForEntity(this.apiAccesses, newAccessRequest, ProjectUser.class);
        assertEquals(HttpStatus.CONFLICT, responseConflict.getStatusCode());
    }

    @Test
    public void dAcceptAccessRequest() {
        int accessRequestId = this.serviceStub.retrieveAccessRequestList().get(0).getProjectUserId();
        assertFalse(!this.serviceStub.existAccessRequest(accessRequestId));
        ParameterizedTypeReference<Void> typeRef = new ParameterizedTypeReference<Void>() {
        };
        ResponseEntity<Void> response = restTemplate.exchange(this.apiAccessId + "/accept", HttpMethod.PUT, null,
                                                              typeRef, accessRequestId);
        assertEquals(HttpStatus.OK, response.getStatusCode());

        // once it's accepted it's no longer a request so next time i try to accept it it cannot be found
        response = restTemplate.exchange(this.apiAccessId + "/accept", HttpMethod.PUT, null, typeRef, accessRequestId);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());

        // something that does not exist
        response = restTemplate.exchange(this.apiAccessId + "/accept", HttpMethod.PUT, null, typeRef,
                                         Integer.MAX_VALUE);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());

    }

    @Test
    public void dDenyAccessRequest() {
        int accessRequestId = this.serviceStub.retrieveAccessRequestList().get(0).getProjectUserId();
        assertFalse(!this.serviceStub.existAccessRequest(accessRequestId));
        ParameterizedTypeReference<Void> typeRef = new ParameterizedTypeReference<Void>() {
        };
        ResponseEntity<Void> response = restTemplate.exchange(this.apiAccessId + "/deny", HttpMethod.PUT, null, typeRef,
                                                              accessRequestId);
        assertEquals(HttpStatus.OK, response.getStatusCode());

        response = restTemplate.exchange(this.apiAccessId + "/deny", HttpMethod.PUT, null, typeRef, accessRequestId);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());

        response = restTemplate.exchange(this.apiAccessId + "/deny", HttpMethod.PUT, null, typeRef, Integer.MAX_VALUE);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());

    }

    @Test
    public void eDeleteAccessRequest() {
        ParameterizedTypeReference<Void> typeRef = new ParameterizedTypeReference<Void>() {
        };
        // does not exist so NOT_FOUND
        ResponseEntity<Void> response = restTemplate.exchange(this.apiAccessId, HttpMethod.DELETE, null, typeRef,
                                                              Integer.MAX_VALUE);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());

        int accessRequestId = this.serviceStub.retrieveAccessRequestList().get(0).getProjectUserId();
        response = restTemplate.exchange(this.apiAccessId, HttpMethod.DELETE, null, typeRef, accessRequestId);
        assertEquals(HttpStatus.OK, response.getStatusCode());

        response = restTemplate.exchange(this.apiAccessId, HttpMethod.DELETE, null, typeRef, accessRequestId);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }
}
