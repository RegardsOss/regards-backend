package fr.cs.regards.controlers;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.*;
import fr.cs.regards.pojo.Plugin;
import fr.cs.regards.pojo.Project;
import fr.cs.regards.pojo.ProjectAdmin;

@RestController
// Indicates that those resources are securised. Only the /oauth endpoint do not need the authentication token
@EnableResourceServer
@RequestMapping("/api")
public class Controler {
	
	private static SendTime timeThread_ = null;
	
	@Autowired
    private SimpMessagingTemplate template;
	
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
	
	/**
	 * Method to start the web socket server timer.
	 * The timer send current date every 2 seconds to the web sockets servers
	 * @return
	 */
	@RequestMapping(value="time/start", method = RequestMethod.GET)
    public @ResponseBody String startTime() {
		if (timeThread_ == null){
			System.out.println("starting time thread !!!!");
			timeThread_ = new SendTime(this);
			timeThread_.run();
		}
		return "OK";
    }
	
	@RequestMapping(value="project-admin", method = RequestMethod.GET)
	public @ResponseBody HttpEntity<ProjectAdmin> getProjectAdmin(
			@RequestParam(value = "name", required = true) String name) {
		ProjectAdmin projectAdmin = new ProjectAdmin("John");
		projectAdmin.add(linkTo(methodOn(Controler.class).getProjectAdmin("John")).withSelfRel());
		return new ResponseEntity<ProjectAdmin>(projectAdmin, HttpStatus.OK);
	}
	
	@RequestMapping(value="project-admins", method = RequestMethod.GET)
    public @ResponseBody HttpEntity<List<ProjectAdmin>> getProjectAdmins() {
		List<ProjectAdmin> projectAdmins = new ArrayList<>();
		
		ProjectAdmin projectAdmin = new ProjectAdmin("John");
		projectAdmin.add(linkTo(methodOn(Controler.class).getProjectAdmin("John")).withSelfRel());
		projectAdmins.add(projectAdmin);
		
		projectAdmin = new ProjectAdmin("Mary");
		projectAdmin.add(linkTo(methodOn(Controler.class).getProjectAdmin("Mary")).withSelfRel());
		projectAdmins.add(projectAdmin);
		
		return new ResponseEntity<List<ProjectAdmin>>(projectAdmins, HttpStatus.OK);
    }
	
	/**
	 * Method to send curent date to web socket clients
	 */
	public void sendTime(){
		Date now = new Date();
		System.out.println("Sending time to websocket");
		// Send time to each client connected
        this.template.convertAndSend("/topic/time",now.toString());
	}

}
