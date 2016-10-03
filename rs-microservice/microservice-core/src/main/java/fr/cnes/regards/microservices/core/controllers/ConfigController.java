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

import fr.cnes.regards.microservices.core.security.endpoint.MethodAuthorizationService;
import fr.cnes.regards.security.utils.endpoint.annotation.ResourceAccess;

@RestController
@RequestMapping("/config")
public class ConfigController {

    /**
     * Is the Config server enabled
     */
    @Value("${cloud.config.server.enabled}")
    Boolean configServerEnabled_ = false;

    /**
     * Property to read from the config server
     */
    @Value("${my.otherproperty}")
    String name_ = "Default value";

    @Autowired
    MethodAuthorizationService authService_;

    @PostConstruct
    public void initAuthorisations() {
        authService_.setAuthorities("/config/value", RequestMethod.GET, "ADMIN");
    }

    @ResourceAccess(name = "config", description = "FIXME")
    @RequestMapping(value = "/value", method = RequestMethod.GET)
    public String getConfigValue() {
        if (configServerEnabled_) {
            return name_;
        }
        else {
            return "Config server disabled !";
        }
    }

}
