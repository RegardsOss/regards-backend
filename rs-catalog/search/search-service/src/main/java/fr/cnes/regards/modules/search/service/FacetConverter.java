/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.search.service;

import java.util.Map;

import org.apache.commons.configuration.ConversionException;
import org.springframework.stereotype.Service;

import com.google.common.collect.ImmutableMap;

import fr.cnes.regards.modules.indexer.domain.facet.FacetType;
import fr.cnes.regards.modules.models.domain.attributes.AttributeModel;
import fr.cnes.regards.modules.models.domain.attributes.AttributeType;
import fr.cnes.regards.modules.opensearch.service.cache.attributemodel.IAttributeFinder;
import fr.cnes.regards.modules.opensearch.service.exception.OpenSearchUnknownParameter;

/**
 * The converter retrieves attributes regarding their names. It may be static or dynamic attributes.
 * And then build facets according to attribute properties.
 *
 * @author Marc Sordi
 *
 */
@Service
public class FacetConverter implements IFacetConverter {

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
            .put(AttributeType.INTEGER_INTERVAL, FacetType.NUMERIC)
            .put(AttributeType.DOUBLE_INTERVAL, FacetType.NUMERIC)
            .put(AttributeType.LONG_INTERVAL, FacetType.NUMERIC)
            .put(AttributeType.DATE_INTERVAL, FacetType.NUMERIC)
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
    public Map<String, FacetType> convert(String[] propertyNames) {
        if (propertyNames == null) {
            return null;
        }

        ImmutableMap.Builder<String, FacetType> facetMapBuilder = new ImmutableMap.Builder<>();

        try {
            for (String propertyName : propertyNames) {
                AttributeModel attModel = finder.findByName(propertyName);
                facetMapBuilder.put(propertyName, MAP.get(attModel.getType()));
            }
        } catch (OpenSearchUnknownParameter e) {
            throw new ConversionException(e.getMessage(), e);
        }

        return facetMapBuilder.build();
    }

}
