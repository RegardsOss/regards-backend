import fr.cnes.regards.framework.hateoas.IResourceController;
import fr.cnes.regards.framework.hateoas.IResourceService;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;{package}.service.GreetingsService;

/**
 * REST module controller
 * <p>
 * TODO Description
 *
 * @author TODO
 */
@RestController
@RequestMapping("/api")
public class GreetingsController implements IResourceController<Greeting> {

    @Autowired
    private IResourceService resourceService;

    @Autowired
    private GreetingsService myService;

    @GetMapping(path = "/greeting")
    @ResponseBody
    @ResourceAccess(description = "send 'greeting' as response")
    public HttpEntity<Resource<Greeting>> greeting(@RequestParam(value = "name", defaultValue = "World") String pName) {
        Greeting greeting = myService.getGreeting(pName);
        return new ResponseEntity<>(new Resource<Greeting>(greeting), HttpStatus.OK);
    }

    @GetMapping(path = "/me")
    @ResponseBody
    @ResourceAccess(description = "send 'me' as response")
    public HttpEntity<Resource<Greeting>> me(@RequestParam(value = "name", defaultValue = "me") String pName) {
        Greeting greeting = myService.getGreeting(pName);
        return new ResponseEntity<>(new Resource<Greeting>(greeting), HttpStatus.OK);
    }

    @Override
    public Resource<Greeting> toResource(Greeting pElement, Object... pExtras) {
        // TODO add hateoas links
        return resourceService.toResource(pElement);
    }
}
