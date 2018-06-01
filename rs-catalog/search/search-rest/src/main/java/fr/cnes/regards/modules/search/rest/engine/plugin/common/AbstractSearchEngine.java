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
package fr.cnes.regards.modules.search.rest.engine.plugin.common;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.modules.entities.domain.AbstractEntity;
import fr.cnes.regards.modules.entities.domain.Collection;
import fr.cnes.regards.modules.entities.domain.DataObject;
import fr.cnes.regards.modules.entities.domain.Dataset;
import fr.cnes.regards.modules.entities.domain.Document;
import fr.cnes.regards.modules.indexer.dao.FacetPage;
import fr.cnes.regards.modules.indexer.domain.IIndexable;
import fr.cnes.regards.modules.indexer.domain.SearchKey;
import fr.cnes.regards.modules.indexer.domain.criterion.ICriterion;
import fr.cnes.regards.modules.search.domain.plugin.ISearchEngine;
import fr.cnes.regards.modules.search.service.ICatalogSearchService;

/**
 * Generic search plugin implementation
 *
 * @author Marc Sordi
 *
 */
public abstract class AbstractSearchEngine implements ISearchEngine {

    /**
     * Path parameter dataset identifier key in parameters {@link MultiValueMap}
     */
    private static final String DATASET_ID = "@dataset@";

    /**
     * Business search service
     */
    @Autowired
    protected ICatalogSearchService searchService;

    @Override
    public List<String> extractFacets(MultiValueMap<String, String> allParams) throws ModuleException {
        return null;
    }

    protected <S, R extends IIndexable> FacetPage<R> search(SearchKey<S, R> searchKey, HttpHeaders headers,
            MultiValueMap<String, String> allParams, Pageable pageable) throws ModuleException {
        // Convert parameters to business criterion
        ICriterion criterion = parse(allParams);
        // Extract facets
        List<String> facets = extractFacets(allParams);
        // Do business search
        return searchService.search(criterion, searchKey, facets, pageable);
    }

    @Override
    public ResponseEntity<?> searchAll(SearchKey<AbstractEntity, AbstractEntity> searchKey, HttpHeaders headers,
            MultiValueMap<String, String> allParams, Pageable pageable) throws ModuleException {
        return ResponseEntity.ok(transform(search(searchKey, headers, allParams, pageable)));
    }

    @Override
    public ResponseEntity<?> searchAllExtra(SearchKey<AbstractEntity, AbstractEntity> searchKey, String extra,
            HttpHeaders headers, MultiValueMap<String, String> allParams, Pageable pageable) throws ModuleException {
        throw new UnsupportedOperationException();
    }

    @Override
    public ResponseEntity<?> searchAllCollections(SearchKey<Collection, Collection> searchKey, HttpHeaders headers,
            MultiValueMap<String, String> allParams, Pageable pageable) throws ModuleException {
        return ResponseEntity.ok(transform(search(searchKey, headers, allParams, pageable)));
    }

    @Override
    public ResponseEntity<?> searchAllCollectionsExtra(SearchKey<Collection, Collection> searchKey, String extra,
            HttpHeaders headers, MultiValueMap<String, String> allParams, Pageable pageable) throws ModuleException {
        throw new UnsupportedOperationException();
    }

    @Override
    public ResponseEntity<?> searchAllDocuments(SearchKey<Document, Document> searchKey, HttpHeaders headers,
            MultiValueMap<String, String> allParams, Pageable pageable) throws ModuleException {
        return ResponseEntity.ok(transform(search(searchKey, headers, allParams, pageable)));
    }

    @Override
    public ResponseEntity<?> searchAllDocumentsExtra(SearchKey<Document, Document> searchKey, String extra,
            HttpHeaders headers, MultiValueMap<String, String> allParams, Pageable pageable) throws ModuleException {
        throw new UnsupportedOperationException();
    }

    @Override
    public ResponseEntity<?> searchAllDatasets(SearchKey<Dataset, Dataset> searchKey, HttpHeaders headers,
            MultiValueMap<String, String> allParams, Pageable pageable) throws ModuleException {
        return ResponseEntity.ok(transform(search(searchKey, headers, allParams, pageable)));
    }

    @Override
    public ResponseEntity<?> searchAllDatasetsExtra(SearchKey<Dataset, Dataset> searchKey, String extra,
            HttpHeaders headers, MultiValueMap<String, String> allParams, Pageable pageable) throws ModuleException {
        throw new UnsupportedOperationException();
    }

    @Override
    public ResponseEntity<?> searchAllDataobjects(SearchKey<DataObject, DataObject> searchKey, HttpHeaders headers,
            MultiValueMap<String, String> allParams, Pageable pageable) throws ModuleException {
        return ResponseEntity.ok(transform(search(searchKey, headers, allParams, pageable)));
    }

    @Override
    public ResponseEntity<?> searchAllDataobjectsExtra(SearchKey<DataObject, DataObject> searchKey, String extra,
            HttpHeaders headers, MultiValueMap<String, String> allParams, Pageable pageable) throws ModuleException {
        throw new UnsupportedOperationException();
    }

    @Override
    public ResponseEntity<?> searchSingleDataset(SearchKey<DataObject, DataObject> searchKey, String datasetId,
            HttpHeaders headers, MultiValueMap<String, String> allParams, Pageable pageable) throws ModuleException {
        allParams.add(DATASET_ID, datasetId);
        return ResponseEntity.ok(transform(search(searchKey, headers, allParams, pageable)));
    }

    @Override
    public ResponseEntity<?> searchSingleDatasetExtra(SearchKey<DataObject, DataObject> searchKey, String datasetId,
            String extra, HttpHeaders headers, MultiValueMap<String, String> allParams, Pageable pageable)
            throws ModuleException {
        throw new UnsupportedOperationException();
    }

    @Override
    public ResponseEntity<?> searchDataobjectsReturnDatasets(SearchKey<DataObject, Dataset> searchKey,
            HttpHeaders headers, MultiValueMap<String, String> allParams, Pageable pageable) throws ModuleException {
        return ResponseEntity.ok(transform(search(searchKey, headers, allParams, pageable)));
    }
}
