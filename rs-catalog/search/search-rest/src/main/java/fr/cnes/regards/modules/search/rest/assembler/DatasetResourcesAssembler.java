/*
 * Copyright 2017-2018 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
 *
 * This file is part of REGARDS.
 *
 * REGARDS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * REGARDS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with REGARDS. If not, see <http://www.gnu.org/licenses/>.
 */
package fr.cnes.regards.modules.search.rest.assembler;

import org.springframework.hateoas.Resource;
import org.springframework.hateoas.ResourceAssembler;

import fr.cnes.regards.framework.hateoas.IResourceService;
import fr.cnes.regards.modules.entities.domain.Dataset;
import fr.cnes.regards.modules.search.rest.assembler.link.DatasetLinkAdder;

/**
 * Custom {@link ResourcesAssembler} for {@link Dataset}s using the {@link IResourceService} and adding links.
 * @author Xavier-Alexandre Brochard
 */
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
