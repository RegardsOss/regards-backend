/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.search.rest.facet;

import java.util.Map;

import org.apache.commons.configuration.ConversionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.converter.Converter;
import org.springframework.web.bind.annotation.ControllerAdvice;

import com.google.common.collect.ImmutableMap;

import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.modules.indexer.domain.facet.FacetType;
import fr.cnes.regards.modules.models.domain.attributes.AttributeModel;
import fr.cnes.regards.modules.models.domain.attributes.AttributeType;
import fr.cnes.regards.modules.search.service.cache.IAttributeModelCache;

/**
 * Teaches Spring how to convert a list of attribute names into a map of facet types.
 *
 * @author Xavier-Alexandre Brochard
 */
@ControllerAdvice
public class AttributeNamesToFacetTypesMap implements Converter<String[], Map<String, FacetType>> {

    @Autowired
    private IAttributeModelCache attributeModelCache;

    // @formatter:off
    private static final Map<AttributeType, FacetType> MAP = new ImmutableMap.Builder<AttributeType, FacetType>()
            .put(AttributeType.STRING, FacetType.STRING)
            .put(AttributeType.STRING_ARRAY, FacetType.STRING)
            .put(AttributeType.INTEGER, FacetType.NUMERIC)
            .put(AttributeType.INTEGER_ARRAY, FacetType.NUMERIC)
            .put(AttributeType.DOUBLE, FacetType.NUMERIC)
            .put(AttributeType.DOUBLE_ARRAY, FacetType.NUMERIC)
            .put(AttributeType.LONG, FacetType.NUMERIC)
            .put(AttributeType.LONG_ARRAY, FacetType.NUMERIC)
            .put(AttributeType.DATE_ISO8601, FacetType.DATE)
            .put(AttributeType.DATE_ARRAY, FacetType.DATE)
            .put(AttributeType.INTEGER_INTERVAL, FacetType.RANGE)
            .put(AttributeType.DOUBLE_INTERVAL, FacetType.RANGE)
            .put(AttributeType.LONG_INTERVAL, FacetType.RANGE)
            .put(AttributeType.DATE_INTERVAL, FacetType.RANGE)
            .build();
    // @formatter:on

    /**
     * Converts a list of attribute names like ["altitude", "lastUpdate"] to the map of corresponding facets.
     */
    @Override
    public Map<String, FacetType> convert(String[] pAttributeNames) {
        ImmutableMap.Builder<String, FacetType> facetMapBuilder = new ImmutableMap.Builder<>();

        try {
            for (String attributeName : pAttributeNames) {
                AttributeModel model = attributeModelCache.findByName(attributeName);
                facetMapBuilder.put(attributeName, MAP.get(model.getType()));
            }
        } catch (EntityNotFoundException e) {
            throw new ConversionException(e);
        }

        return facetMapBuilder.build();
    }

}
