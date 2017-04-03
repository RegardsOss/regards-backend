/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.search.rest.assembler;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.HateoasPageableHandlerMethodArgumentResolver;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.stereotype.Component;

import fr.cnes.regards.framework.hateoas.IResourceService;
import fr.cnes.regards.framework.hateoas.LinkRels;
import fr.cnes.regards.framework.hateoas.MethodParamFactory;
import fr.cnes.regards.modules.entities.domain.Dataset;
import fr.cnes.regards.modules.entities.urn.UniformResourceName;
import fr.cnes.regards.modules.search.rest.CatalogController;

/**
 * Custom {@link PagedResourcesAssembler} for {@link Dataset}s.
 * @author Xavier-Alexandre Brochard
 */
@Component
public class DatasetResourcesAssembler extends PagedResourcesAssembler<Dataset> {

    /**
     * Logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(DatasetResourcesAssembler.class);

    /**
     * The resource service. Autowired by Spring.
     */
    private final IResourceService resourceService;

    /**
     * @param pResolver stuff required by hateoas
     * @param pResourceService handles method authorizations before actually adding a link
     */
    public DatasetResourcesAssembler(@Autowired HateoasPageableHandlerMethodArgumentResolver pResolver,
            IResourceService pResourceService) {
        super(pResolver, null);
        resourceService = pResourceService;
    }

    @Override
    public PagedResources<Resource<Dataset>> toResource(Page<Dataset> pElements) {
        PagedResources<Resource<Dataset>> pagedResources = super.toResource(pElements);
        pagedResources.forEach(resource -> addLinks(resource));
        return pagedResources;
    }

    /**
     * Add links to the passed resource
     * @param pResource
     * @return the resource augmented with links
     */
    private Resource<Dataset> addLinks(Resource<Dataset> pResource) {
        UniformResourceName ipId = pResource.getContent().getIpId();

        resourceService.addLink(pResource, CatalogController.class, "getDataset", LinkRels.SELF,
                                MethodParamFactory.build(UniformResourceName.class, pResource.getContent().getIpId()));

        try {
            String asString = URLEncoder.encode(ipId.toString(), StandardCharsets.UTF_8.toString());
            // Use special way of adding links because we need to have the request param in the templated link
            resourceService.addLinkWithParams(pResource, CatalogController.class, "searchDataobjects", LinkRels.NEXT,
                                              MethodParamFactory.build(String.class, "tags:" + asString),
                                              MethodParamFactory.build(Map.class), //
                                              MethodParamFactory.build(Pageable.class));
        } catch (UnsupportedEncodingException e) {
            LOGGER.info("Could not encode the URN" + ipId + "of the ressource", e);
        }

        return pResource;
    }

}
