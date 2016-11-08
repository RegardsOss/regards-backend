/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.rest;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.hateoas.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import fr.cnes.regards.framework.module.annotation.ModuleInfo;
import fr.cnes.regards.framework.security.domain.ResourceMapping;
import fr.cnes.regards.modules.accessrights.domain.projects.ResourcesAccess;
import fr.cnes.regards.modules.accessrights.service.IResourcesService;
import fr.cnes.regards.modules.accessrights.signature.IResourcesSignature;

/**
 *
 * Class ResourcesController
 *
 * Rest controller to access ResourcesAccess entities
 *
 * @author CS
 * @since 1.0-SNASHOT
 */
@RestController
@ModuleInfo(name = "accessrights", version = "1.0-SNAPSHOT", author = "REGARDS", legalOwner = "CS",
        documentation = "http://test")
public class ResourcesController implements IResourcesSignature {

    /**
     * Business service
     */
    private final IResourcesService service;

    public ResourcesController(final IResourcesService pService) {
        super();
        service = pService;
    }

    @Override
    public ResponseEntity<List<ResourceMapping>> collectResources() {
        return new ResponseEntity<>(service.collectResources(), HttpStatus.OK);
    }

    @Override
    public ResponseEntity<List<Resource<ResourcesAccess>>> getResourceAccessList() {
        final List<ResourcesAccess> resourcesAccess = service.retrieveRessources();
        final List<Resource<ResourcesAccess>> resources = resourcesAccess.stream().map(p -> new Resource<>(p))
                .collect(Collectors.toList());
        return new ResponseEntity<>(resources, HttpStatus.OK);
    }

}
