package fr.cnes.regards.microservices.core.controlers;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

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
	
	@RequestMapping(value="/cloud/config",method=RequestMethod.GET)
	public String getConfigValue() {
		if (configServerEnabled_){
			return name;
		} else {
			return "Config server disabled !";
		}
	}

}
