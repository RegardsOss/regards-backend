/*
 * Copyright 2017-2020 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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


package fr.cnes.regards.modules.opensearch.service.parser;

import fr.cnes.regards.framework.feign.security.FeignSecurityManager;
import fr.cnes.regards.framework.geojson.geometry.IGeometry;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.modules.dam.domain.datasources.plugins.DataSourceException;
import fr.cnes.regards.modules.indexer.domain.criterion.ICriterion;
import fr.cnes.regards.modules.indexer.domain.criterion.exception.InvalidGeometryException;
import fr.cnes.regards.modules.opensearch.service.exception.OpenSearchParseException;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;

/**
 * This {@link IParser} implementation only handles toponyms of the OpenSearch request and returns an
 * {@link ICriterion} describing the Geometry.<br>
 * @author Iliana Ghazali
 */

public class ToponymParser implements IParser {

    /** Parameter to retrieve from the opensearch query parameters */
    public static final String TOPONYM_BUSINESS_ID = "toponym";
    
    /** Client to get toponyms */
    private final IToponymClient toponymClient;

    
    public ToponymParser(IToponymClient toponymClient) {
        this.toponymClient = toponymClient;
    }

    @Override
    public ICriterion parse(MultiValueMap<String, String> parameters) throws OpenSearchParseException {
        // Check required query parameter
        if (!parameters.containsKey(TOPONYM_BUSINESS_ID)) {
            return null;
        } else {
            try {
                return ToponymGeometryCriterionBuilder
                        .build(getToponymGeometry(parameters.getFirst(TOPONYM_BUSINESS_ID)));
            } catch (InvalidGeometryException | DataSourceException | EntityNotFoundException e) {
                throw new OpenSearchParseException(e);
            }
        }
    }

    /**
     * Get a toponym from the toponym server
     * @param businessId reference of the toponym
     * @return toponym with {@link IGeometry} format
     * @throws DataSourceException thrown if client could not be called
     * @throws EntityNotFoundException thrown if the corresponding toponym was not found in the toponym database
     */
    public IGeometry getToponymGeometry(String businessId) throws DataSourceException, EntityNotFoundException {
        ResponseEntity<EntityModel<IGeometry>> response;
        try {
            FeignSecurityManager.asSystem();
            // remote request to toponym server
            response = toponymClient.retrieveToponym(businessId);
        } finally {
            FeignSecurityManager.reset();
        }

        // Manage request error
        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new DataSourceException(
                    "Error while calling Toponym client (HTTP STATUS : " + response.getStatusCode());
        }

        // check if toponym was found in the toponym server, if not throw exception
        EntityModel<IGeometry> toponymGeometry = response.getBody();
        if (response.getBody() == null || toponymGeometry == null) {
            throw new EntityNotFoundException(businessId, IGeometry.class);
        }
        // Return toponym geometry
        return toponymGeometry.getContent();
    }
}
