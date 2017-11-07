package fr.cnes.regards.modules.storage.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import fr.cnes.regards.framework.hateoas.IResourceController;
import fr.cnes.regards.framework.hateoas.IResourceService;
import fr.cnes.regards.framework.hateoas.LinkRels;
import fr.cnes.regards.framework.hateoas.MethodParamFactory;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.modules.storage.domain.database.AIPEntity;
import fr.cnes.regards.modules.storage.service.database.IAIPEntityService;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
@RestController(value = AIPEntityController.BASE_PATH)
public class AIPEntityController implements IResourceController<AIPEntity> {

    public static final String BASE_PATH = "sips/{sip_id}/aips";

    @Autowired
    private IResourceService resourceService;

    @Autowired
    private IAIPEntityService aipEntityService;

    @ResponseBody
    @ResourceAccess(description = "send pages of AIPEntity")
    @RequestMapping(method = RequestMethod.GET)
    public ResponseEntity<PagedResources<Resource<AIPEntity>>> retrieveAIPEntities(
            @PathVariable("sip_id") String sipId, Pageable pageable,
            PagedResourcesAssembler<AIPEntity> pagedResourcesAssembler) {
        return new ResponseEntity<>(toPagedResources(aipEntityService.retrieveBySip(sipId, pageable),
                                                     pagedResourcesAssembler), HttpStatus.OK);
    }

    @Override
    public Resource<AIPEntity> toResource(AIPEntity pElement, Object... pExtras) {
        Resource<AIPEntity> resource = new Resource<>(pElement);
        resourceService.addLink(resource,
                                this.getClass(),
                                "retrieveAIPEntities",
                                LinkRels.LIST,
                                MethodParamFactory.build(String.class));
        return resource;
    }
}
