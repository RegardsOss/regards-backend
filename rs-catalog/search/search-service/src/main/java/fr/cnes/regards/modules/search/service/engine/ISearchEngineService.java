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
package fr.cnes.regards.modules.search.service.engine;

import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;

import fr.cnes.regards.modules.indexer.domain.IIndexable;
import fr.cnes.regards.modules.indexer.domain.SearchKey;

/**
 * Search engine service contract
 *
 * @author Marc Sordi
 *
 */
public interface ISearchEngineService {

    // TODO document
    <S, R extends IIndexable> ResponseEntity<?> handleRequest(SearchKey<S, R> searchKey, String engineType,
            HttpHeaders headers, MultiValueMap<String, String> allParams, Pageable pageable);

    // TODO document
    <S, R extends IIndexable> ResponseEntity<?> handleRequest(SearchKey<S, R> searchKey, String engineType,
            String extra, HttpHeaders headers, MultiValueMap<String, String> allParams, Pageable pageable);
}
