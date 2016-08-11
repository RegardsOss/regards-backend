package fr.cnes.regards.microservices.${artifactId}.rest;

import java.util.concurrent.atomic.AtomicLong;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import fr.cnes.regards.microservices.core.auth.MethodAutorizationService;
import fr.cnes.regards.microservices.core.auth.ResourceAccess;
import fr.cnes.regards.microservices.core.auth.RoleAuthority;
import fr.cnes.regards.microservices.${artifactId}.domain.Greeting;
import fr.cnes.regards.microservices.${artifactId}.service.GreetingsService;


/**
 * 
 * myService Rest controller for the microservice
 *
 */
@RestController
@RequestMapping("/api")
public class GreetingController {
	
	private static final String template = "Hello, %s!";
	private final AtomicLong counter = new AtomicLong();
	
	@Autowired
	MethodAutorizationService authService_;
	
	@Autowired
	GreetingsService myService_;
	
	/**
	 * Method to iniate REST resources authorizations.
	 */
	@PostConstruct
	public void initAuthorisations() {
		authService_.setAutorities("/api/me@GET",new RoleAuthority("ADMIN"));
		authService_.setAutorities("/api/greeting@GET", new RoleAuthority("USER"));
	}

	/**
	 * Rest resource /api/greeting/{name} 
	 * Method GET 
	 * 
	 * @param name
	 * @return
	 */
	@ResourceAccess
	@RequestMapping(value="/greeting",method=RequestMethod.GET)
	public Greeting greeting(@RequestParam(value = "name", defaultValue = "World") String name) {
		return new Greeting(counter.incrementAndGet(), String.format(template, name));
	}
	
	/**
	 * Rest resource /api/me/{name}
	 * Method GET 
	 * 
	 * @param name
	 * @return
	 */
	@ResourceAccess
	@RequestMapping(value="/me",method=RequestMethod.GET)
	public Greeting me(@RequestParam(value = "name", defaultValue = "me") String name) {
		return new Greeting(counter.incrementAndGet(), String.format(template, name));
	}
	
}