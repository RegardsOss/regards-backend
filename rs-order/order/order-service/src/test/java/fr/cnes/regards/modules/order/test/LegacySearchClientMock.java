/*
 * Copyright 2017-2024 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.order.test;

import fr.cnes.regards.framework.urn.UniformResourceName;
import fr.cnes.regards.modules.dam.domain.entities.feature.EntityFeature;
import fr.cnes.regards.modules.search.client.ILegacySearchEngineClient;
import fr.cnes.regards.modules.search.domain.plugin.legacy.FacettedPagedModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;

import java.util.List;

/**
 * @author Sébastien Binda
 */
public class LegacySearchClientMock implements ILegacySearchEngineClient {

    @Override
    public ResponseEntity<FacettedPagedModel<EntityModel<EntityFeature>>> searchAll(HttpHeaders headers,
                                                                                    MultiValueMap<String, String> queryParams,
                                                                                    String engineParserType,
                                                                                    int page,
                                                                                    int size) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ResponseEntity<EntityModel<EntityFeature>> getEntity(UniformResourceName urn, HttpHeaders headers) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ResponseEntity<FacettedPagedModel<EntityModel<EntityFeature>>> searchAllCollections(HttpHeaders headers,
                                                                                               MultiValueMap<String, String> queryParams,
                                                                                               String engineParserType,
                                                                                               int page,
                                                                                               int size) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ResponseEntity<List<String>> searchCollectionPropertyValues(String propertyName,
                                                                       HttpHeaders headers,
                                                                       MultiValueMap<String, String> queryParams,
                                                                       int maxCount) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ResponseEntity<EntityModel<EntityFeature>> getCollection(UniformResourceName urn, HttpHeaders headers) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ResponseEntity<FacettedPagedModel<EntityModel<EntityFeature>>> searchAllDatasets(HttpHeaders headers,
                                                                                            MultiValueMap<String, String> queryParams,
                                                                                            String engineParserType,
                                                                                            int page,
                                                                                            int size) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ResponseEntity<List<String>> searchDatasetPropertyValues(String propertyName,
                                                                    HttpHeaders headers,
                                                                    MultiValueMap<String, String> queryParams,
                                                                    int maxCount) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ResponseEntity<EntityModel<EntityFeature>> getDataset(UniformResourceName urn, HttpHeaders headers) {
        return new ResponseEntity<>(EntityModel.of(SearchClientMock.DS_MAP.get(urn)), HttpStatus.OK);
    }

    @Override
    public ResponseEntity<FacettedPagedModel<EntityModel<EntityFeature>>> searchAllDataobjects(HttpHeaders headers,
                                                                                               MultiValueMap<String, String> queryParams,
                                                                                               String engineParserType,
                                                                                               int page,
                                                                                               int size) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ResponseEntity<List<String>> searchDataobjectPropertyValues(String propertyName,
                                                                       HttpHeaders headers,
                                                                       MultiValueMap<String, String> queryParams,
                                                                       int maxCount) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ResponseEntity<EntityModel<EntityFeature>> getDataobject(UniformResourceName urn, HttpHeaders headers) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ResponseEntity<FacettedPagedModel<EntityModel<EntityFeature>>> searchSingleDataset(String datasetUrn,
                                                                                              HttpHeaders headers,
                                                                                              MultiValueMap<String, String> queryParams,
                                                                                              String engineParserType,
                                                                                              int page,
                                                                                              int size) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ResponseEntity<List<String>> searchDataobjectPropertyValuesOnDataset(String datasetUrn,
                                                                                String propertyName,
                                                                                HttpHeaders headers,
                                                                                MultiValueMap<String, String> queryParams,
                                                                                int maxCount) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ResponseEntity<FacettedPagedModel<EntityModel<EntityFeature>>> searchDataobjectsReturnDatasets(HttpHeaders headers,
                                                                                                          MultiValueMap<String, String> queryParams,
                                                                                                          String engineParserType,
                                                                                                          int page,
                                                                                                          int size) {
        // TODO Auto-generated method stub
        return null;
    }

}
