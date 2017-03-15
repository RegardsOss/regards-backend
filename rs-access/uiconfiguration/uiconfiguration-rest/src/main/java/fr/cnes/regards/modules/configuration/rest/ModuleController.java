/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.configuration.rest;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import fr.cnes.regards.framework.hateoas.IResourceController;
import fr.cnes.regards.framework.module.annotation.ModuleInfo;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.modules.configuration.domain.Module;

/**
 * REST controller for the microservice Access
 *
 * @author Sébastien Binda
 *
 */
@RestController
@ModuleInfo(name = "Module", version = "1.0-SNAPSHOT", author = "REGARDS", legalOwner = "CS",
        documentation = "http://test")
@RequestMapping("/applications/{applicationId}/modules")
public class ModuleController implements IResourceController<Module> {

    /**
     * Entry point to retrieve a {@link Layout}
     *
     * @return {@link Layout}
     * @throws EntityNotFoundException
     */
    @RequestMapping(method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @ResourceAccess(description = "Endpoint to retrieve IHM modules for given application", role = DefaultRole.PUBLIC)
    public HttpEntity<PagedResources<Resource<Module>>> retrieveModules(
            @PathVariable("applicationId") final String pApplicationId, final Pageable pPageable,
            final PagedResourcesAssembler<Module> pAssembler) {

        final Page<Module> modules = null;// = service.retrieveModules(pApplicationId, pPageable);
        final PagedResources<Resource<Module>> resources = toPagedResources(modules, pAssembler);
        return new ResponseEntity<>(resources, HttpStatus.OK);

    }

    @Override
    public Resource<Module> toResource(final Module pElement, final Object... pExtras) {
        return null;
    }

}
