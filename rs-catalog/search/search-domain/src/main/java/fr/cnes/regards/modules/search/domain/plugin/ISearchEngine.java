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
package fr.cnes.regards.modules.search.domain.plugin;

import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;

import fr.cnes.regards.framework.modules.plugins.annotations.PluginInterface;

/**
 * TODO
 *
 * @author Marc Sordi
 *
 */
@PluginInterface(description = "Search engine plugin interface")
public interface ISearchEngine {

    // /**
    // * Map request context to search context
    // * @param context request context
    // * @return search context
    // */
    // SearchContext<S, R> map(SearchContext<S, R> context);
    //
    // Object map(FacetPage<R> page);

    // TODO
    ResponseEntity<?> handleRequest(String engineType, String extra, HttpHeaders headers,
            MultiValueMap<String, String> allParams, Pageable pageable);
}
