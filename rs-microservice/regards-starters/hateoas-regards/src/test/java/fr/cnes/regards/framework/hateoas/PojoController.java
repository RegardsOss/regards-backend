/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.hateoas;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import fr.cnes.regards.framework.security.annotation.ResourceAccess;

/**
 *
 * POJO controller
 *
 * @author msordi
 *
 */
@RestController
@RequestMapping("/pojos")
public class PojoController implements IResourceController<Pojo> {

    /**
     * Resource service
     */
    @Autowired
    private IResourceService resourceService;

    @ResourceAccess(description = "Get all")
    @GetMapping
    public ResponseEntity<List<Resource<Pojo>>> getPojos() {
        final List<Pojo> pojos = new ArrayList<>();
        pojos.add(new Pojo(1L, "first"));
        pojos.add(new Pojo(2L, "second"));
        return ResponseEntity.ok(toResources(pojos));
    }

    @ResourceAccess(description = "Get one")
    @GetMapping("/{pPojoId}")
    public ResponseEntity<Resource<Pojo>> getPojo(@PathVariable Long pPojoId) {
        return ResponseEntity.ok(toResource(new Pojo(pPojoId, "dynamic")));
    }

    @Override
    public Resource<Pojo> toResource(Pojo pElement) {
        final Resource<Pojo> resource = resourceService.toResource(pElement);
        resourceService.addLink(resource, this.getClass(), "getAttribute", LinkRels.SELF,
                                MethodParamFactory.build(Long.class, pElement.getId()));
        return resource;
    }
}
