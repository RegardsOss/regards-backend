package regards.sample.security;

import java.util.concurrent.atomic.AtomicLong;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import regards.sample.security.auth.ResourceAccess;

@RestController
public class GreetingController {

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
}