/*
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.search.service;

import com.google.common.collect.ImmutableMap;
import fr.cnes.regards.modules.indexer.domain.facet.FacetType;
import fr.cnes.regards.modules.model.domain.attributes.AttributeModel;
import fr.cnes.regards.modules.model.dto.properties.PropertyType;
import fr.cnes.regards.modules.opensearch.service.cache.attributemodel.IAttributeFinder;
import fr.cnes.regards.modules.opensearch.service.exception.OpenSearchUnknownParameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * The converter retrieves attributes regarding their names. It may be internal, static or dynamic attributes.
 * And then build facets according to attribute properties.
 *
 * @author Marc Sordi
 */
@Service
public class FacetConverter implements IFacetConverter {

    private static final Logger LOGGER = LoggerFactory.getLogger(FacetConverter.class);

    // @formatter:off
    private static final Map<PropertyType, FacetType> MAP = new ImmutableMap.Builder<PropertyType, FacetType>()
            .put(PropertyType.URL, FacetType.STRING)
            .put(PropertyType.STRING, FacetType.STRING)
            .put(PropertyType.STRING_ARRAY, FacetType.STRING)
            .put(PropertyType.BOOLEAN, FacetType.BOOLEAN)
            .put(PropertyType.INTEGER, FacetType.NUMERIC)
            .put(PropertyType.INTEGER_ARRAY, FacetType.NUMERIC)
            .put(PropertyType.DOUBLE, FacetType.NUMERIC)
            .put(PropertyType.DOUBLE_ARRAY, FacetType.NUMERIC)
            .put(PropertyType.LONG, FacetType.NUMERIC)
            .put(PropertyType.LONG_ARRAY, FacetType.NUMERIC)
            .put(PropertyType.DATE_ISO8601, FacetType.DATE)
            .put(PropertyType.DATE_ARRAY, FacetType.DATE)
            .put(PropertyType.INTEGER_INTERVAL, FacetType.NUMERIC)
            .put(PropertyType.DOUBLE_INTERVAL, FacetType.NUMERIC)
            .put(PropertyType.LONG_INTERVAL, FacetType.NUMERIC)
            .put(PropertyType.DATE_INTERVAL, FacetType.DATE)
            .put(PropertyType.INTEGER_RANGE, FacetType.NUMERIC)
            .put(PropertyType.DOUBLE_RANGE, FacetType.NUMERIC)
            .put(PropertyType.LONG_RANGE, FacetType.NUMERIC)
            .put(PropertyType.DATE_RANGE, FacetType.DATE)
            .build();
    // @formatter:on

    /**
     * Service retrieving the up-to-date list of {@link AttributeModel}s. Autowired by Spring.
     */
    private final IAttributeFinder finder;

    public FacetConverter(IAttributeFinder finder) {
        this.finder = finder;
    }

    @Override
    public Map<String, FacetType> convert(List<String> propertyNames, Map<String, String> reverseFacetNames)
        throws OpenSearchUnknownParameter {
        if (propertyNames == null) {
            return null;
        }

        ImmutableMap.Builder<String, FacetType> facetMapBuilder = new ImmutableMap.Builder<>();

        for (String propertyName : propertyNames) {
            AttributeModel attModel = finder.findByName(propertyName);
            String queryablePath = attModel.getFullJsonPath();
            FacetType facetType = MAP.get(attModel.getType());
            if (facetType != null) {
                facetMapBuilder.put(queryablePath, facetType);
                reverseFacetNames.put(queryablePath, propertyName);
            } else {
                LOGGER.warn("Facets are not available for attribute type {}", attModel.getType());
            }
        }

        return facetMapBuilder.build();
    }

}
