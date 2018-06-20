/*
 * Copyright 2017-2018 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.search.rest.engine.plugin.opensearch.formatter.geojson;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.compress.utils.Lists;
import org.springframework.hateoas.Link;

import fr.cnes.regards.framework.geojson.Feature;
import fr.cnes.regards.framework.geojson.FeatureWithPropertiesCollection;
import fr.cnes.regards.modules.entities.domain.AbstractEntity;
import fr.cnes.regards.modules.indexer.dao.FacetPage;
import fr.cnes.regards.modules.search.domain.plugin.SearchContext;
import fr.cnes.regards.modules.search.rest.engine.plugin.opensearch.OpenSearchConfiguration;
import fr.cnes.regards.modules.search.rest.engine.plugin.opensearch.OpenSearchParameterConfiguration;
import fr.cnes.regards.modules.search.rest.engine.plugin.opensearch.extension.IOpenSearchExtension;
import fr.cnes.regards.modules.search.rest.engine.plugin.opensearch.formatter.IOpenSearchResponseBuilder;
import fr.cnes.regards.modules.search.rest.engine.plugin.opensearch.formatter.atom.modules.gml.impl.GmlTimeModuleGenerator;
import fr.cnes.regards.modules.search.rest.engine.plugin.opensearch.formatter.atom.modules.regards.impl.RegardsModuleGenerator;

/**
 * Build open search responses in GEO+Json format handling :<ul>
 * <li>parameters extension</li>
 * <li>time & geo extension {@link GmlTimeModuleGenerator}</li>
 * <li>regards extension {@link RegardsModuleGenerator}</li>
 * </ul>
 * @author Sébastien Binda
 */
public class GeojsonResponseBuilder implements IOpenSearchResponseBuilder<FeatureWithPropertiesCollection> {

    public static final String ID = "id";

    public static final String TITLE = "title";

    public static final String TOTAL_RESULTS = "totalResults";

    public static final String START_INDEX = "startIndex";

    public static final String ITEMS_PER_PAGE = "itemsPerPage";

    public static final String DESCRIPTION = "description";

    public static final String QUERY = "query";

    public static final String LINKS = "links";

    public static final String APPLICATION_GEO_JSON_VALUE = "application/geo+json";

    private final List<IOpenSearchExtension> extensions = Lists.newArrayList();

    private final FeatureWithPropertiesCollection response = new FeatureWithPropertiesCollection();

    @Override
    public void addMetadata(String searchId, String searchTitle, String searchDescription,
            String openSearchDescriptionUrl, SearchContext context, OpenSearchConfiguration configuration,
            FacetPage<AbstractEntity> page, List<Link> links) {
        response.addProperty(ID, searchId);
        response.addProperty(TITLE, searchTitle);
        response.addProperty(DESCRIPTION, searchDescription);
        response.addProperty(TOTAL_RESULTS, page.getTotalElements());
        response.addProperty(START_INDEX, page.getNumber() * page.getSize());
        response.addProperty(ITEMS_PER_PAGE, context.getPageable().getPageSize());

        Query query = new Query();
        context.getQueryParams().forEach((name, values) -> values.forEach(value -> query.addFilter(name, value)));
        response.addProperty(QUERY, query);
        response.addProperty(LINKS, links.stream().map(l -> GeoJsonLink.build(l, APPLICATION_GEO_JSON_VALUE))
                .collect(Collectors.toList()));
    }

    @Override
    public void addEntity(AbstractEntity entity, List<OpenSearchParameterConfiguration> paramConfigurations,
            List<Link> entityLinks) {
        Feature feature = new Feature();
        feature.setId(entity.getIpId().toString());
        // All links are alternate links here in geo json format
        // Other types like icon or enclosure are handle in extensions (example : media)
        String title = String.format("GeoJson link for %s", entity.getIpId());
        feature.addProperty(LINKS,
                            entityLinks.stream()
                                    .map(l -> GeoJsonLink.build(l, "alternate", title, APPLICATION_GEO_JSON_VALUE))
                                    .collect(Collectors.toList()));
        feature.addProperty(TITLE, entity.getLabel());
        if (entity.getLastUpdate() != null) {
            feature.addProperty("updated", entity.getLastUpdate());
        } else {
            feature.addProperty("updated", entity.getCreationDate());
        }
        // Handle extensions
        for (IOpenSearchExtension extension : extensions) {
            if (extension.isActivated()) {
                extension.formatGeoJsonResponseFeature(entity, paramConfigurations, feature);
            }
        }
        response.add(feature);
    }

    @Override
    public void clear() {
        response.getFeatures().clear();
    }

    @Override
    public FeatureWithPropertiesCollection build() {
        return response;
    }

    @Override
    public void addExtension(IOpenSearchExtension configuration) {
        extensions.add(configuration);
    }

}
