package fr.cnes.regards.microservices.backend.controllers.administration;

import fr.cnes.regards.microservices.backend.pojo.administration.PluginJS;
import fr.cnes.regards.microservices.core.auth.MethodAutorizationService;
import fr.cnes.regards.microservices.core.auth.ResourceAccess;
import fr.cnes.regards.microservices.core.auth.RoleAuthority;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;

@RestController
// Indicates that those resources are securised. Only the /oauth endpoint do not
// need the authentication token
@EnableResourceServer
@RequestMapping("/api")
public class PluginJSController {

    @Autowired
    MethodAutorizationService authService_;

    /**
     * Method to iniate REST resources authorizations.
     */
    @PostConstruct
    public void initAuthorisations() {
        authService_.setAutorities("/api/access/plugins@GET", new RoleAuthority("PUBLIC"), new RoleAuthority("USER"), new RoleAuthority("ADMIN"));
    }

    @ResourceAccess(description = "")
    @RequestMapping(value = "/access/plugins", method = RequestMethod.GET)
    public @ResponseBody List<PluginJS> getPlugins() {
        List<PluginJS> plugins = new ArrayList<>();
        plugins.add(new PluginJS("HelloWorldPlugin", "HelloWorldPlugin/hw.js"));
        return plugins;
    }

}
