package fr.cnes.regards.microservice.modules.test.project.rest;

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
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import fr.cnes.regards.microservices.modules.test.RegardsIntegrationTest;
import fr.cnes.regards.modules.project.domain.Project;
import fr.cnes.regards.modules.project.service.ProjectServiceStub;

/**
 * Just Test the REST API so status code. Correction is left to others.
 *
 * @author svissier
 *
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ProjectControllerIT extends RegardsIntegrationTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProjectControllerIT.class);

    private TestRestTemplate restTemplate;

    private String apiProjects;

    private String apiProjectId;

    @Autowired
    private ProjectServiceStub serviceStub;

    @Value("${root.admin.login:admin}")
    private String rootAdminLogin;

    @Value("${root.admin.password:admin}")
    private String rootAdminPassword;

    @Before
    public void init() {
        if (restTemplate == null) {
            restTemplate = buildOauth2RestTemplate("acme", "acmesecret", "admin", "admin", "");
        }
        this.apiProjects = getApiEndpoint().concat("/projects");
        this.apiProjectId = this.apiProjects + "/{project_id}";
    }

    @Test
    public void aGetAllProjects() {

        // we have to use exchange instead of getForEntity as long as we use List otherwise the response body is not
        // well casted.
        ParameterizedTypeReference<List<Project>> typeRef = new ParameterizedTypeReference<List<Project>>() {
        };
        ResponseEntity<List<Project>> response = restTemplate.exchange(this.apiProjects, HttpMethod.GET, null, typeRef);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    public void bCreateProject() {
        Project newProject;
        newProject = new Project("description", "iconICON", Boolean.TRUE, "ilFautBienUnNomPourTester");

        ResponseEntity<Project> response = restTemplate.postForEntity(this.apiProjects, newProject, Project.class);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());

        ResponseEntity<Project> responseConflict = restTemplate.postForEntity(this.apiProjects, newProject,
                                                                              Project.class);
        assertEquals(HttpStatus.CONFLICT, responseConflict.getStatusCode());
    }

    @Test
    public void cGetProject() {
        // make sure that Project with functional Identifier "name" is present.
        assertFalse(!this.serviceStub.existProject("name"));

        ParameterizedTypeReference<Project> typeRef = new ParameterizedTypeReference<Project>() {
        };
        ResponseEntity<Project> response = restTemplate.exchange(this.apiProjectId, HttpMethod.GET, null, typeRef,
                                                                 "name");
        assertEquals(HttpStatus.OK, response.getStatusCode());

        // make sure that Project with functional Identifier "msdfqmsdfqbndsjkqfmsdbqjkmfsdjkqfbkmfbjkmsdfqsdfmqbsdq"
        // doesn't exist
        assertFalse(this.serviceStub.existProject("msdfqmsdfqbndsjkqfmsdbqjkmfsdjkqfbkmfbjkmsdfqsdfmqbsdq"));
        ResponseEntity<Project> responseNotFound = restTemplate
                .exchange(this.apiProjectId, HttpMethod.GET, null, typeRef,
                          "msdfqmsdfqbndsjkqfmsdbqjkmfsdjkqfbkmfbjkmsdfqsdfmqbsdq");
        assertEquals(HttpStatus.NOT_FOUND, responseNotFound.getStatusCode());

    }

    @Test
    public void dUpdateProject() {
        // make sure that Project with functional Identifier "name" is present.
        assertThat(this.serviceStub.existProject("name"));
        Project updated = this.serviceStub.retrieveProject("name");
        updated.setDescription("AnOtherDescription");
        ParameterizedTypeReference<Void> typeRef = new ParameterizedTypeReference<Void>() {
        };
        HttpEntity<Project> request = new HttpEntity<>(updated);
        ResponseEntity<Void> response = restTemplate.exchange(this.apiProjectId, HttpMethod.PUT, request, typeRef,
                                                              "name");
        // if that's the same functional ID and the parameter is valid:
        assertEquals(HttpStatus.OK, response.getStatusCode());

        // if that's not the same functional ID and the parameter is valid:
        Project notSameID = new Project("desc", "icon", Boolean.TRUE, "AnotherName");
        HttpEntity<Project> requestOperationNotAllowed = new HttpEntity<>(notSameID);
        ResponseEntity<Void> responseOperationNotAllowed = restTemplate
                .exchange(this.apiProjectId, HttpMethod.PUT, requestOperationNotAllowed, typeRef, "name");
        assertEquals(HttpStatus.METHOD_NOT_ALLOWED, responseOperationNotAllowed.getStatusCode());
    }

    @Test
    public void eDeleteProject() {
        ParameterizedTypeReference<Void> typeRef = new ParameterizedTypeReference<Void>() {
        };
        ResponseEntity<Void> response = restTemplate.exchange(this.apiProjectId, HttpMethod.DELETE, null, typeRef,
                                                              "name");
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }
}
