/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.search.rest.converter;

import java.util.Map;

import org.apache.commons.configuration.ConversionException;
import org.springframework.core.convert.converter.Converter;
import org.springframework.web.bind.annotation.ControllerAdvice;

import com.google.common.collect.ImmutableMap;

import fr.cnes.regards.modules.indexer.domain.facet.FacetType;
import fr.cnes.regards.modules.models.domain.attributes.AttributeModel;
import fr.cnes.regards.modules.models.domain.attributes.AttributeType;
import fr.cnes.regards.modules.opensearch.service.cache.attributemodel.IAttributeFinder;
import fr.cnes.regards.modules.opensearch.service.exception.OpenSearchUnknownParameter;

/**
 * Teaches Spring how to convert a list of attribute names into a map of facet types.
 *
 * @author Xavier-Alexandre Brochard
 */
@ControllerAdvice
public class AttributeNamesToFacetTypesMap implements Converter<String[], Map<String, FacetType>> {

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
     * Service retrieving the up-to-date list of {@link AttributeModel}s. Autowired by Spring.
     */
    private final IAttributeFinder finder;

    /**
     * @param pAttributeModelCache
     */
    public AttributeNamesToFacetTypesMap(IAttributeFinder finder) {
        super();
        this.finder = finder;
    }

    /**
     * Converts a list of attribute names like ["altitude", "lastUpdate"] to the map of corresponding facets.
     */
    @Override
    public Map<String, FacetType> convert(String[] pAttributeNames) {
        ImmutableMap.Builder<String, FacetType> facetMapBuilder = new ImmutableMap.Builder<>();

        try {
            for (String attributeName : pAttributeNames) {
                AttributeModel model = finder.findByName(attributeName);
                // FIXME : add properties wrapper if does not exists
                facetMapBuilder.put(attributeName, MAP.get(model.getType()));
            }
        } catch (OpenSearchUnknownParameter e) {
            throw new ConversionException(e.getMessage(), e);
        }

        return facetMapBuilder.build();
    }

}
