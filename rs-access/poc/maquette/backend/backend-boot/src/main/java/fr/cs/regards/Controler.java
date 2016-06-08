package fr.cs.regards;

import java.util.ArrayList;
import java.util.List;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.*;
import fr.cs.regards.pojo.Plugin;
import fr.cs.regards.pojo.Project;

@RestController
@RequestMapping("/api")
public class Controler {
	
	@RequestMapping(value="project", method = RequestMethod.GET)
	public @ResponseBody HttpEntity<Project> getProject(
			@RequestParam(value = "name", required = true) String pProjectName) {
		Project project = new Project("cdpp");
		project.add(linkTo(methodOn(Controler.class).getProject("cdpp")).withSelfRel());
		return new ResponseEntity<Project>(project, HttpStatus.OK);
	}
	
	@RequestMapping(value="projects", method = RequestMethod.GET)
    public @ResponseBody HttpEntity<List<Project>> getProjects() {
		List<Project> projects = new ArrayList<>();
		Project project = new Project("cdpp");
		project.add(linkTo(methodOn(Controler.class).getProject("cdpp")).withSelfRel());
		projects.add(project);
		project = new Project("ssalto");
		project.add(linkTo(methodOn(Controler.class).getProject("cdpp")).withSelfRel());
		projects.add(project);		
		return new ResponseEntity<List<Project>>(projects, HttpStatus.OK);
    }
	
	@RequestMapping(value="plugins", method = RequestMethod.GET)
    public @ResponseBody List<Plugin> getPlugins() {
		List<Plugin> plugins = new ArrayList<>();
		plugins.add(new Plugin("HelloWorldPlugin","HelloWorldPlugin/hw.js"));
        return plugins;
    }

}
