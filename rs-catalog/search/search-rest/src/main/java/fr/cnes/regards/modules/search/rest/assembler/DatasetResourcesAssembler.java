/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.search.rest.assembler;

import org.springframework.hateoas.Resource;
import org.springframework.hateoas.ResourceAssembler;
import org.springframework.stereotype.Component;

import fr.cnes.regards.framework.hateoas.IResourceService;
import fr.cnes.regards.modules.entities.domain.Dataset;
import fr.cnes.regards.modules.search.rest.assembler.link.DatasetLinkAdder;

/**
 * Custom {@link ResourcesAssembler} for {@link Dataset}s.
 * @author Xavier-Alexandre Brochard
 */
@Component
public class DatasetResourcesAssembler implements ResourceAssembler<Dataset, Resource<Dataset>> {

    /**
     * The resource service
     */
    private final IResourceService resourceService;

    /**
     * Takes a {@link Dataset} resource and adds the HATEOAS links to it
     */
    private final DatasetLinkAdder datasetLinkAdder;

    /**
     * Constructor
     * @param pDatasetLinkAdder Takes a {@link Dataset} resource and adds the HATEOAS links to it
     * @param pResourceService the resource service
     */
    public DatasetResourcesAssembler(DatasetLinkAdder pDatasetLinkAdder, IResourceService pResourceService) {
        datasetLinkAdder = pDatasetLinkAdder;
        resourceService = pResourceService;
    }

    @Override
    public Resource<Dataset> toResource(Dataset pElement) {
        Resource<Dataset> resource = resourceService.toResource(pElement);
        datasetLinkAdder.addLinks(resource);
        return resource;
    }

}
