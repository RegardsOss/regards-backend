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
package fr.cnes.regards.modules.search.rest.engine.plugin.legacy;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.Resource;
import org.springframework.http.ResponseEntity;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;
import fr.cnes.regards.modules.entities.domain.AbstractEntity;
import fr.cnes.regards.modules.indexer.dao.FacetPage;
import fr.cnes.regards.modules.indexer.domain.criterion.ICriterion;
import fr.cnes.regards.modules.search.domain.plugin.SearchContext;
import fr.cnes.regards.modules.search.domain.plugin.SearchType;
import fr.cnes.regards.modules.search.rest.assembler.FacettedPagedResourcesAssembler;
import fr.cnes.regards.modules.search.rest.assembler.resource.FacettedPagedResources;

/**
 * Native search engine used for compatibility with legacy system
 * @author Marc Sordi
 */
@Plugin(id = "LegacySearchAllEngine", author = "REGARDS Team", contact = "regards@c-s.fr",
        description = "Cross entity search for legacy search engine", licence = "GPLv3", owner = "CSSI",
        url = "https://github.com/RegardsOss", version = "1.0.0")
public class LegacySearchAllEngine
        extends AbstractLegacySearch<FacettedPagedResources<Resource<AbstractEntity>>, Void> {

    // FIXME
    @Autowired
    private FacettedPagedResourcesAssembler<AbstractEntity> assembler;

    @Override
    public boolean supports(SearchType searchType) {
        return SearchType.ALL.equals(searchType);
    }

    @Override
    public ResponseEntity<FacettedPagedResources<Resource<AbstractEntity>>> search(SearchContext context)
            throws ModuleException {
        // Convert parameters to business criterion
        ICriterion criterion = parse(context.getQueryParams());
        // Extract facets
        List<String> facets = context.getQueryParams().get(FACETS);
        // Do business search
        FacetPage<AbstractEntity> facetPage = searchService.search(criterion, context.getSearchType(), facets,
                                                                   context.getPageable());
        // Build and return HATEOAS response
        return ResponseEntity.ok(toResources(facetPage));
    }

    private FacettedPagedResources<Resource<AbstractEntity>> toResources(FacetPage<AbstractEntity> facetPage) {
        // PagedResources<Resource<AbstractEntity>> pagedResources = PagedResources.wrap(content, faceP)
        return null;
    }
}
