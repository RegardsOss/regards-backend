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
package fr.cnes.regards.modules.search.service.engine.plugin.opensearch.formatter.geojson;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.compress.utils.Lists;
import org.springframework.hateoas.Link;

import fr.cnes.regards.framework.geojson.Feature;
import fr.cnes.regards.framework.geojson.FeatureWithPropertiesCollection;
import fr.cnes.regards.framework.geojson.GeoJsonLink;
import fr.cnes.regards.framework.geojson.GeoJsonMediaType;
import fr.cnes.regards.framework.geojson.Query;
import fr.cnes.regards.modules.dam.domain.entities.feature.EntityFeature;
import fr.cnes.regards.modules.indexer.dao.FacetPage;
import fr.cnes.regards.modules.search.domain.plugin.SearchContext;
import fr.cnes.regards.modules.search.service.engine.plugin.opensearch.Configuration;
import fr.cnes.regards.modules.search.service.engine.plugin.opensearch.EngineConfiguration;
import fr.cnes.regards.modules.search.service.engine.plugin.opensearch.ParameterConfiguration;
import fr.cnes.regards.modules.search.service.engine.plugin.opensearch.extension.IOpenSearchExtension;
import fr.cnes.regards.modules.search.service.engine.plugin.opensearch.formatter.IResponseBuilder;

/**
 * Build open search responses in GEO+Json format by creating a {@link FeatureWithPropertiesCollection} handling :<ul>
 * <li>Opensearch parameters extension</li>
 * <li>{@link IOpenSearchExtension}s for additional extensions like geo+time or media.</li>
 * </ul>
 * @author Sébastien Binda
 */
public class GeojsonResponseBuilder implements IResponseBuilder<FeatureWithPropertiesCollection> {

    /**
     * List of opensearch extensions to apply to the current geojson response FeatureCollection builder
     */
    private final List<IOpenSearchExtension> extensions = Lists.newArrayList();

    /**
     * {@link FeatureWithPropertiesCollection} GEO+Json response
     */
    private final FeatureWithPropertiesCollection response = new FeatureWithPropertiesCollection();

    private final String token;

    public GeojsonResponseBuilder(String token) {
        super();
        this.token = token;
    }

    @Override
    public void addMetadata(String searchId, EngineConfiguration engineConf, String openSearchDescriptionUrl,
            SearchContext context, Configuration configuration, FacetPage<EntityFeature> page, List<Link> links) {
        response.setId(searchId);
        response.setTitle(engineConf.getSearchTitle());
        response.setDescription(engineConf.getSearchDescription());
        response.setPaginationInfos(page.getTotalElements(), page.getNumber() * page.getSize(), page.getSize());
        Query query = new Query();
        context.getQueryParams().forEach((name, values) -> values.forEach(value -> query.addFilter(name, value)));
        query.addFilter("token", token);
        response.setQuery(query);
        response.setLinks(links.stream()
                .map(l -> GeoJsonLinkBuilder.build(l, GeoJsonMediaType.APPLICATION_GEOJSON_VALUE, token))
                .collect(Collectors.toList()));
    }

    @Override
    public void addEntity(EntityFeature entity, Optional<OffsetDateTime> entityLastUpdate,
            List<ParameterConfiguration> paramConfigurations, List<Link> entityLinks) {
        if (entity != null) {
            Feature feature = new Feature();
            feature.setId(entity.getId().toString());
            // All links are alternate links here in geo json format
            // Other types like icon or enclosure are handle in extensions (example : media)
            String title = String.format("GeoJson link for %s", entity.getId());
            feature.setLinks(entityLinks.stream()
                    .map(l -> GeoJsonLinkBuilder.build(l, GeoJsonLink.LINK_ALTERNATE_REL, title,
                                                       GeoJsonMediaType.APPLICATION_GEOJSON_VALUE, token))
                    .collect(Collectors.toList()));
            feature.setTitle(entity.getLabel());
            feature.addProperty("providerId", entity.getProviderId());
            if (entityLastUpdate.isPresent()) {
                feature.setUpdated(entityLastUpdate.get());
            }
            // Handle extensions
            for (IOpenSearchExtension extension : extensions) {
                if (extension.isActivated()) {
                    extension.formatGeoJsonResponseFeature(entity, paramConfigurations, feature, token);
                }
            }
            response.add(feature);
        }
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
