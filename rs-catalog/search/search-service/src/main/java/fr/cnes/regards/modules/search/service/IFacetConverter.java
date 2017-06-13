/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.search.service;

import java.util.Map;

import fr.cnes.regards.modules.indexer.domain.facet.FacetType;

/**
 *
 * Convert list of URL facets to search facets.
 * @author Marc Sordi
 *
 */
@FunctionalInterface
public interface IFacetConverter {

    public Map<String, FacetType> convert(String[] propertyNames);
}
