package fr.cnes.regards.microservices.backend.controllers.administration;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import fr.cnes.regards.microservices.backend.pojo.administration.PluginJS;
import fr.cnes.regards.microservices.core.annotation.ModuleInfo;
import fr.cnes.regards.microservices.core.security.endpoint.MethodAutorizationService;
import fr.cnes.regards.microservices.core.security.endpoint.annotation.ResourceAccess;

@RestController
@ModuleInfo(name = "plugin JS controller", version = "1.0-SNAPSHOT", author = "REGARDS", legalOwner = "CS", documentation = "http://test")
@RequestMapping("/api")
public class PluginJSController {

    @Autowired
    MethodAutorizationService authService_;

//    /**
//     * Method to iniate REST resources authorizations.
//     */
//    @PostConstruct
//    public void initAuthorisations() {
//        authService_.setAutorities("/api/access/plugins@GET", new RoleAuthority("PUBLIC"), new RoleAuthority("USER"), new RoleAuthority("ADMIN"));
//    }

    @ResourceAccess(description = "")
    @RequestMapping(value = "/access/plugins", method = RequestMethod.GET)
    public @ResponseBody List<PluginJS> getPlugins() {
        List<PluginJS> plugins = new ArrayList<>();
        plugins.add(new PluginJS("HelloWorldPlugin", "HelloWorldPlugin/hw.js"));
        return plugins;
    }

}
