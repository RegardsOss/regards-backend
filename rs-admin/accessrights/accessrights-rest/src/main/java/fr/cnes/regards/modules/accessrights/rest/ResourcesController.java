/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.rest;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import fr.cnes.regards.framework.hateoas.IResourceController;
import fr.cnes.regards.framework.hateoas.IResourceService;
import fr.cnes.regards.framework.hateoas.LinkRels;
import fr.cnes.regards.framework.hateoas.MethodParamFactory;
import fr.cnes.regards.framework.module.annotation.ModuleInfo;
import fr.cnes.regards.framework.security.domain.ResourceMapping;
import fr.cnes.regards.modules.accessrights.domain.projects.ResourcesAccess;
import fr.cnes.regards.modules.accessrights.service.resources.IResourcesService;
import fr.cnes.regards.modules.accessrights.signature.IResourcesSignature;

/**
 *
 * Class ResourcesController
 *
 * Rest controller to access ResourcesAccess entities
 *
 * @author Sébastien Binda
 * @since 1.0-SNASHOT
 */
@RestController
@ModuleInfo(name = "accessrights", version = "1.0-SNAPSHOT", author = "REGARDS", legalOwner = "CS",
        documentation = "http://test")
public class ResourcesController implements IResourcesSignature, IResourceController<ResourcesAccess> {

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(ResourcesController.class);

    /**
     * Business service
     */
    private final IResourcesService service;

    /**
     * Resource service to manage visibles hateoas links
     */
    private final IResourceService hateoasService;

    public ResourcesController(final IResourcesService pService, final IResourceService pHateoasService) {
        super();
        service = pService;
        hateoasService = pHateoasService;
    }

    @Override
    public ResponseEntity<List<Resource<ResourcesAccess>>> retrieveResourcesAccesses() {
        final List<ResourcesAccess> resourcesAccess = service.retrieveRessources();
        final List<Resource<ResourcesAccess>> resources = resourcesAccess.stream().map(p -> new Resource<>(p))
                .collect(Collectors.toList());
        return new ResponseEntity<>(resources, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<List<Resource<ResourcesAccess>>> registerMicroserviceEndpoints(
            @PathVariable("microservicename") final String pMicroserviceName,
            @RequestBody final List<ResourceMapping> pResourcesToRegister) {
        // TODO Auto-generated method stub
        return new ResponseEntity<>(toResources(new ArrayList<>()), HttpStatus.OK);
    }

    @Override
    public ResponseEntity<Resource<ResourcesAccess>> updateResourceAccess(final Long pResourceId,
            final ResourcesAccess pResourceAccessToUpdate) {
        // TODO Auto-generated method stub
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);
    }

    @Override
    public Resource<ResourcesAccess> toResource(final ResourcesAccess pElement, final Object... pExtras) {
        Resource<ResourcesAccess> resource = null;
        if ((pElement != null) && (pElement.getId() != null)) {
            resource = hateoasService.toResource(pElement);
            hateoasService.addLink(resource, this.getClass(), "updateResourceAccess", LinkRels.UPDATE,
                                   MethodParamFactory.build(Long.class, pElement.getId()));
        } else {
            LOG.warn(String.format("Invalid %s entity. Cannot create hateoas resources", this.getClass().getName()));
        }
        return resource;
    }

}
