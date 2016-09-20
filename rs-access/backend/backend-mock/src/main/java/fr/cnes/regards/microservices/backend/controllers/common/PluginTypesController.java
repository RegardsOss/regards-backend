package fr.cnes.regards.microservices.backend.controllers.common;

import fr.cnes.regards.microservices.backend.pojo.common.Plugin;
import fr.cnes.regards.microservices.core.auth.MethodAutorizationService;
import fr.cnes.regards.microservices.core.auth.ResourceAccess;
import fr.cnes.regards.microservices.core.auth.RoleAuthority;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;
import org.springframework.web.bind.annotation.*;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

@RestController
// Indicates that those resources are securised. Only the /oauth endpoint do not
// need the authentication token
@EnableResourceServer
@RequestMapping("/api")
public class PluginTypesController {

    @Autowired
    MethodAutorizationService authService_;

    /**
     * Method to initiate REST resources authorizations.
     */
    @PostConstruct
    public void initAuthorisations() {
        authService_.setAutorities("/api/pluginstypes/{plugin_type}@GET", new RoleAuthority("PUBLIC"), new RoleAuthority("USER"), new RoleAuthority("ADMIN"));
    }

    @ResourceAccess(description = "")
    @RequestMapping(value = "/pluginstypes/{plugin_type}", method = RequestMethod.GET)
    public @ResponseBody List<Plugin> getPlugins(@PathVariable("plugin_type") String type) {
        List<Plugin> plugins = new ArrayList<>();
        AtomicLong counter = new AtomicLong();
        String[] types = {"Mysql", "Oracle", "MongoDB"};
        for (int i = 0; i < types.length; i++) {
            Plugin plugin = new Plugin(counter.getAndIncrement(), types[i], null);
            plugin.add(linkTo(methodOn(PluginController.class).getPlugin(plugin.getLabel())).withSelfRel());
            plugin.add(linkTo(methodOn(PluginController.class).getPluginConfig(plugin.getLabel())).withRel("config"));
            plugins.add(plugin);
        }
        return plugins;
    }

}
