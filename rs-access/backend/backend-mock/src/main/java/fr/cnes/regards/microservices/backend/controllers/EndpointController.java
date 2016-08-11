package fr.cnes.regards.microservices.backend.controllers;

import static java.util.Arrays.asList;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

import java.util.HashMap;
import java.util.List;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.Link;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import fr.cnes.regards.microservices.core.auth.MethodAutorizationService;
import fr.cnes.regards.microservices.core.auth.RoleAuthority;

@RestController
@EnableResourceServer
@RequestMapping("/api")
public class EndpointController {

    @Autowired
    MethodAutorizationService authService_;
    
	/**
     * Method to initiate REST resources authorizations.
     */
    @PostConstruct
    public void initAuthorisations() {
        authService_.setAutorities("/api/endpoints@GET", new RoleAuthority("PUBLIC"), new RoleAuthority("ADMIN"));
    }

    @RequestMapping(value = "/endpoints", method=RequestMethod.GET)
    public @ResponseBody ResponseEntity<HashMap<String, String>> getEndpoints() {
       return ResponseEntity.ok(buildEndpoints());
    }
    
    public HashMap<String, String> buildEndpoints() {
        // Note this is unfortunately hand-written. If you add a new entity, have to manually add a new link
        final List<Link> links = asList(
  		  linkTo(methodOn(ProjectController.class).getProjects()).withRel("projects_url"),
  		  linkTo(methodOn(ProjectUserController.class).getProjectUsers()).withRel("projects_users_url")
        );
        
        final HashMap<String, String> map = new HashMap<>();
        for (Link link : links) {
			map.put(link.getRel(), link.getHref());
		}
        
        return map;
    }
    
}
