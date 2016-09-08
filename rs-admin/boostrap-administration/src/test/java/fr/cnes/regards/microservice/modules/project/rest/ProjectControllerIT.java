package fr.cnes.regards.microservice.modules.project.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import fr.cnes.regards.modules.project.domain.Project;
import fr.cnes.regards.modules.project.service.ProjectServiceStub;

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
    public void getAllProjects() {

        List<Project> allProjects = this.serviceStub.retrieveProjectList();

        // we have to use exchange instead of getForEntity as long as we use List otherwise the response body is not
        // well casted.
        ParameterizedTypeReference<List<Project>> typeRef = new ParameterizedTypeReference<List<Project>>() {
        };
        ResponseEntity<List<Project>> response = restTemplate.exchange(this.apiProjects, HttpMethod.GET, null, typeRef);
        List<Project> received = response.getBody();
        assertThat((allProjects.size() == received.size()) && (received.stream().filter(p -> allProjects.contains(p))
                .collect(Collectors.toList()).size() == allProjects.size()));
    }

    @Test
    public void createProject() {
        Project newProject;
        newProject = new Project("description", "iconICON", Boolean.TRUE, "ilFautBienUnNomPourTester");

        ResponseEntity<Project> response = restTemplate.postForEntity(this.apiProjects, newProject, Project.class);

        Project received = response.getBody();
        assertEquals(newProject, received);
        newProject.setDescription("NotTheSameOne");
        assertFalse(received.equals(newProject));
    }

    @Test
    public void getProject() {
        Project target = this.serviceStub.retrieveProject("name");
        ParameterizedTypeReference<Project> typeRef = new ParameterizedTypeReference<Project>() {
        };
        ResponseEntity<Project> response = restTemplate.exchange(this.apiProjectId, HttpMethod.GET, null, typeRef,
                                                                 "name");
        assertEquals(target, response.getBody());
    }
}
