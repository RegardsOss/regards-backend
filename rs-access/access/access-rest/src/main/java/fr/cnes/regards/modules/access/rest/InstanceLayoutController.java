/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.access.rest;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import fr.cnes.regards.framework.hateoas.IResourceController;
import fr.cnes.regards.framework.hateoas.IResourceService;
import fr.cnes.regards.framework.hateoas.LinkRels;
import fr.cnes.regards.framework.hateoas.MethodParamFactory;
import fr.cnes.regards.framework.module.annotation.ModuleInfo;
import fr.cnes.regards.framework.module.rest.exception.EntityException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.modules.access.domain.instance.InstanceLayout;
import fr.cnes.regards.modules.access.domain.project.Layout;
import fr.cnes.regards.modules.access.service.IInstanceLayoutService;

/**
 * REST controller for the microservice Access
 *
 * @author Sébastien Binda
 *
 */
@RestController
@ModuleInfo(name = "Layout", version = "1.0-SNAPSHOT", author = "REGARDS", legalOwner = "CS",
        documentation = "http://test")
@RequestMapping("/instance/layouts")
public class InstanceLayoutController implements IResourceController<InstanceLayout> {

    @Autowired
    IInstanceLayoutService layoutService;

    @Autowired
    private IResourceService resourceService;

    /**
     * Entry point to retrieve a {@link Layout}
     *
     * @return {@link Layout}
     * @throws EntityNotFoundException
     */
    @RequestMapping(value = "/{applicationId}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @ResourceAccess(description = "Endpoint to retrieve IHM layout configuration for the given applicationId",
            role = DefaultRole.PUBLIC)
    public HttpEntity<Resource<InstanceLayout>> retrieveLayout(
            @PathVariable("applicationId") final String pApplicationId) throws EntityNotFoundException {
        final InstanceLayout layout = layoutService.retrieveLayout(pApplicationId);
        final Resource<InstanceLayout> resource = toResource(layout);
        return new ResponseEntity<>(resource, HttpStatus.OK);
    }

    /**
     * Entry point to update {@link Layout}
     *
     * @return updated {@link Layout}
     * @throws EntityNotFoundException
     */
    @RequestMapping(value = "/{applicationId}", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @ResourceAccess(description = "Endpoint to retrieve IHM layout configuration for the given applicationId",
            role = DefaultRole.INSTANCE_ADMIN)
    public HttpEntity<Resource<InstanceLayout>> updateLayout(@PathVariable("applicationId") final String pApplicationId,
            @Valid @RequestBody final InstanceLayout pLayout) throws EntityException {
        final InstanceLayout layout = layoutService.updateLayout(pLayout);
        final Resource<InstanceLayout> resource = toResource(layout);
        return new ResponseEntity<>(resource, HttpStatus.OK);
    }

    @Override
    public Resource<InstanceLayout> toResource(final InstanceLayout pElement, final Object... pExtras) {
        final Resource<InstanceLayout> resource = resourceService.toResource(pElement);
        resourceService.addLink(resource, this.getClass(), "retrieveLayout", LinkRels.SELF,
                                MethodParamFactory.build(String.class, pElement.getApplicationId()));
        resourceService.addLink(resource, this.getClass(), "updateLayout", LinkRels.UPDATE);
        return resource;
    }

}
