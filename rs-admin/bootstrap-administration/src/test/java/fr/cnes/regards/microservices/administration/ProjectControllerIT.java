/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.microservices.administration;

import static org.junit.Assert.assertFalse;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.Assert;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import fr.cnes.regards.microservices.core.security.jwt.JWTService;
import fr.cnes.regards.microservices.modules.test.RegardsIntegration;
import fr.cnes.regards.modules.project.domain.Project;
import fr.cnes.regards.modules.project.service.ProjectServiceStub;

/**
 * Just Test the REST API so status code. Correction is left to others.
 *
 * @author svissier
 *
 */
// @RunWith(SpringRunner.class)
// @SpringBootTest(webEnvironment = WebEnvironment.DEFINED_PORT)
// @AutoConfigureMockMvc
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ProjectControllerIT extends RegardsIntegration {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProjectControllerIT.class);

    @Autowired
    private JWTService jwtService_;

    private String jwt_;

    private String apiProjects;

    private String apiProjectId;

    @Autowired
    private ProjectServiceStub serviceStub;

    @Before
    public void init() {
        jwt_ = jwtService_.generateToken("PROJECT", "email", "SVG", "USER");
        apiProjects = "/projects";
        apiProjectId = apiProjects + "/{project_id}";
    }

    @Test
    public void aGetAllProjects() {

        // we have to use exchange instead of getForEntity as long as we use List otherwise the response body is not
        // well casted.
        // ParameterizedTypeReference<List<Project>> typeRef = new ParameterizedTypeReference<List<Project>>() {
        // };
        // ResponseEntity<List<Project>> response = restTemplate.exchange(apiProjects, HttpMethod.GET, null, typeRef);
        // assertEquals(HttpStatus.OK, response.getStatusCode());
        jwt_ = jwtService_.generateToken("PROJECT", "email", "SVG", "USER");
        performGet(apiProjects, jwt_, status().isOk());

    }

    @Test
    public void bCreateProject() {
        Project newProject;
        newProject = new Project("description", "iconICON", Boolean.TRUE, "ilFautBienUnNomPourTester");

        try {
            // ResponseEntity<Project)> response = restTemplate.postForEntity(apiProjects, newProject, Project.class);
            // assertEquals(HttpStatus.CREATED, response.getStatusCode());
            jwt_ = jwtService_.generateToken("PROJECT", "email", "SVG", "USER");
            performPost(apiProjects, jwt_, newProject, status().isCreated());
            // ResponseEntity<Project> responseConflict = restTemplate.postForEntity(apiProjects, newProject,
            // Project.class);
            // assertEquals(HttpStatus.CONFLICT, responseConflict.getStatusCode());
            jwt_ = jwtService_.generateToken("PROJECT", "email", "SVG", "USER");
            performPost(apiProjects, jwt_, newProject, status().isConflict());
        }
        // catch (IOException e) {
        // String message = "Cannot (de)serialize model attributes";
        // LOGGER.error(message, e);
        // Assert.fail(message);
        // }
        catch (Exception e) {
            String message = "Cannot reach model attributes";
            LOGGER.error(message, e);
            Assert.fail(message);
        }

    }

    @Test
    public void cGetProject() {
        // make sure that Project with functional Identifier "name" is present.
        assertFalse(!serviceStub.existProject("name"));
        //
        // ParameterizedTypeReference<Project> typeRef = new ParameterizedTypeReference<Project>() {
        // };
        // ResponseEntity<Project> response = restTemplate.exchange(apiProjectId, HttpMethod.GET, null, typeRef,
        // "name");
        // assertEquals(HttpStatus.OK, response.getStatusCode());
        try {
            performGet(apiProjectId, jwt_, status().isOk(), "name");

            // make sure that Project with functional Identifier
            // "msdfqmsdfqbndsjkqfmsdbqjkmfsdjkqfbkmfbjkmsdfqsdfmqbsdq"
            // doesn't exist
            assertFalse(serviceStub.existProject("msdfqmsdfqbndsjkqfmsdbqjkmfsdjkqfbkmfbjkmsdfqsdfmqbsdq"));
            // ResponseEntity<Project> responseNotFound = restTemplate
            // .exchange(apiProjectId, HttpMethod.GET, null, typeRef,
            // "msdfqmsdfqbndsjkqfmsdbqjkmfsdjkqfbkmfbjkmsdfqsdfmqbsdq");
            // assertEquals(HttpStatus.NOT_FOUND, responseNotFound.getStatusCode());
            performGet(apiProjectId, jwt_, status().isNotFound(), "mqsdfhnlùsdfqhnjlm");
        }
        catch (Exception e) {
            String message = "Cannot reach model attributes";
            LOGGER.error(message, e);
            Assert.fail(message);
        }

    }

    @Test
    public void dUpdateProject() {
        // make sure that Project with functional Identifier "name" is present.
        assertFalse(!serviceStub.existProject("name"));
        Project updated = serviceStub.retrieveProject("name");
        updated.setDescription("AnOtherDescription");
        // ParameterizedTypeReference<Void> typeRef = new ParameterizedTypeReference<Void>() {
        // };
        // HttpEntity<Project> request = new HttpEntity<>(updated);
        // ResponseEntity<Void> response = restTemplate.exchange(apiProjectId, HttpMethod.PUT, request, typeRef,
        // "name");
        // // if that's the same functional ID and the parameter is valid:
        // assertEquals(HttpStatus.OK, response.getStatusCode());
        try {
            jwt_ = jwtService_.generateToken("PROJECT", "email", "SVG", "USER");
            performPut(apiProjectId, jwt_, updated, status().isOk(), "name");

            // if that's not the same functional ID and the parameter is valid:
            Project notSameID = new Project("desc", "icon", Boolean.TRUE, "AnotherName");
            // HttpEntity<Project> requestOperationNotAllowed = new HttpEntity<>(notSameID);
            // ResponseEntity<Void> responseOperationNotAllowed = restTemplate
            // .exchange(apiProjectId, HttpMethod.PUT, requestOperationNotAllowed, typeRef, "name");
            // assertEquals(HttpStatus.METHOD_NOT_ALLOWED, responseOperationNotAllowed.getStatusCode());
            jwt_ = jwtService_.generateToken("PROJECT", "email", "SVG", "USER");
            performPut(apiProjectId, jwt_, notSameID, status().isMethodNotAllowed(), "name");
        }
        // catch (IOException e) {
        // String message = "Cannot (de)serialize model attributes";
        // LOGGER.error(message, e);
        // Assert.fail(message);
        // }
        catch (Exception e) {
            String message = "Cannot reach model attributes";
            LOGGER.error(message, e);
            Assert.fail(message);
        }
    }

    @Test
    public void eDeleteProject() {
        // ParameterizedTypeReference<Void> typeRef = new ParameterizedTypeReference<Void>() {
        // };
        // ResponseEntity<Void> response = restTemplate.exchange(apiProjectId, HttpMethod.DELETE, null, typeRef,
        // "name");
        // assertEquals(HttpStatus.OK, response.getStatusCode());
        try {
            jwt_ = jwtService_.generateToken("PROJECT", "email", "SVG", "USER");
            performDelete(apiProjectId, jwt_, status().isOk(), "name");
        }
        catch (Exception e) {
            String message = "Cannot reach model attributes";
            LOGGER.error(message, e);
            Assert.fail(message);
        }
    }
}
