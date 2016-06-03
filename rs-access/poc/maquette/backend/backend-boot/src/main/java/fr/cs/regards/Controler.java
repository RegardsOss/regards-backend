package fr.cs.regards;

import java.util.ArrayList;
import java.util.List;

import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;

import fr.cs.regards.pojo.Plugin;
import fr.cs.regards.pojo.Project;

@RestController
@RequestMapping("/api")
public class Controler {
	
	@RequestMapping(value="projects", method = RequestMethod.GET)
    public @ResponseBody List<Project> getProjects() {
		List<Project> projects = new ArrayList<>();
		projects.add(new Project("cdpp"));
		projects.add(new Project("ssalto"));
        return projects;
    }
	
	@RequestMapping(value="plugins", method = RequestMethod.GET)
    public @ResponseBody List<Plugin> getPlugins() {
		List<Plugin> plugins = new ArrayList<>();
		plugins.add(new Plugin("HelloWorldPlugin","HelloWorldPlugin/hw.js"));
        return plugins;
    }

}
