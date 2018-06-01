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
package fr.cnes.regards.modules.search.rest.engine.plugin.opensearch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;
import fr.cnes.regards.modules.entities.domain.AbstractEntity;
import fr.cnes.regards.modules.indexer.dao.FacetPage;
import fr.cnes.regards.modules.indexer.domain.IIndexable;
import fr.cnes.regards.modules.indexer.domain.SearchKey;
import fr.cnes.regards.modules.indexer.domain.criterion.ICriterion;
import fr.cnes.regards.modules.search.rest.engine.plugin.common.AbstractSearchEngine;
import fr.cnes.regards.modules.search.rest.engine.plugin.common.HelloWorld;

/**
 * OpenSearch engine plugin
 * @author Marc Sordi
 */
@Plugin(id = "opensearch", author = "REGARDS Team", contact = "regards@c-s.fr", description = "Native search engine",
        licence = "GPLv3", owner = "CSSI", url = "https://github.com/RegardsOss", version = "1.0.0")
public class OpenSearchEngine extends AbstractSearchEngine {

    @SuppressWarnings("unused")
    private static final Logger LOGGER = LoggerFactory.getLogger(OpenSearchEngine.class);

    @Override
    public ICriterion parse(MultiValueMap<String, String> allParams) throws ModuleException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <T, R extends IIndexable> T transform(FacetPage<R> page) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ResponseEntity<?> searchAll(SearchKey<AbstractEntity, AbstractEntity> searchKey, HttpHeaders headers,
            MultiValueMap<String, String> allParams, Pageable pageable) throws ModuleException {
        // TODO
        HelloWorld world = new HelloWorld();
        world.setMessage("Yes! We did it!");
        return ResponseEntity.ok(world);
    }
}
