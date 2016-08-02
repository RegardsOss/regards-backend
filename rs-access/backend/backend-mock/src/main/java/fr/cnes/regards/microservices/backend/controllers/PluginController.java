package fr.cnes.regards.microservices.backend.controllers;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import fr.cnes.regards.microservices.backend.pojo.Plugin;
import fr.cnes.regards.microservices.core.auth.MethodAutorizationService;
import fr.cnes.regards.microservices.core.auth.ResourceAccess;
import fr.cnes.regards.microservices.core.auth.RoleAuthority;

@RestController
// Indicates that those resources are securised. Only the /oauth endpoint do not
// need the authentication token
@EnableResourceServer
@RequestMapping("/api")
public class PluginController {
	
	@Autowired
	MethodAutorizationService authService_;
	
	/**
	 * Method to iniate REST resources authorizations.
	 */
	@PostConstruct
	public void initAuthorisations() {
		authService_.setAutorities("/api/plugins@GET",new RoleAuthority("PUBLIC"),new RoleAuthority("USER"),new RoleAuthority("ADMIN"));
	}

	@ResourceAccess
	@RequestMapping(value = "/plugins", method = RequestMethod.GET)
	public @ResponseBody List<Plugin> getPlugins() {
		List<Plugin> plugins = new ArrayList<>();
		plugins.add(new Plugin("HelloWorldPlugin", "HelloWorldPlugin/hw.js"));
		return plugins;
	}

}
