package fr.cnes.regards.microservices.modules.test.accesses.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

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
import fr.cnes.regards.modules.accessRights.domain.ProjectUser;
import fr.cnes.regards.modules.accessRights.service.ProjectUserServiceStub;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class AccessesControllerIT extends RegardsIntegrationTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(AccessesControllerIT.class);

    private TestRestTemplate restTemplate;

    private String apiAccesses;

    private String apiAccessId;

    @Autowired
    private ProjectUserServiceStub serviceStub;

    @Value("${root.admin.login:admin}")
    private String rootAdminLogin;

    @Value("${root.admin.password:admin}")
    private String rootAdminPassword;

    @Before
    public void init() {
        if (restTemplate == null) {
            restTemplate = buildOauth2RestTemplate("acme", "acmesecret", "admin", "admin", "");
        }
        this.apiAccesses = getApiEndpoint().concat("/projects");
        this.apiAccessId = this.apiAccesses + "/{project_id}";
    }

    @Test
    public void aGetAllAccesses() {

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
        ProjectUser newAccessRequest;
        newAccessRequest = new ProjectUser("email");

        ResponseEntity<ProjectUser> response = restTemplate.postForEntity(this.apiAccesses, newAccessRequest,
                                                                          ProjectUser.class);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());

        ResponseEntity<ProjectUser> responseConflict = restTemplate.postForEntity(this.apiAccesses, newAccessRequest,
                                                                                  ProjectUser.class);
        assertEquals(HttpStatus.CONFLICT, responseConflict.getStatusCode());
    }

    @Test
    public void cGetProjectUser() {
        // make sure that ProjectUserUser with functional Identifier "name" is present.
        assertFalse(!this.serviceStub.existAccessRequest("email"));

        ParameterizedTypeReference<ProjectUser> typeRef = new ParameterizedTypeReference<ProjectUser>() {
        };
        ResponseEntity<ProjectUser> response = restTemplate.exchange(this.apiAccessId, HttpMethod.GET, null, typeRef,
                                                                     "name");
        assertEquals(HttpStatus.OK, response.getStatusCode());

        // make sure that ProjectUser with functional Identifier
        // "msdfqmsdfqbndsjkqfmsdbqjkmfsdjkqfbkmfbjkmsdfqsdfmqbsdq"
        // doesn't exist
        assertFalse(this.serviceStub.existAccessRequest("msdfqmsdfqbndsjkqfmsdbqjkmfsdjkqfbkmfbjkmsdfqsdfmqbsdq"));
        ResponseEntity<ProjectUser> responseNotFound = restTemplate
                .exchange(this.apiAccessId, HttpMethod.GET, null, typeRef,
                          "msdfqmsdfqbndsjkqfmsdbqjkmfsdjkqfbkmfbjkmsdfqsdfmqbsdq");
        assertEquals(HttpStatus.NOT_FOUND, responseNotFound.getStatusCode());

    }

    @Test
    public void dAcceptProjectUser() {
        // make sure that ProjectUser with functional Identifier "name" is present.
        assertThat(this.serviceStub.existAccessRequest("email"));
        ParameterizedTypeReference<Void> typeRef = new ParameterizedTypeReference<Void>() {
        };
        ResponseEntity<Void> response = restTemplate.exchange(this.apiAccessId + "/accept", HttpMethod.PUT, null,
                                                              typeRef, "email");
        assertEquals(HttpStatus.OK, response.getStatusCode());

        response = restTemplate.exchange(this.apiAccessId + "/accept", HttpMethod.PUT, null, typeRef, "email");
        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());

        response = restTemplate.exchange(this.apiAccessId + "/accept", HttpMethod.PUT, null, typeRef,
                                         "emailljkùqflsdbnqlùfsdqùdfnlùqsdn");
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());

    }

    @Test
    public void dDenyProjectUser() {
        // make sure that ProjectUser with functional Identifier "name" is present.
        assertFalse(this.serviceStub.existAccessRequest("email"));
        ParameterizedTypeReference<Void> typeRef = new ParameterizedTypeReference<Void>() {
        };
        ResponseEntity<Void> response = restTemplate.exchange(this.apiAccessId + "/accept", HttpMethod.PUT, null,
                                                              typeRef, "email");
        assertEquals(HttpStatus.OK, response.getStatusCode());

        response = restTemplate.exchange(this.apiAccessId + "/accept", HttpMethod.PUT, null, typeRef, "email");
        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());

        response = restTemplate.exchange(this.apiAccessId + "/accept", HttpMethod.PUT, null, typeRef,
                                         "emailljkùqflsdbnqlùfsdqùdfnlùqsdn");
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());

    }

    @Test
    public void eDeleteProjectUser() {
        ParameterizedTypeReference<Void> typeRef = new ParameterizedTypeReference<Void>() {
        };
        ResponseEntity<Void> response = restTemplate.exchange(this.apiAccessId, HttpMethod.DELETE, null, typeRef,
                                                              "email");
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }
}
