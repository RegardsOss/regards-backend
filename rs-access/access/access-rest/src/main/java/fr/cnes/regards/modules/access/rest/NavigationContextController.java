/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.access.rest;

import java.util.List;
import java.util.NoSuchElementException;

import javax.naming.OperationNotSupportedException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import fr.cnes.regards.microservices.core.annotation.ModuleInfo;
import fr.cnes.regards.microservices.core.security.endpoint.MethodAutorizationService;
import fr.cnes.regards.microservices.core.security.endpoint.annotation.ResourceAccess;
import fr.cnes.regards.modules.access.domain.NavigationContext;
import fr.cnes.regards.modules.access.service.INavigationContextService;
import fr.cnes.regards.modules.core.exception.AlreadyExistingException;

/**
 * REST controller for the microservice Access
 * 
 * @author cmertz
 *
 */
@RestController
@ModuleInfo(name = "navigation context", version = "1.0-SNAPSHOT", author = "REGARDS", legalOwner = "CS", documentation = "http://test")
@RequestMapping("/tiny")
public class NavigationContextController {

    @Autowired
    MethodAutorizationService authService_;

    @Autowired
    INavigationContextService service_;

    @ExceptionHandler(AlreadyExistingException.class)
    @ResponseStatus(value = HttpStatus.CONFLICT)
    public void dataAlreadyExisting() {
    }

    @ExceptionHandler(OperationNotSupportedException.class)
    @ResponseStatus(value = HttpStatus.METHOD_NOT_ALLOWED)
    public void operationNotSupported() {
    }

    @ExceptionHandler(NoSuchElementException.class)
    @ResponseStatus(value = HttpStatus.METHOD_NOT_ALLOWED)
    public void noSuchElement() {
    }

    @RequestMapping(value = "/url/{tinyUrl}", method = RequestMethod.GET, produces = "application/json")
    @ResourceAccess(description = "Get a navigation contexts")
    public @ResponseBody HttpEntity<Resource<NavigationContext>> load(@PathVariable("tinyUrl") String pTinyUrl) {
        NavigationContext navigationContext = service_.load(pTinyUrl);
        return new ResponseEntity<>(new Resource<NavigationContext>(navigationContext), HttpStatus.OK);

    }

    @RequestMapping(value = "/url/{tinyUrl}", method = RequestMethod.PUT, produces = "application/json", consumes = "application/json")
    @ResourceAccess(description = "Update a navigation context")
    public @ResponseBody HttpEntity<Void> update(@PathVariable("tinyUrl") String pTinyUrl,
            @RequestBody NavigationContext pNavigationContext)
            throws OperationNotSupportedException, NoSuchElementException {
        service_.update(pTinyUrl, pNavigationContext);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @RequestMapping(value = "/url/{tinyUrl}", method = RequestMethod.DELETE, produces = "application/json")
    @ResourceAccess(description = "Delete a navigation context")
    public @ResponseBody HttpEntity<Void> delete(@PathVariable("tinyUrl") String pTinyUrl)
            throws NoSuchElementException {
        service_.delete(pTinyUrl);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @RequestMapping(value = "/urls", method = RequestMethod.GET, produces = "application/json")
    @ResourceAccess(description = "Navigation contexts list")
    public @ResponseBody HttpEntity<List<NavigationContext>> List() {
        List<NavigationContext> navigationContexts = service_.list();
        // addLinksToProjects(projects);
        return new ResponseEntity<>(navigationContexts, HttpStatus.OK);
    }

    @RequestMapping(value = "/urls", method = RequestMethod.POST, produces = "application/json", consumes = "application/json")
    @ResourceAccess(description = "Navigation contexts list")
    public @ResponseBody HttpEntity<NavigationContext> create(@RequestBody NavigationContext pNavigationContext)
            throws AlreadyExistingException {
        NavigationContext navigationContexts = service_.create(pNavigationContext);
        // addLinksToProjects(projects);
        return new ResponseEntity<>(navigationContexts, HttpStatus.OK);
    }

}
