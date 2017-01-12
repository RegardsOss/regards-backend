/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.storage.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.Resource;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import fr.cnes.regards.framework.hateoas.IResourceController;
import fr.cnes.regards.framework.hateoas.IResourceService;
import fr.cnes.regards.framework.module.annotation.ModuleInfo;
import fr.cnes.regards.modules.storage.domain.AIP;
//import fr.cnes.regards.modules.storage.domain.Greeting;
import fr.cnes.regards.modules.storage.service.GreetingsService;

/**
 * REST module controller
 *
 * TODO Description
 *
 * @author TODO
 *
 */
@RestController
@ModuleInfo(name = "storage", version = "1.0-SNAPSHOT", author = "REGARDS", legalOwner = "CS",
        documentation = "http://test")
@RequestMapping("/api")
public class AIPController implements IResourceController<AIP> {

    @Autowired
    private IResourceService resourceService;

    @Autowired
    private GreetingsService myService;

    // @RequestMapping(value = "/greeting", method = RequestMethod.GET)
    // @ResponseBody
    // @ResourceAccess(description = "send 'greeting' as response")
    // public HttpEntity<Resource<AIP>> greeting(@RequestParam(value = "name", defaultValue = "World") String pName) {
    // Greeting greeting = myService.getGreeting(pName);
    // return new ResponseEntity<>(new Resource<Greeting>(greeting), HttpStatus.OK);
    // }

    // @RequestMapping(value = "/me", method = RequestMethod.GET)
    // @ResponseBody
    // @ResourceAccess(description = "send 'me' as response")
    // public HttpEntity<Resource<AIP>> me(@RequestParam(value = "name", defaultValue = "me") String pName) {
    // Greeting greeting = myService.getGreeting(pName);
    // return new ResponseEntity<>(new Resource<Greeting>(greeting), HttpStatus.OK);
    // }

    @Override
    public Resource<AIP> toResource(AIP pElement, Object... pExtras) {
        // TODO add hateoas links
        return resourceService.toResource(pElement);
    }
}
