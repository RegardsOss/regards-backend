package fr.cnes.regards.microservices.backend.controllers.common;

import fr.cnes.regards.microservices.backend.pojo.common.Plugin;
import fr.cnes.regards.microservices.backend.pojo.common.PluginParameter;
import fr.cnes.regards.microservices.core.auth.MethodAutorizationService;
import fr.cnes.regards.microservices.core.auth.ResourceAccess;
import fr.cnes.regards.microservices.core.auth.RoleAuthority;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;
import org.springframework.web.bind.annotation.*;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;

@RestController
// Indicates that those resources are securised. Only the /oauth endpoint do not
// need the authentication token
@EnableResourceServer
@RequestMapping("/api")
public class PluginController {

    @Autowired
    MethodAutorizationService authService_;

    /**
     * Method to initiate REST resources authorizations.
     */
    @PostConstruct
    public void initAuthorisations() {
        authService_.setAutorities("/api/plugins@GET", new RoleAuthority("ADMIN"));
        authService_.setAutorities("/api/plugins/{plugin_name}@GET", new RoleAuthority("ADMIN"));
        authService_.setAutorities("/api/plugins/{plugin_name}/config@GET", new RoleAuthority("ADMIN"));
    }

    @ResourceAccess
    @RequestMapping(value = "/plugins/", method = RequestMethod.GET)
    public @ResponseBody List<Plugin> getPlugins() {
        List<Plugin> plugins = new ArrayList<>();
        return plugins;
    }

    @ResourceAccess
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

    @ResourceAccess
    @RequestMapping(value = "/plugins/{plugin_name}/config", method = RequestMethod.GET)
    public @ResponseBody List<PluginParameter> getPluginConfig(@PathVariable("plugin_name") String name) {
        List<String> description = new ArrayList<>();
        // TODO
        return null;
    }

}
