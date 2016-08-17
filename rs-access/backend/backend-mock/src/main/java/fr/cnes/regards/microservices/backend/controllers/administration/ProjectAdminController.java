package fr.cnes.regards.microservices.backend.controllers.administration;

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

import fr.cnes.regards.microservices.backend.pojo.ProjectAdmin;
import fr.cnes.regards.microservices.core.auth.MethodAutorizationService;
import fr.cnes.regards.microservices.core.auth.ResourceAccess;
import fr.cnes.regards.microservices.core.auth.RoleAuthority;

@RestController
@EnableResourceServer
@RequestMapping("/api")
public class ProjectAdminController {

	@Autowired
	MethodAutorizationService authService_;

	/**
	 * Method to iniate REST resources authorizations.
	 */
	@PostConstruct
	public void initAuthorisations() {
		authService_.setAutorities("/api/project-admin@GET",new RoleAuthority("ADMIN"));
		authService_.setAutorities("/api/project-admins@GET", new RoleAuthority("ADMIN"));
	}

	@ResourceAccess
	@RequestMapping(value = "/project-admin", method = RequestMethod.GET)
	public @ResponseBody HttpEntity<ProjectAdmin> getProjectAdmin(
			@RequestParam(value = "name", required = true) String name) {
		ProjectAdmin projectAdmin = new ProjectAdmin(name);
		projectAdmin.add(linkTo(methodOn(ProjectAdminController.class).getProjectAdmin(name)).withSelfRel());
		return new ResponseEntity<ProjectAdmin>(projectAdmin, HttpStatus.OK);
	}

	@ResourceAccess
	@RequestMapping(value = "/project-admins", method = RequestMethod.GET)
	public @ResponseBody HttpEntity<List<ProjectAdmin>> getProjectAdminsByNames(
			@RequestParam(value = "names", required = true) String[] names) {
		List<ProjectAdmin> projectAdmins = new ArrayList<>(names.length);

		for (String name : names) {
			ProjectAdmin projectAdmin = new ProjectAdmin(name);
			projectAdmin.add(linkTo(methodOn(ProjectAdminController.class).getProjectAdmin(name)).withSelfRel());
			projectAdmins.add(projectAdmin);
		}

		return new ResponseEntity<List<ProjectAdmin>>(projectAdmins, HttpStatus.OK);
	}

}
