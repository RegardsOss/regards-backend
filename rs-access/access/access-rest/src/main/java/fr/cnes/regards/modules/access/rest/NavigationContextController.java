/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.access.rest;

import java.util.List;
import java.util.stream.Collectors;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import fr.cnes.regards.framework.module.annotation.ModuleInfo;
import fr.cnes.regards.framework.module.rest.exception.AlreadyExistingException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.modules.access.domain.NavigationContext;
import fr.cnes.regards.modules.access.service.INavigationContextService;
import fr.cnes.regards.modules.access.signature.INavigationContextSignature;

/**
 * REST controller for the microservice Access
 *
 * @author Christophe Mertz
 *
 */
@RestController
@ModuleInfo(name = "navigation context", version = "1.0-SNAPSHOT", author = "REGARDS", legalOwner = "CS",
        documentation = "http://test")
public class NavigationContextController implements INavigationContextSignature {

    @Autowired
    INavigationContextService service;

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

    @Override
    public ResponseEntity<Resource<NavigationContext>> load(@PathVariable("navCtxId") Long pNavCtxId)
            throws EntityNotFoundException {
        NavigationContext navigationContext = service.load(pNavCtxId);
        return new ResponseEntity<>(new Resource<NavigationContext>(navigationContext), HttpStatus.OK);
    }

    @Override
    public ResponseEntity<Resource<NavigationContext>> update(@PathVariable("navCtxId") Long pNavCtxId,
            @Valid @RequestBody NavigationContext pNavigationContext) throws EntityNotFoundException {
        if (pNavigationContext.getId() != pNavCtxId) {
            throw new EntityNotFoundException(String.format("invalid context navigation identifier : <%l>", pNavCtxId),
                    NavigationContext.class);
        }
        service.update(pNavigationContext);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Override
    public ResponseEntity<Void> delete(@PathVariable("navCtxId") Long pNavCtxId) throws EntityNotFoundException {
        service.delete(pNavCtxId);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Override
    public ResponseEntity<List<Resource<NavigationContext>>> list() {
        List<NavigationContext> navigationContexts = service.list();
        final List<Resource<NavigationContext>> resources = navigationContexts.stream().map(p -> new Resource<>(p))
                .collect(Collectors.toList());
        // addLinksToProjects(projects);
        return new ResponseEntity<>(resources, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<Resource<NavigationContext>> create(@Valid @RequestBody NavigationContext pNavigationContext)
            throws AlreadyExistingException {
        NavigationContext navigationContext = service.create(pNavigationContext);
        // addLinksToProjects(projects);
        final Resource<NavigationContext> resource = new Resource<>(navigationContext);
        return new ResponseEntity<>(resource, HttpStatus.CREATED);
    }

}
