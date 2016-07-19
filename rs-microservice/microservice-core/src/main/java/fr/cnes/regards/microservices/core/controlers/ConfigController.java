package fr.cnes.regards.microservices.core.controlers;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import fr.cnes.regards.microservices.core.auth.MethodAutorizationService;
import fr.cnes.regards.microservices.core.auth.ResourceAccess;
import fr.cnes.regards.microservices.core.auth.RoleAuthority;

@RestController
@RequestMapping("/config")
public class ConfigController {
	
	/**
	 * Is the Config server enabled
	 */
	@Value("${cloud.config.server.enabled}")
	boolean configServerEnabled_ = false;
	
	/**
	 * Property to read from the config server
	 */
	@Value("${my.otherproperty}")
	String name = "Default value";
	
	@Autowired
	MethodAutorizationService authService_;

	@PostConstruct
	public void initAuthorisations() {
		authService_.setAutorities("/config/value@GET",new RoleAuthority("ADMIN"));
	}
	
	@ResourceAccess
	@RequestMapping(value="/value",method=RequestMethod.GET)
	public String getConfigValue() {
		if (configServerEnabled_){
			return name;
		} else {
			return "Config server disabled !";
		}
	}

}
