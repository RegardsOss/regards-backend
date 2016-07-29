package fr.cnes.regards.microservices.backend.controlers;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import fr.cnes.regards.microservices.backend.pojo.Project;
import fr.cnes.regards.microservices.core.auth.MethodAutorizationService;
import fr.cnes.regards.microservices.core.auth.ResourceAccess;
import fr.cnes.regards.microservices.core.auth.RoleAuthority;

@RestController
@EnableResourceServer
@RequestMapping("/api")
public class ProjectController {

	@Autowired
	MethodAutorizationService authService_;

	/**
	 * Method to iniate REST resources authorizations.
	 */
	@PostConstruct
	public void initAuthorisations() {
		authService_.setAutorities("/api/project@GET",new RoleAuthority("PUBLIC"), new RoleAuthority("ADMIN"));
		authService_.setAutorities("/api/projects@GET", new RoleAuthority("PUBLIC"), new RoleAuthority("ADMIN"));
	}

	@ResourceAccess
	@RequestMapping(value="/project", method = RequestMethod.GET)
	public @ResponseBody HttpEntity<Project> getProject(
			@RequestParam(value = "name", required = true) String pProjectName) {
		Project project = new Project("cdpp");
		project.add(linkTo(methodOn(ProjectController.class).getProject("cdpp")).withSelfRel());
		return new ResponseEntity<Project>(project, HttpStatus.OK);
	}

	@ResourceAccess
	@RequestMapping(value="/projects", method = RequestMethod.GET)
    public @ResponseBody HttpEntity<List<Project>> getProjects() {
		List<Project> projects = new ArrayList<>();

		Project project = new Project("cdpp");
		String[] cdppAdmins = { "Alice", "David", "Bob" };
		project.add(linkTo(methodOn(ProjectController.class).getProject("cdpp")).withSelfRel());
		project.add(linkTo(methodOn(ProjectAdminController.class).getProjectAdminsByNames(cdppAdmins)).withRel("users"));
		projects.add(project);

		project = new Project("ssalto");
		String[] ssaltoAdmins = { "Carl", "David" };
		project.add(linkTo(methodOn(ProjectController.class).getProject("ssalto")).withSelfRel());
		project.add(linkTo(methodOn(ProjectAdminController.class).getProjectAdminsByNames(ssaltoAdmins)).withRel("users"));
		projects.add(project);
		return new ResponseEntity<List<Project>>(projects, HttpStatus.OK);
    }

}
