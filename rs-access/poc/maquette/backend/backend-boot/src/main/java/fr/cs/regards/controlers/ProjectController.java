package fr.cs.regards.controlers;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

import java.util.ArrayList;
import java.util.List;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import fr.cs.regards.pojo.Project;

@RestController
@EnableResourceServer
@RequestMapping("/api")
public class ProjectController {

	@RequestMapping(value="project", method = RequestMethod.GET)
	public @ResponseBody HttpEntity<Project> getProject(
			@RequestParam(value = "name", required = true) String pProjectName) {
		Project project = new Project("cdpp");
		project.add(linkTo(methodOn(ProjectController.class).getProject("cdpp")).withSelfRel());
		return new ResponseEntity<Project>(project, HttpStatus.OK);
	}
	
	@RequestMapping(value="projects", method = RequestMethod.GET)
    public @ResponseBody HttpEntity<List<Project>> getProjects() {
		List<Project> projects = new ArrayList<>();
		
		Project project = new Project("cdpp");
		project.add(linkTo(methodOn(ProjectController.class).getProject("cdpp")).withSelfRel());
		project.add(linkTo(methodOn(ProjectAdminController.class).getProjectAdmins()).withRel("users"));
		projects.add(project);
		
		project = new Project("ssalto");
		project.add(linkTo(methodOn(ProjectController.class).getProject("ssalto")).withSelfRel());
		project.add(linkTo(methodOn(ProjectAdminController.class).getProjectAdmins()).withRel("users"));
		projects.add(project);		
		return new ResponseEntity<List<Project>>(projects, HttpStatus.OK);
    }
	
}
