/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.search.rest.assembler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.web.HateoasPageableHandlerMethodArgumentResolver;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.stereotype.Component;

import fr.cnes.regards.modules.entities.domain.Dataset;
import fr.cnes.regards.modules.search.rest.assembler.link.DatasetLinkAdder;

/**
 * Custom {@link PagedResourcesAssembler} for {@link Dataset}s.
 * @author Xavier-Alexandre Brochard
 */
@Component
public class PagedDatasetResourcesAssembler extends PagedResourcesAssembler<Dataset> {

    /**
     * Takes a {@link Dataset} resource and adds the HATEOAS links to it
     */
    private final DatasetLinkAdder datasetLinkAdder;

    /**
     * @param pResolver stuff required by hateoas
     * @param pDatasetLinkAdder Takes a {@link Dataset} resource and adds the HATEOAS links to it
     */
    public PagedDatasetResourcesAssembler(@Autowired HateoasPageableHandlerMethodArgumentResolver pResolver,
            DatasetLinkAdder pDatasetLinkAdder) {
        super(pResolver, null);
        datasetLinkAdder = pDatasetLinkAdder;
    }

    @Override
    public PagedResources<Resource<Dataset>> toResource(Page<Dataset> pElements) {
        PagedResources<Resource<Dataset>> pagedResources = super.toResource(pElements);
        pagedResources.forEach(resource -> datasetLinkAdder.addLinks(resource)); // NOSONAR
        return pagedResources;
    }

}
