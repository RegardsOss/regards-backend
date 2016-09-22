package fr.cnes.regards.microservices.backend.controllers.common;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import fr.cnes.regards.microservices.backend.pojo.common.Plugin;
import fr.cnes.regards.microservices.backend.pojo.common.PluginParameter;
import fr.cnes.regards.microservices.core.annotation.ModuleInfo;
import fr.cnes.regards.microservices.core.security.endpoint.MethodAutorizationService;
import fr.cnes.regards.microservices.core.security.endpoint.annotation.ResourceAccess;

@RestController
@ModuleInfo(name = "plugin controller", version = "1.0-SNAPSHOT", author = "REGARDS", legalOwner = "CS", documentation = "http://test")
@RequestMapping("/api")
public class PluginController {

    @Autowired
    MethodAutorizationService authService_;

//    /**
//     * Method to initiate REST resources authorizations.
//     */
//    @PostConstruct
//    public void initAuthorisations() {
//        authService_.setAutorities("/api/plugins@GET", new RoleAuthority("ADMIN"));
//        authService_.setAutorities("/api/plugins/{plugin_name}@GET", new RoleAuthority("ADMIN"));
//        authService_.setAutorities("/api/plugins/{plugin_name}/config@GET", new RoleAuthority("ADMIN"));
//    }

    @ResourceAccess(description = "")
    @RequestMapping(value = "/plugins/", method = RequestMethod.GET)
    public @ResponseBody List<Plugin> getPlugins() {
        List<Plugin> plugins = new ArrayList<>();
        return plugins;
    }

    @ResourceAccess(description = "")
    @RequestMapping(value = "/plugins/{plugin_name}", method = RequestMethod.GET)
    public @ResponseBody Plugin getPlugin(@PathVariable("plugin_name") String name) {
        List<PluginParameter> pluginParameters = new ArrayList<>();
        pluginParameters.add(new PluginParameter(new ArrayList<Object>(), false, "plugin.connection.username", ""));
        pluginParameters.add(new PluginParameter(new ArrayList<Object>(), false, "plugin.connection.password", ""));
        pluginParameters.add(new PluginParameter(new ArrayList<Object>(), false, "plugin.connection.url", ""));
        pluginParameters.add(new PluginParameter(new ArrayList<Object>(), false, "plugin.connection.port", ""));
        Plugin plugin = new Plugin(15L, name, pluginParameters);
        return plugin;
    }

    @ResourceAccess(description = "")
    @RequestMapping(value = "/plugins/{plugin_name}/config", method = RequestMethod.GET)
    public @ResponseBody List<PluginParameter> getPluginConfig(@PathVariable("plugin_name") String name) {
        List<String> description = new ArrayList<>();
        // TODO
        return null;
    }

}
