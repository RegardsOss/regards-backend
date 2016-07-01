package fr.cs.regards.controlers;

import java.util.concurrent.atomic.AtomicLong;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.EurekaClient;

import fr.cs.regards.auth.ResourceAccess;


@RestController
@RequestMapping("/api")
public class GreetingController {
	
	@Autowired
    private EurekaClient discoveryClient_;
    
    public String serviceUrl() {
        InstanceInfo instance = discoveryClient_.getNextServerFromEureka("HelloWorld", false);
        return instance.getHomePageUrl();
    }

	private static final String template = "Hello, %s!";
	private final AtomicLong counter = new AtomicLong();

	@PreAuthorize("hasRole('USER')")
	@RequestMapping("/greeting")
	public Greeting greeting(@RequestParam(value = "name", defaultValue = "World") String name) {
		return new Greeting(counter.incrementAndGet(), String.format(template, name));
	}
	
	@ResourceAccess(name="ME", method="GET")
	@RequestMapping("/me")
	public Greeting me(@RequestParam(value = "name", defaultValue = "me") String name) {
		return new Greeting(counter.incrementAndGet(), String.format(template, name));
	}
	
	@RequestMapping("/eureka/adress")
	public String test() {
		return serviceUrl();
	}
}