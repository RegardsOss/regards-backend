/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.search.rest.assembler.link;

import java.util.Map;

import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.Resource;
import org.springframework.stereotype.Component;

import fr.cnes.regards.framework.hateoas.IResourceService;
import fr.cnes.regards.framework.hateoas.LinkRels;
import fr.cnes.regards.framework.hateoas.MethodParamFactory;
import fr.cnes.regards.modules.entities.domain.Dataset;
import fr.cnes.regards.modules.entities.urn.UniformResourceName;
import fr.cnes.regards.modules.search.rest.CatalogController;
import fr.cnes.regards.modules.search.rest.assembler.ILinksAdder;

/**
 * Adds custom HATEOAS links to a {@link Dataset} resource.
 * @author Xavier-Alexandre Brochard
 */
@Component
public class DatasetLinkAdder implements ILinksAdder {

    /**
     * The resource service. Autowired by Spring.
     */
    private final IResourceService resourceService;

    /**
     * Constructor
     * @param pResourceService handles method authorizations before actually adding a link
     */
    public DatasetLinkAdder(IResourceService pResourceService) {
        super();
        resourceService = pResourceService;
    }

    /* (non-Javadoc)
     * @see fr.cnes.regards.modules.search.rest.assembler.ILinksAdder#addLinks(org.springframework.hateoas.Resource)
     */
    @Override
    public Resource<Dataset> addLinks(Resource<Dataset> pResource) {
        UniformResourceName ipId = pResource.getContent().getIpId();

        resourceService.addLink(pResource, CatalogController.class, "getDataset", LinkRels.SELF,
                                MethodParamFactory.build(UniformResourceName.class, pResource.getContent().getIpId()));

        resourceService.addLinkWithParams(pResource, CatalogController.class, "searchDataobjects", LinkRels.NEXT,
                                          MethodParamFactory.build(String.class, "tags:" + ipId.toString()),
                                          MethodParamFactory.build(Map.class), //
                                          MethodParamFactory.build(Pageable.class));

        return pResource;
    }

}
