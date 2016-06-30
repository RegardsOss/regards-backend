package fr.cs.regards.controlers;

import java.util.ArrayList;
import java.util.List;

import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import fr.cs.regards.pojo.Plugin;

@RestController
// Indicates that those resources are securised. Only the /oauth endpoint do not
// need the authentication token
@EnableResourceServer
@RequestMapping("/api")
public class PluginController {

	@RequestMapping(value = "plugins", method = RequestMethod.GET)
	public @ResponseBody List<Plugin> getPlugins() {
		List<Plugin> plugins = new ArrayList<>();
		plugins.add(new Plugin("HelloWorldPlugin", "HelloWorldPlugin/hw.js"));
		return plugins;
	}

}
