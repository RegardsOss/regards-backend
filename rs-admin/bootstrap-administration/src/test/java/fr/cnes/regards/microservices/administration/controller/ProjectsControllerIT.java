/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.microservices.administration.controller;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.web.bind.annotation.RequestMethod;

import fr.cnes.regards.framework.security.autoconfigure.endpoint.DefaultMethodAuthorizationServiceImpl;
import fr.cnes.regards.framework.security.utils.jwt.JWTService;
import fr.cnes.regards.framework.test.integration.AbstractRegardsIntegrationTest;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import fr.cnes.regards.modules.core.exception.EntityException;
import fr.cnes.regards.modules.project.domain.Project;
import fr.cnes.regards.modules.project.domain.ProjectConnection;
import fr.cnes.regards.modules.project.service.IProjectService;

/**
 * Just Test the REST API so status code. Correction is left to others.
 *
 * @author svissier
 *
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@ActiveProfiles("test")
public class ProjectsControllerIT extends AbstractRegardsIntegrationTest {

    @Autowired
    private JWTService jwtService_;

    @Autowired
    private DefaultMethodAuthorizationServiceImpl authService_;

    private String jwt_;

    private String apiProjects;

    private String apiProjectId;

    private String apiProjectConnection;

    private String apiProjectConnections;

    private String errorMessage;

    @Autowired
    private IProjectService projectService_;

    @Before
    public void init() {
        setLogger(LoggerFactory.getLogger(ProjectsControllerIT.class));
        jwt_ = jwtService_.generateToken("PROJECT", "email", "SVG", "USER");

        apiProjectConnection = "/projects/{project_name}/connection/{microservice}";
        apiProjectConnections = "/projects/connections";

        authService_.setAuthorities("/projects", RequestMethod.GET, "USER");
        authService_.setAuthorities("/projects", RequestMethod.POST, "USER");
        authService_.setAuthorities("/projects/{project_name}", RequestMethod.GET, "USER");
        authService_.setAuthorities("/projects/{project_name}", RequestMethod.PUT, "USER");
        authService_.setAuthorities("/projects/{project_name}", RequestMethod.DELETE, "USER");

        authService_.setAuthorities(apiProjectConnection, RequestMethod.GET, "USER");
        authService_.setAuthorities(apiProjectConnection, RequestMethod.DELETE, "USER");

        authService_.setAuthorities(apiProjectConnections, RequestMethod.PUT, "USER");
        authService_.setAuthorities(apiProjectConnections, RequestMethod.POST, "USER");

        errorMessage = "Cannot reach model attributes";
        apiProjects = "/projects";
        apiProjectId = apiProjects + "/{project_name}";

    }

    @Test
    @Requirement("REGARDS_DSL_ADM_INST_130")
    @Purpose("Check that the system allows to retrieve all projects for an instance.")
    public void aGetAllProjects() {
        final List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(status().isOk());
        performGet(apiProjects, jwt_, expectations, errorMessage);

    }

    @Test
    @Requirement("REGARDS_DSL_ADM_INST_100")
    @Purpose("Check that the system allows to create a project on an instance and handle fail cases.")
    public void bCreateProject() {
        Project newProject;
        newProject = new Project(54242L, "description", "iconICON", Boolean.TRUE, "ilFautBienUnNomPourTester");

        List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(status().isCreated());
        performPost(apiProjects, jwt_, newProject, expectations, errorMessage);

        expectations = new ArrayList<>(1);
        expectations.add(status().isConflict());
        performPost(apiProjects, jwt_, newProject, expectations, errorMessage);

    }

    @Test
    @Requirement("REGARDS_DSL_ADM_INST_100")
    @Purpose("Check that the system allows to retrieve a project on an instance and handle fail cases.")
    public void cGetProject() {
        assertFalse(!projectService_.existProject("name"));

        List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(status().isOk());
        performGet(apiProjectId, jwt_, expectations, errorMessage, "name");

        assertFalse(projectService_.existProject("msdfqmsdfqbndsjkqfmsdbqjkmfsdjkqfbkmfbjkmsdfqsdfmqbsdq"));

        expectations = new ArrayList<>(1);
        expectations.add(status().isNotFound());
        performGet(apiProjectId, jwt_, expectations, errorMessage, "mqsdfhnlùsdfqhnjlm");
    }

    @Test
    @Requirement("REGARDS_DSL_ADM_INST_100")
    @Purpose("Check that the system allows to update a project on an instance and handle fail cases.")
    public void dUpdateProject() throws EntityException {

        assertTrue(projectService_.existProject("name"));
        final Project updated = projectService_.retrieveProject("name");
        updated.setDescription("AnOtherDescription");

        List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(status().isOk());
        performPut(apiProjectId, jwt_, updated, expectations, errorMessage, "name");

        final Project notSameID = new Project(454L, "desc", "icon", Boolean.TRUE, "AnotherName");

        expectations = new ArrayList<>(1);
        expectations.add(status().isMethodNotAllowed());
        performPut(apiProjectId, jwt_, notSameID, expectations, errorMessage, "name");

    }

    @Test
    @Requirement("REGARDS_DSL_ADM_INST_100")
    @Purpose("Check that the system allows to delete a project on an instance.")
    public void eDeleteProject() {
        final List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(status().isOk());
        performDelete(apiProjectId, jwt_, expectations, errorMessage, "name");
    }

    /**
     *
     * Test creation of a new database connection for a given project and a given microservice
     *
     * @since 1.0-SNAPSHOT
     */
    @Requirement("REGARDS_DSL_SYS_ARC_050")
    @Purpose("Test creation of a new database connection for a given project and a given microservice.")
    @Test
    public void createProjectConnection() {

        Project newProject;
        newProject = new Project(60L, "description", "iconICON", Boolean.TRUE, "create-project");

        // Initialize database with a valid project
        final List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(status().isCreated());
        performPost(apiProjects, jwt_, newProject, expectations, errorMessage);

        // Test creation of a valid project connection
        ProjectConnection projectConnection = new ProjectConnection(10L, newProject, "create-microservice", "user",
                "pwd", "driver", "url");
        performPost(apiProjectConnections, jwt_, projectConnection, expectations, errorMessage);

        // Test creation of a already existing project connection
        expectations.clear();
        expectations.add(status().isConflict());
        performPost(apiProjectConnections, jwt_, projectConnection, expectations, errorMessage);
        projectConnection = new ProjectConnection(20L, newProject, "create-microservice", "user", "pwd", "driver",
                "url");
        performPost(apiProjectConnections, jwt_, projectConnection, expectations, errorMessage);

        // Test creation of a project connection associated to an non existing project
        expectations.clear();
        expectations.add(status().isNotFound());
        final Project inexistingProject = new Project(70L, "description", "iconICON", Boolean.TRUE,
                "create-project-inexisting");
        projectConnection = new ProjectConnection(10L, inexistingProject, "create-microservice", "user", "pwd",
                "driver", "url");
        performPost(apiProjectConnections, jwt_, projectConnection, expectations, errorMessage);

    }

    /**
     *
     * Test deletion of a database connection for a given project and a given microservice.
     *
     * @since 1.0-SNAPSHOT
     */
    @Requirement("REGARDS_DSL_SYS_ARC_050")
    @Purpose("Test deletion of a database project connection for a given project and a given microservice.")
    @Test
    public void deleteProjectConnection() {

        // Test deletion of non existing project connection
        final List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(status().isNotFound());
        performDelete(apiProjectConnection, jwt_, expectations, errorMessage, "delete-project", "delete-microservice");

        // Create a project and a project connection to delete
        Project newProject;
        newProject = new Project(61L, "description", "iconICON", Boolean.TRUE, "delete-project");
        expectations.clear();
        expectations.add(status().isCreated());
        performPost(apiProjects, jwt_, newProject, expectations, errorMessage);
        final ProjectConnection projectConnection = new ProjectConnection(11L, newProject, "delete-microservice",
                "user", "pwd", "driver", "url");
        performPost(apiProjectConnections, jwt_, projectConnection, expectations, errorMessage);

        // Delete a valid project connection
        expectations.clear();
        expectations.add(status().isOk());
        performDelete(apiProjectConnection, jwt_, expectations, errorMessage, "delete-project", "delete-microservice");

    }

    /**
     *
     * Test update of a database connection for a given project and a given microservice.
     *
     * @since 1.0-SNAPSHOT
     */
    @Requirement("REGARDS_DSL_SYS_ARC_050")
    @Purpose("Test update of a database project connection.")
    @Test
    public void updateProjectConnection() {

        Project newProject;
        newProject = new Project(62L, "description", "iconICON", Boolean.TRUE, "update-project");
        final ProjectConnection projectConnection = new ProjectConnection(12L, newProject, "update-microservice",
                "user", "pwd", "driver", "url");

        // Test updating a non existing project connection
        final List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(status().isNotFound());
        performPut(apiProjectConnections, jwt_, projectConnection, expectations, errorMessage);

        // Initialization of database with valid project and project connection
        expectations.clear();
        expectations.add(status().isCreated());
        performPost(apiProjects, jwt_, newProject, expectations, errorMessage);
        performPost(apiProjectConnections, jwt_, projectConnection, expectations, errorMessage);

        // Test update
        projectConnection.setUrl("new url");
        expectations.clear();
        expectations.add(status().isOk());
        performPut(apiProjectConnections, jwt_, projectConnection, expectations, errorMessage);

    }

    /**
     *
     * Test update of a database connection for a given project and a given microservice.
     *
     * @since 1.0-SNAPSHOT
     */
    @Requirement("REGARDS_DSL_SYS_ARC_050")
    @Purpose("Test update of a database project connection.")
    @Test
    public void getProjectConnection() {

        // Test getting a non existing project connection
        final List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(status().isNotFound());
        performGet(apiProjectConnection, jwt_, expectations, errorMessage, "get-project", "get-microservice");

        // Initialization of database with valid project and project connection
        Project newProject;
        newProject = new Project(63L, "description", "iconICON", Boolean.TRUE, "get-project");
        final ProjectConnection projectConnection = new ProjectConnection(13L, newProject, "get-microservice", "user",
                "pwd", "driver", "url");
        expectations.clear();
        expectations.add(status().isCreated());
        performPost(apiProjects, jwt_, newProject, expectations, errorMessage);
        performPost(apiProjectConnections, jwt_, projectConnection, expectations, errorMessage);

        // Test getting existing project connection
        expectations.clear();
        expectations.add(status().isOk());
        performGet(apiProjectConnection, jwt_, expectations, errorMessage, "get-project", "get-microservice");

    }
}
