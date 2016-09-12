package fr.cnes.regards.modules.access.rest;

import java.util.List;

import javax.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import fr.cnes.regards.microservices.core.auth.MethodAutorizationService;
import fr.cnes.regards.microservices.core.auth.ResourceAccess;
import fr.cnes.regards.microservices.core.auth.RoleAuthority;
import fr.cnes.regards.microservices.core.information.ModuleInfo;
import fr.cnes.regards.modules.access.domain.NavigationContext;
import fr.cnes.regards.modules.access.service.NavigationContextServiceStub;

/**
 *
 * REST controller for the microservice Access
 *
 */
@RestController
@ModuleInfo(name = "navigation context", version = "1.0-SNAPSHOT", author = "REGARDS", legalOwner = "CS", documentation = "http://test")
@RequestMapping("/tiny")
public class NavigationContextController {

    @Autowired
    MethodAutorizationService authService_;

    @Autowired
    NavigationContextServiceStub service_;

    /**
     * Method to initiate REST resources authorizations.
     */
    @PostConstruct
    public void initAuthorisations() {
        authService_.setAutorities("/tiny/url/{pTinyUrl}@GET", new RoleAuthority("USER"));
        authService_.setAutorities("/tiny/url/{pTinyUrl}@PUT", new RoleAuthority("USER"));
        authService_.setAutorities("/tiny/url/{pTinyUrl}@DELETE", new RoleAuthority("USER"));
        authService_.setAutorities("/tiny/urls@GET", new RoleAuthority("USER"));
        authService_.setAutorities("/tiny/urls@POST", new RoleAuthority("USER"));
    }

    //    @GetMapping(value = "/url/{tinyUrl}", produces = "application/json")
    //    @ResourceAccess
    //    public @ResponseBody HttpEntity<NavigationContext> load(@PathVariable("tinyUrl") String pTinyUrl) {
    //        NavigationContext navigationContext = service_.load(pTinyUrl);
    //        return new ResponseEntity<>(navigationContext, HttpStatus.OK);
    //    }
    //
    //    @PutMapping(value = "/url/{tinyUrl}", produces = "application/json")
    //    @ResourceAccess
    //    public @ResponseBody HttpEntity<Void> update(@PathVariable("tinyUrl") String pTinyUrl,
    //            @RequestBody NavigationContext pNavigationContext) throws OperationNotSupportedException {
    //        service_.update(pTinyUrl, pNavigationContext);
    //        return new ResponseEntity<>(HttpStatus.OK);
    //    }

    @RequestMapping(value = "/urls", method = RequestMethod.GET, produces = "application/json")
    @ResourceAccess
    public @ResponseBody HttpEntity<List<NavigationContext>> List() {
        List<NavigationContext> navigationContexts = service_.list();
        // addLinksToProjects(projects);
        return new ResponseEntity<>(navigationContexts, HttpStatus.OK);
    }

    // void update(String pTinyUrl);
    // void delete(String pTinyUrl);
    // NavigationContext create(NavigationContext pNavigationContext);

}
