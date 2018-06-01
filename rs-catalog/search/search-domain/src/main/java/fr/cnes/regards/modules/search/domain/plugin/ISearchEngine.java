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

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.annotations.PluginInterface;
import fr.cnes.regards.modules.entities.domain.AbstractEntity;
import fr.cnes.regards.modules.entities.domain.Collection;
import fr.cnes.regards.modules.entities.domain.DataObject;
import fr.cnes.regards.modules.entities.domain.Dataset;
import fr.cnes.regards.modules.entities.domain.Document;
import fr.cnes.regards.modules.indexer.dao.FacetPage;
import fr.cnes.regards.modules.indexer.domain.IIndexable;
import fr.cnes.regards.modules.indexer.domain.SearchKey;
import fr.cnes.regards.modules.indexer.domain.criterion.ICriterion;

/**
 * TODO : peut-être gérer le support des MediaType ou non?
 *
 * Search engine plugin contract
 *
 * @author Marc Sordi
 *
 */
@PluginInterface(description = "Search engine plugin interface")
public interface ISearchEngine {

    /**
     * Parse query parameters and transform to {@link ICriterion} (available for all search method)<br/>
     * Use {@link ICriterion} as criterion builder.
     * @param allParams all query parameters
     * @return {@link ICriterion}
     */
    ICriterion parse(MultiValueMap<String, String> allParams) throws ModuleException;

    /**
     * Extract facets from query parameters (available for all search method)
     */
    List<String> extractFacets(MultiValueMap<String, String> allParams) throws ModuleException;

    /**
     * Transform business response in whatever your want
     */
    <T, R extends IIndexable> T transform(FacetPage<R> page);

    /**
     * Cross entity search
     */
    ResponseEntity<?> searchAll(SearchKey<AbstractEntity, AbstractEntity> searchKey, HttpHeaders headers,
            MultiValueMap<String, String> allParams, Pageable pageable) throws ModuleException;

    /**
     * Additional route handling related to cross entity search
     */
    ResponseEntity<?> searchAllExtra(SearchKey<AbstractEntity, AbstractEntity> searchKey, String extra,
            HttpHeaders headers, MultiValueMap<String, String> allParams, Pageable pageable) throws ModuleException;

    /**
     * Collection search
     */
    ResponseEntity<?> searchAllCollections(SearchKey<Collection, Collection> searchKey, HttpHeaders headers,
            MultiValueMap<String, String> allParams, Pageable pageable) throws ModuleException;

    /**
     * Additional route handling related to collection search
     */
    ResponseEntity<?> searchAllCollectionsExtra(SearchKey<Collection, Collection> searchKey, String extra,
            HttpHeaders headers, MultiValueMap<String, String> allParams, Pageable pageable) throws ModuleException;

    /**
     * Document search
     */
    ResponseEntity<?> searchAllDocuments(SearchKey<Document, Document> searchKey, HttpHeaders headers,
            MultiValueMap<String, String> allParams, Pageable pageable) throws ModuleException;

    /**
     * Additional route handling related to document search
     */
    ResponseEntity<?> searchAllDocumentsExtra(SearchKey<Document, Document> searchKey, String extra,
            HttpHeaders headers, MultiValueMap<String, String> allParams, Pageable pageable) throws ModuleException;

    /**
     * Dataset search
     */
    ResponseEntity<?> searchAllDatasets(SearchKey<Dataset, Dataset> searchKey, HttpHeaders headers,
            MultiValueMap<String, String> allParams, Pageable pageable) throws ModuleException;

    /**
     * Additional route handling related to dataset search
     */
    ResponseEntity<?> searchAllDatasetsExtra(SearchKey<Dataset, Dataset> searchKey, String extra, HttpHeaders headers,
            MultiValueMap<String, String> allParams, Pageable pageable) throws ModuleException;

    /**
     * DataObject search
     */
    ResponseEntity<?> searchAllDataobjects(SearchKey<DataObject, DataObject> searchKey, HttpHeaders headers,
            MultiValueMap<String, String> allParams, Pageable pageable) throws ModuleException;

    /**
     * Additional route handling related to dataObject search
     */
    ResponseEntity<?> searchAllDataobjectsExtra(SearchKey<DataObject, DataObject> searchKey, String extra,
            HttpHeaders headers, MultiValueMap<String, String> allParams, Pageable pageable) throws ModuleException;

    /**
     * DataObject search by dataset
     */
    ResponseEntity<?> searchSingleDataset(SearchKey<DataObject, DataObject> searchKey, String datasetId,
            HttpHeaders headers, MultiValueMap<String, String> allParams, Pageable pageable) throws ModuleException;

    /**
     * Additional route handling related to dataObject search by dataset
     */
    ResponseEntity<?> searchSingleDatasetExtra(SearchKey<DataObject, DataObject> searchKey, String datasetId,
            String extra, HttpHeaders headers, MultiValueMap<String, String> allParams, Pageable pageable)
            throws ModuleException;

    /**
     * DataObject search returning datasets
     */
    ResponseEntity<?> searchDataobjectsReturnDatasets(SearchKey<DataObject, Dataset> searchKey, HttpHeaders headers,
            MultiValueMap<String, String> allParams, Pageable pageable) throws ModuleException;

}
