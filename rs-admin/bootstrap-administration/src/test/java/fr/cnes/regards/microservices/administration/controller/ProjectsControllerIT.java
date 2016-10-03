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
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.web.bind.annotation.RequestMethod;

import fr.cnes.regards.microservices.core.security.endpoint.MethodAuthorizationService;
import fr.cnes.regards.microservices.core.test.RegardsIntegrationTest;
import fr.cnes.regards.modules.project.domain.Project;
import fr.cnes.regards.modules.project.service.IProjectService;
import fr.cnes.regards.security.utils.jwt.JWTService;

/**
 * Just Test the REST API so status code. Correction is left to others.
 *
 * @author svissier
 *
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ProjectsControllerIT extends RegardsIntegrationTest {

    @Autowired
    private JWTService jwtService_;

    @Autowired
    private MethodAuthorizationService authService_;

    private String jwt_;

    private String apiProjects;

    private String apiProjectId;

    private String errorMessage;

    @Autowired
    private IProjectService projectService_;

    @Before
    public void init() {
        setLogger(LoggerFactory.getLogger(ProjectsControllerIT.class));
        jwt_ = jwtService_.generateToken("PROJECT", "email", "SVG", "USER");
        authService_.setAuthorities("/projects", RequestMethod.GET, "USER");
        authService_.setAuthorities("/projects", RequestMethod.POST, "USER");
        authService_.setAuthorities("/projects/{project_id}", RequestMethod.GET, "USER");
        authService_.setAuthorities("/projects/{project_id}", RequestMethod.PUT, "USER");
        authService_.setAuthorities("/projects/{project_id}", RequestMethod.DELETE, "USER");
        errorMessage = "Cannot reach model attributes";
        apiProjects = "/projects";
        apiProjectId = apiProjects + "/{project_id}";
    }

    @Test
    public void aGetAllProjects() {
        List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(status().isOk());
        performGet(apiProjects, jwt_, expectations, errorMessage);

    }

    @Test
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
    public void dUpdateProject() {

        assertTrue(projectService_.existProject("name"));
        Project updated = projectService_.retrieveProject("name");
        updated.setDescription("AnOtherDescription");

        List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(status().isOk());
        performPut(apiProjectId, jwt_, updated, expectations, errorMessage, "name");

        Project notSameID = new Project(454L, "desc", "icon", Boolean.TRUE, "AnotherName");

        expectations = new ArrayList<>(1);
        expectations.add(status().isMethodNotAllowed());
        performPut(apiProjectId, jwt_, notSameID, expectations, errorMessage, "name");

    }

    @Test
    public void eDeleteProject() {
        List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(status().isOk());
        performDelete(apiProjectId, jwt_, expectations, errorMessage, "name");

    }
}
