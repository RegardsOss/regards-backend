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

import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;
import fr.cnes.regards.modules.indexer.domain.criterion.ICriterion;
import fr.cnes.regards.modules.search.domain.plugin.ISearchEngine;
import fr.cnes.regards.modules.search.domain.plugin.SearchContext;
import fr.cnes.regards.modules.search.domain.plugin.SearchType;
import fr.cnes.regards.modules.search.rest.engine.plugin.common.HelloWorld;

/**
 * OpenSearch engine plugin
 * @author Marc Sordi
 */
@Plugin(id = "OpenSearchEngine", author = "REGARDS Team", contact = "regards@c-s.fr",
        description = "Native search engine", licence = "GPLv3", owner = "CSSI", url = "https://github.com/RegardsOss",
        version = "1.0.0")
public class OpenSearchEngine implements ISearchEngine<HelloWorld, Void> {

    @Override
    public boolean supports(SearchType searchType) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public ResponseEntity<HelloWorld> search(SearchContext context) throws ModuleException {
        // TODO Auto-generated method stub
        HelloWorld world = new HelloWorld();
        world.setMessage("Yes! We did it!");
        return ResponseEntity.ok(world);
    }

    @Override
    public ICriterion parse(MultiValueMap<String, String> queryParams) throws ModuleException {
        // TODO Auto-generated method stub
        return null;
    }

}
