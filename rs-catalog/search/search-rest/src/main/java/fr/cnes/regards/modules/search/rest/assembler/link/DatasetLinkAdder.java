/*
 * Copyright 2017 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.search.rest.assembler.link;

import java.util.HashMap;
import java.util.Map;

import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.Resource;
import org.springframework.stereotype.Component;

import fr.cnes.regards.framework.hateoas.IResourceService;
import fr.cnes.regards.framework.hateoas.LinkRels;
import fr.cnes.regards.framework.hateoas.MethodParamFactory;
import fr.cnes.regards.framework.oais.urn.UniformResourceName;
import fr.cnes.regards.modules.entities.domain.Dataset;
import fr.cnes.regards.modules.search.rest.CatalogController;
import fr.cnes.regards.modules.search.rest.assembler.DatasetResourcesAssembler;
import fr.cnes.regards.modules.search.rest.assembler.FacettedPagedResourcesAssembler;

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
     * HATEOAS link to reach dataobjects
     */
    public static final String LINK_TO_DATAOBJECTS = "dataobjects";

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
                                MethodParamFactory.build(UniformResourceName.class, pResource.getContent().getIpId()),
                                MethodParamFactory.build(DatasetResourcesAssembler.class));

        Map<String, String> q = new HashMap<>();
        q.put("q", "tags:" + ipId.toString());
        resourceService.addLinkWithParams(pResource, CatalogController.class, "searchDataobjects", LINK_TO_DATAOBJECTS,
                                          MethodParamFactory.build(Map.class, q),
                                          MethodParamFactory.build(String[].class),
                                          MethodParamFactory.build(Pageable.class),
                                          MethodParamFactory.build(FacettedPagedResourcesAssembler.class));

        return pResource;
    }

}
