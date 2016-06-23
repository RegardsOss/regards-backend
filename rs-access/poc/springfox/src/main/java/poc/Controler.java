package poc;


import java.util.ArrayList;
import java.util.List;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.annotations.ApiOperation;

import org.springframework.web.bind.annotation.RequestMapping;

@RestController
@RequestMapping("/api")
public class Controler {
	
	@ApiOperation(value = "")
	@RequestMapping(value="project", method = RequestMethod.GET)
	public @ResponseBody HttpEntity<Project> getProject(
			@RequestParam(value = "name", required = true) String pProjectName) {
		
		Project project = new Project();
		project.setName(pProjectName);
		project.setPublic(false);
		
		return new ResponseEntity<Project>(project, HttpStatus.OK);
	}
	
	@ApiOperation(value = "")
	@RequestMapping(value="projects", method = RequestMethod.GET)
    public @ResponseBody HttpEntity<List<Project>> getProjects() {
		
		List<Project> projects = new ArrayList<>();

		Project project = new Project();
		project.setName("premier");
		project.setPublic(false);
		
		Project project2 = new Project();
		project2.setName("deuxieme");
		project2.setPublic(true);
		
		projects.add(project);
		projects.add(project2);
		
		return new ResponseEntity<List<Project>>(projects, HttpStatus.OK);
    }

}
