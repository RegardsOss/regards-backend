package fr.cnes.regards.microservices.backend.controllers.common;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import fr.cnes.regards.microservices.backend.pojo.common.Plugin;
import fr.cnes.regards.microservices.core.annotation.ModuleInfo;
import fr.cnes.regards.microservices.core.security.endpoint.MethodAutorizationService;
import fr.cnes.regards.microservices.core.security.endpoint.annotation.ResourceAccess;

@RestController
@ModuleInfo(name = "plugin types controller", version = "1.0-SNAPSHOT", author = "REGARDS", legalOwner = "CS", documentation = "http://test")
@RequestMapping("/api")
public class PluginTypesController {

    @Autowired
    MethodAutorizationService authService_;

//    /**
//     * Method to initiate REST resources authorizations.
//     */
//    @PostConstruct
//    public void initAuthorisations() {
//        authService_.setAutorities("/api/pluginstypes/{plugin_type}@GET", new RoleAuthority("PUBLIC"), new RoleAuthority("USER"), new RoleAuthority("ADMIN"));
//    }

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
