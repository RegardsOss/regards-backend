package fr.cnes.regards.microservices.backend.controlers;

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

import fr.cnes.regards.microservices.backend.pojo.ProjectUser;
import fr.cnes.regards.microservices.core.auth.MethodAutorizationService;
import fr.cnes.regards.microservices.core.auth.ResourceAccess;
import fr.cnes.regards.microservices.core.auth.RoleAuthority;

@RestController
@EnableResourceServer
@RequestMapping("/api")
public class ProjectUserController {
	
	@Autowired
	MethodAutorizationService authService_;
	
	/**
	 * Method to iniate REST resources authorizations.
	 */
	@PostConstruct
	public void initAuthorisations() {
		authService_.setAutorities("/api/project/users@GET",new RoleAuthority("ADMIN"));
	}

	@ResourceAccess
	@RequestMapping(value = "/project/users", method = RequestMethod.GET)
	public @ResponseBody HttpEntity<List<ProjectUser>> getProjectUsers(
			@RequestParam(value = "project_id", required = true) Integer project_id) {

		List<ProjectUser> users = new ArrayList<>();
		users.add(new ProjectUser("Jose"));
		users.add(new ProjectUser("Sam"));
		users.add(new ProjectUser("Michel"));
		users.add(new ProjectUser("Louis"));
		
		return new ResponseEntity<List<ProjectUser>>(users, HttpStatus.OK);
	}

	
	
}

