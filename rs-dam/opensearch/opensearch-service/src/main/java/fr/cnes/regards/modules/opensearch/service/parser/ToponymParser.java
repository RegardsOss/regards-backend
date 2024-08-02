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

package fr.cnes.regards.modules.opensearch.service.parser;

import fr.cnes.regards.framework.feign.security.FeignSecurityManager;
import fr.cnes.regards.framework.geojson.geometry.IGeometry;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.utils.ResponseEntityUtils;
import fr.cnes.regards.modules.dam.domain.datasources.plugins.DataSourceException;
import fr.cnes.regards.modules.indexer.domain.criterion.ICriterion;
import fr.cnes.regards.modules.indexer.domain.criterion.exception.InvalidGeometryException;
import fr.cnes.regards.modules.opensearch.service.exception.OpenSearchParseException;
import fr.cnes.regards.modules.toponyms.client.IToponymsClient;
import fr.cnes.regards.modules.toponyms.domain.ToponymDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

/**
 * This {@link IParser} implementation only handles toponyms of the OpenSearch request and returns an
 * {@link ICriterion} describing the Geometry.<br>
 *
 * @author Iliana Ghazali
 */

public class ToponymParser implements IParser {

    // Class logger
    private static final Logger LOGGER = LoggerFactory.getLogger(ToponymParser.class);

    /**
     * Parameter to retrieve from the opensearch query parameters
     */
    public static final String TOPONYM_BUSINESS_ID = "toponym";

    /**
     * Client to get toponyms
     */
    private final IToponymsClient toponymClient;

    public ToponymParser(IToponymsClient toponymClient) {
        this.toponymClient = toponymClient;
    }

    @Override
    public ICriterion parse(MultiValueMap<String, String> parameters) throws OpenSearchParseException {
        // Check required query parameter
        if (!parameters.containsKey(TOPONYM_BUSINESS_ID)) {
            return null;
        } else {
            try {
                return ToponymGeometryCriterionBuilder.build(getToponymGeometry(parameters.getFirst(TOPONYM_BUSINESS_ID)));
            } catch (InvalidGeometryException | DataSourceException | EntityNotFoundException e) {
                throw new OpenSearchParseException(e);
            }
        }
    }

    /**
     * Get a toponym from the toponym server
     *
     * @param businessId reference of the toponym
     * @return toponym with {@link IGeometry} format
     * @throws DataSourceException     thrown if client could not be called
     * @throws EntityNotFoundException thrown if the corresponding toponym was not found in the toponym database
     */
    public IGeometry getToponymGeometry(String businessId) throws DataSourceException, EntityNotFoundException {
        ResponseEntity<EntityModel<ToponymDTO>> response;
        try {
            FeignSecurityManager.asInstance();
            // remote request to toponym server
            response = toponymClient.get(businessId);
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            LOGGER.error(e.getMessage(), e);
            throw new EntityNotFoundException(businessId, ToponymDTO.class);
        } finally {
            FeignSecurityManager.reset();
        }

        // Manage request error
        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new DataSourceException("Error while calling Toponym client (HTTP STATUS : "
                                          + response.getStatusCode());
        }
        return extractGeometryOrThrow(response, businessId);
    }

    private IGeometry extractGeometryOrThrow(ResponseEntity<EntityModel<ToponymDTO>> response, String businessId)
        throws EntityNotFoundException {
        ToponymDTO toponym = ResponseEntityUtils.extractContentOrNull(response);
        if (toponym != null) {
            IGeometry geometry = toponym.getGeometry();
            if (geometry != null) {
                return geometry;
            }
        }
        throw new EntityNotFoundException(businessId, IGeometry.class);
    }
}
