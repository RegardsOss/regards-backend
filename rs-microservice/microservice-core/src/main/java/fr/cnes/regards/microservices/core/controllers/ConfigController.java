/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.microservices.core.controllers;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import fr.cnes.regards.microservices.core.security.endpoint.MethodAutorizationService;
import fr.cnes.regards.microservices.core.security.endpoint.RoleAuthority;
import fr.cnes.regards.microservices.core.security.endpoint.annotation.ResourceAccess;

@RestController
@RequestMapping("/config")
public class ConfigController {

    /**
     * Is the Config server enabled
     */
    @Value("${cloud.config.server.enabled}")
    Boolean configServerEnabled = false;

    /**
     * Property to read from the config server
     */
    @Value("${my.otherproperty}")
    String name = "Default value";

    @Autowired
    MethodAutorizationService authService;

    @PostConstruct
    public void initAuthorisations() {
        authService.setAutorities("/config/value@GET", new RoleAuthority("ADMIN"));
    }

    @ResourceAccess(name = "config", description = "FIXME")
    @RequestMapping(value = "/value", method = RequestMethod.GET)
    public String getConfigValue() {
        if (configServerEnabled) {
            return name;
        }
        else {
            return "Config server disabled !";
        }
    }

}
