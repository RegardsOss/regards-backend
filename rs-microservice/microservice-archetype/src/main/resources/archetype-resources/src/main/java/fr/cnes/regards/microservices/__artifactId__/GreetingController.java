package fr.cnes.regards.microservices.${artifactId};


import java.util.concurrent.atomic.AtomicLong;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import fr.cnes.regards.microservices.core.auth.ResourceAccess;


/**
 * 
 * ${artifactId} Rest controller for the microservice
 *
 */
@RestController
@RequestMapping("/api")
public class GreetingController {
	
	private static final String template = "Hello, %s!";
	private final AtomicLong counter = new AtomicLong();

	@PreAuthorize("hasRole('USER')")
	@RequestMapping(value="/greeting",method=RequestMethod.GET)
	public Greeting greeting(@RequestParam(value = "name", defaultValue = "World") String name) {
		return new Greeting(counter.incrementAndGet(), String.format(template, name));
	}
	
	@ResourceAccess(name="ME", method="GET")
	@RequestMapping(value="/me",method=RequestMethod.GET)
	public Greeting me(@RequestParam(value = "name", defaultValue = "me") String name) {
		return new Greeting(counter.incrementAndGet(), String.format(template, name));
	}
	
	@RequestMapping(value="/hello",method=RequestMethod.GET)
	public String hellow(){
		return "Hello";
	}
}