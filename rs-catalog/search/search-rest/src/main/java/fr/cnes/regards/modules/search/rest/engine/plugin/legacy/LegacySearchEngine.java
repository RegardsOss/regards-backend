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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.MultiValueMap;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;
import fr.cnes.regards.modules.entities.domain.AbstractEntity;
import fr.cnes.regards.modules.indexer.dao.FacetPage;
import fr.cnes.regards.modules.indexer.domain.IIndexable;
import fr.cnes.regards.modules.indexer.domain.criterion.ICriterion;
import fr.cnes.regards.modules.opensearch.service.IOpenSearchService;
import fr.cnes.regards.modules.search.rest.assembler.FacettedPagedResourcesAssembler;
import fr.cnes.regards.modules.search.rest.engine.plugin.common.AbstractSearchEngine;

/**
 * Native search engine used for compatibility with legacy system
 * @author Marc Sordi
 */
@Plugin(id = "legacy", author = "REGARDS Team", contact = "regards@c-s.fr", description = "Native search engine",
        licence = "GPLv3", owner = "CSSI", url = "https://github.com/RegardsOss", version = "1.0.0")
public class LegacySearchEngine extends AbstractSearchEngine {

    @SuppressWarnings("unused")
    private static final Logger LOGGER = LoggerFactory.getLogger(LegacySearchEngine.class);

    /**
     * Query parameter for facets
     */
    private static final String FACETS = "facets";

    @Autowired
    private IOpenSearchService openSearchService;

    // FIXME
    @Autowired
    private FacettedPagedResourcesAssembler<AbstractEntity> assembler;

    @Override
    public ICriterion parse(MultiValueMap<String, String> allParams) throws ModuleException {
        return openSearchService.parse(allParams);
    }

    @Override
    public List<String> extractFacets(MultiValueMap<String, String> allParams) throws ModuleException {
        return allParams.get(FACETS);
    }

    /*
     * (non-Javadoc)
     * 
     * @see fr.cnes.regards.modules.search.domain.plugin.ISearchEngine#transform(fr.cnes.regards.modules.indexer.dao.
     * FacetPage)
     */
    @Override
    public <T, R extends IIndexable> T transform(FacetPage<R> page) {
        // TODO Auto-generated method stub
        return null;
    }

    // // FIXME
    // @SuppressWarnings("unchecked")
    // @Override
    // public <R extends IIndexable, T> T transform(FacetPage<R> page) throws ModuleException {
    // return (T) assembler.toResource((FacetPage<AbstractEntity>) page);
    // }
}
