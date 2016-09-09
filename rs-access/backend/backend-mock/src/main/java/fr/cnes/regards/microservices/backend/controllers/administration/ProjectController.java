package fr.cnes.regards.microservices.backend.controllers.administration;

import fr.cnes.regards.microservices.backend.pojo.administration.Project;
import fr.cnes.regards.microservices.core.auth.MethodAutorizationService;
import fr.cnes.regards.microservices.core.auth.ResourceAccess;
import fr.cnes.regards.microservices.core.auth.RoleAuthority;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;
import org.springframework.web.bind.annotation.*;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

@RestController
@EnableResourceServer
@RequestMapping("/api")
public class ProjectController {

    @Autowired
    MethodAutorizationService authService_;

    /**
     * Method to initiate REST resources authorizations.
     */
    @PostConstruct
    public void initAuthorisations() {
        authService_.setAutorities("/api/project@GET", new RoleAuthority("PUBLIC"), new RoleAuthority("ADMIN"));
        authService_.setAutorities("/api/projects@GET", new RoleAuthority("PUBLIC"), new RoleAuthority("ADMIN"));
        authService_.setAutorities("/api/projects@POST", new RoleAuthority("PUBLIC"), new RoleAuthority("ADMIN"));
        authService_.setAutorities("/api/projects/{project_id}@GET", new RoleAuthority("PUBLIC"), new RoleAuthority("ADMIN"));
        authService_.setAutorities("/api/projects/{project_id}@PUT", new RoleAuthority("PUBLIC"), new RoleAuthority("ADMIN"));
        authService_.setAutorities("/api/projects/{project_id}@DELETE", new RoleAuthority("PUBLIC"), new RoleAuthority("ADMIN"));
    }

    @ResourceAccess
    @RequestMapping(value = "/projects", method = RequestMethod.GET)
    public @ResponseBody HttpEntity<List<Project>> getProjects() {
        return new ResponseEntity<>(getInMemory(), HttpStatus.OK);
    }

    @ResourceAccess
    @RequestMapping(value = "/projects", method = RequestMethod.POST)
    public @ResponseBody HttpEntity<List<Project>> addProject() {
        List<Project> projects = new ArrayList<>();
        Project project = new Project("newProject");
        project.setProjectId(3L);
        String[] cdppAdmins = {"Tom", "David"};
        project.add(linkTo(methodOn(ProjectController.class).getProject(3L)).withSelfRel());
        project.add(linkTo(methodOn(ProjectAdminController.class).getProjectAdminsByNames(cdppAdmins)).withRel("users"));
        projects.add(project);
        return new ResponseEntity<>(projects, HttpStatus.OK);
    }

    @ResourceAccess
    @RequestMapping(value = "/projects/{project_id}", method = RequestMethod.GET)
    public @ResponseBody HttpEntity<Project> getProject(@PathVariable("project_id") Long project_id) {
        Logger.getGlobal().info("project_id" + project_id);
        Project project = getInMemory()
                .stream()
                .filter(p -> p.getProjectId() == project_id)
                .findFirst()
                .get();

        return new ResponseEntity<>(project, HttpStatus.OK);
    }

    @ResourceAccess
    @RequestMapping(value = "/projects/{project_id}", method = RequestMethod.PUT)
    public @ResponseBody HttpEntity<Project> updateProject(@PathVariable("project_id") Long project_id) {
        Project project = getInMemory()
                .stream()
                .filter(p -> p.getProjectId() == project_id)
                .findFirst()
                .get();

        project.setName("Updated name");
        return new ResponseEntity<>(project, HttpStatus.OK);
    }

    @ResourceAccess
    @RequestMapping(value = "/projects/{project_id}", method = RequestMethod.DELETE)
    @ResponseStatus(value = HttpStatus.OK)
    public void deleteProject(@PathVariable("project_id") Long project_id) {
        // Simulate deletion
        getInMemory()
                .stream()
                .filter(p -> p.getProjectId() != project_id)
                .collect(Collectors.toList());
    }

    private List<Project> getInMemory() {
        List<Project> projects = new ArrayList<>();

        Project project = new Project("cdpp");
        project.setProjectId(0L);
        String[] cdppAdmins = {"Alice", "David", "Bob"};
        project.add(linkTo(methodOn(ProjectController.class).getProject(0L)).withSelfRel());
        project.add(linkTo(methodOn(ProjectAdminController.class).getProjectAdminsByNames(cdppAdmins)).withRel("users"));
        projects.add(project);

        project = new Project("ssalto");
        project.setProjectId(1L);
        String[] ssaltoAdmins = {"Carl", "David"};
        project.add(linkTo(methodOn(ProjectController.class).getProject(1L)).withSelfRel());
        project.add(linkTo(methodOn(ProjectAdminController.class).getProjectAdminsByNames(ssaltoAdmins)).withRel("users"));
        projects.add(project);

        return projects;
    }

}
