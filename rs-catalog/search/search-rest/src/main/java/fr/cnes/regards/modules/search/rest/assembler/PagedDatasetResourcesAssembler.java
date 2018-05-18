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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.web.HateoasPageableHandlerMethodArgumentResolver;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;

import fr.cnes.regards.framework.hateoas.IResourceService;
import fr.cnes.regards.modules.entities.domain.Dataset;
import fr.cnes.regards.modules.search.rest.assembler.link.DatasetLinkAdder;

/**
 * Custom {@link PagedResourcesAssembler} for {@link Dataset}s using the {@link IResourceService} to convert to resource and adding links.
 * @author Xavier-Alexandre Brochard
 */
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
