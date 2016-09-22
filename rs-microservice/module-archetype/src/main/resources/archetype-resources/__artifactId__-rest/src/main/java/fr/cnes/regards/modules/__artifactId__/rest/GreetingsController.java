/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.${artifactId}.rest;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import fr.cnes.modules.core.hateoas.HateoasDTO;
import fr.cnes.regards.microservices.core.annotation.ModuleInfo;
import fr.cnes.regards.microservices.core.security.endpoint.annotation.ResourceAccess;
import fr.cnes.regards.modules.${artifactId}.domain.Greeting;
import fr.cnes.regards.modules.${artifactId}.service.GreetingsService;

/**
 * REST module controller
 * 
 * TODO Description
 * @author TODO
 *
 */
@RestController
@ModuleInfo(name="${artifactId}", version="${version}", author="REGARDS", legalOwner="CS", documentation="http://test")
@RequestMapping("/api")
public class GreetingsController {

    @Autowired
    GreetingsService myService_;

    @RequestMapping(value = "/greeting", method = RequestMethod.GET)
    @ResourceAccess(description = "send 'greeting' as response")
    public ResponseEntity<HateoasDTO<Greeting>> greeting(@RequestParam(value = "name", defaultValue = "World") String pName) {
        Greeting greeting = myService_.getGreeting(pName);
        return new ResponseEntity<>(new HateoasDTO<>(greeting), HttpStatus.OK);
    }

    @RequestMapping(value = "/me", method = RequestMethod.GET)
    @ResourceAccess(description = "send 'me' as response")
    public ResponseEntity<HateoasDTO<Greeting>> me(@RequestParam(value = "name", defaultValue = "me") String pName) {
        Greeting greeting = myService_.getGreeting(pName);
        return new ResponseEntity<>(new HateoasDTO<>(greeting), HttpStatus.OK);
    }
}
