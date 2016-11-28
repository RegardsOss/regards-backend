/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.access.rest;

import java.util.List;
import java.util.stream.Collectors;

import javax.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import fr.cnes.regards.framework.module.annotation.ModuleInfo;
import fr.cnes.regards.framework.module.rest.exception.EntityAlreadyExistsException;
import fr.cnes.regards.framework.module.rest.exception.EntityException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.modules.access.domain.NavigationContext;
import fr.cnes.regards.modules.access.service.INavigationContextService;

/**
 * REST controller for the microservice Access
 *
 * @author Christophe Mertz
 *
 */
@RestController
@ModuleInfo(name = "navigation context", version = "1.0-SNAPSHOT", author = "REGARDS", legalOwner = "CS",
        documentation = "http://test")
@RequestMapping("/tiny")
public class NavigationContextController {

    /**
     * Class logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(NavigationContextController.class);

    @Autowired
    private INavigationContextService service;

    /**
     * Constructor to specify a particular {@link INavigationContextSignature}.
     * 
     * @param pNavigationContextService
     *            The {@link NavigationContext} used.
     */
    public NavigationContextController(final INavigationContextService pNavigationContextService) {
        super();
        service = pNavigationContextService;
    }

    /**
     * Load a {@link NavigationContext}
     * 
     * @param pNavCtxId
     *            the id of the {@link NavigationContext} to load
     * @return the loaded {@link NavigationContext}
     * @throws EntityNotFoundException
     *             throw if an error occurs
     */
    @RequestMapping(value = "/url/{navCtxId}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    ResponseEntity<Resource<NavigationContext>> load(@PathVariable("navCtxId") Long pNavCtxId)
            throws EntityNotFoundException {
        final NavigationContext navigationContext = service.load(pNavCtxId);
        return new ResponseEntity<>(new Resource<NavigationContext>(navigationContext), HttpStatus.OK);
    }

    /**
     * Update a {@link NavigationContext}.
     * 
     * @param pNavCtxId
     *            the id of the {@link NavigationContext} to load
     * @param pNavigationContext
     *            the {@link NavigationContext} to update
     * @return the updated {@link NavigationContext}
     * @throws EntityNotFoundException
     *             throw if an error occurs
     */
    @RequestMapping(value = "/url/{navCtxId}", method = RequestMethod.PUT, consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    ResponseEntity<Resource<NavigationContext>> update(@PathVariable("navCtxId") Long pNavCtxId,
            @Valid @RequestBody NavigationContext pNavigationContext) throws EntityNotFoundException {
        if (pNavigationContext.getId() != pNavCtxId) {
            LOGGER.error(String.format("invalid context navigation identifier : <%s>", pNavCtxId),
                         NavigationContext.class);
            throw new EntityNotFoundException(pNavCtxId, NavigationContext.class);
        }
        NavigationContext navigationContext = service.update(pNavigationContext);
        final Resource<NavigationContext> resource = new Resource<>(navigationContext);
        return new ResponseEntity<>(resource, HttpStatus.OK);
    }

    /**
     * Delete a {@link NavigationContext}.
     * 
     * @param pNavCtxId
     *            the id of the {@link NavigationContext} to load
     * @return void
     * @throws EntityNotFoundException
     *             throw if an error occurs
     */
    @RequestMapping(value = "/url/{navCtxId}", method = RequestMethod.DELETE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    ResponseEntity<Void> delete(@PathVariable("navCtxId") Long pNavCtxId) throws EntityNotFoundException {
        service.delete(pNavCtxId);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    /**
     * Lists all the {@link} {@link NavigationContext}.
     * 
     * @return a list of {@link NavigationContext}
     */
    @RequestMapping(value = "/urls", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    ResponseEntity<List<Resource<NavigationContext>>> list() {
        final List<NavigationContext> navigationContexts = service.list();
        final List<Resource<NavigationContext>> resources = navigationContexts.stream().map(p -> new Resource<>(p))
                .collect(Collectors.toList());
        // addLinksToProjects(projects);
        return new ResponseEntity<>(resources, HttpStatus.OK);
    }

    /**
     * Create a {@link NavigationContext}
     * 
     * @param pNavigationContext
     *            the {@link NavigationContext} to create
     * @return the created {@link NavigationContext}
     * @throws EntityException 
     */
    @RequestMapping(value = "/urls", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    ResponseEntity<Resource<NavigationContext>> create(@Valid @RequestBody NavigationContext pNavigationContext)
            throws EntityException {
        final NavigationContext navigationContext;
        try {
            navigationContext = service.create(pNavigationContext);
        } catch (ModuleException e) {
            LOGGER.error(e.getMessage(), e);
            throw new EntityException(e.getMessage());
        }
        // addLinksToProjects(projects);
        final Resource<NavigationContext> resource = new Resource<>(navigationContext);
        return new ResponseEntity<>(resource, HttpStatus.CREATED);
    }

}
