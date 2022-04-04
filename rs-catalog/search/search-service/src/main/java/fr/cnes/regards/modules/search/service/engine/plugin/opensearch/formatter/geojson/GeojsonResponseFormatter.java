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

import fr.cnes.regards.framework.geojson.*;
import fr.cnes.regards.framework.urn.UniformResourceName;
import fr.cnes.regards.modules.dam.domain.entities.feature.EntityFeature;
import fr.cnes.regards.modules.indexer.domain.DataFile;
import fr.cnes.regards.modules.search.domain.plugin.SearchContext;
import fr.cnes.regards.modules.search.service.engine.plugin.opensearch.ParameterConfiguration;
import fr.cnes.regards.modules.search.service.engine.plugin.opensearch.extension.IOpenSearchExtension;
import fr.cnes.regards.modules.search.service.engine.plugin.opensearch.formatter.AbstractResponseFormatter;
import org.springframework.hateoas.Link;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Build open search responses in GEO+Json format by creating a {@link FeatureWithPropertiesCollection} handling :<ul>
 * <li>Opensearch parameters extension</li>
 * <li>{@link IOpenSearchExtension}s for additional extensions like geo+time or media.</li>
 * </ul>
 *
 * @author Sébastien Binda
 */
public class GeojsonResponseFormatter extends AbstractResponseFormatter<Feature, FeatureWithPropertiesCollection> {

    public GeojsonResponseFormatter(String scope) {
        super(scope);
    }

    /**
     * {@link FeatureWithPropertiesCollection} GEO+Json response
     */
    @Override
    protected FeatureWithPropertiesCollection buildResponse() {
        return new FeatureWithPropertiesCollection();
    }

    @Override
    protected void addResponseLanguage(String language) {
        // not supported
    }

    @Override
    protected void addResponseUpdated() {
        // not supported
    }

    @Override
    protected void addResponseAuthor(String contact, String attribution) {
        // not supported
    }

    @Override
    protected void addResponseQuery(SearchContext context, String role) {
        Query query = new Query();
        context.getQueryParams().forEach((name, values) -> values.forEach(value -> query.addFilter(name, value)));
        response.setQuery(query);
    }

    @Override
    protected void addResponseLinks(List<Link> links) {
        response.setLinks(links.stream()
                              .map(link -> new GeoJsonLink(link, GeoJsonMediaType.APPLICATION_GEOJSON_VALUE))
                              .collect(Collectors.toList()));
    }

    @Override
    protected void addResponseOpenSearchDescription(String openSearchDescriptionUrl) {
        // do nothing
    }

    @Override
    protected void addResponsePaginationInfos(long totalResults, long startIndex, int itemsPerPage) {
        response.setPaginationInfos(totalResults, startIndex, itemsPerPage);
    }

    @Override
    protected void addResponseDescription(String description) {
        response.setDescription(description);
    }

    @Override
    protected void addResponseTitle(String title) {
        response.setTitle(title);
    }

    @Override
    protected void addResponseId(String searchId) {
        response.setId(searchId);
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
    protected void updateEntityWithExtension(IOpenSearchExtension extension,
                                             EntityFeature entity,
                                             List<ParameterConfiguration> paramConfigurations) {
        extension.formatGeoJsonResponseFeature(entity, paramConfigurations, this.feature, this.scope);
    }

    @Override
    protected void addFeatureServices(DataFile firstRawData) {
        feature.addProperty("services", GeojsonFeatureServiceDownloadBuilder.buildGeojsonServices(firstRawData));
    }

    @Override
    protected void addFeatureUpdated(OffsetDateTime date) {
        this.feature.setUpdated(date);
    }

    @Override
    protected void addFeatureProviderId(String providerId) {
        this.feature.addProperty("providerId", providerId);
    }

    @Override
    protected void addFeatureTitle(String title) {
        this.feature.setTitle(title);
    }

    @Override
    protected void addFeatureLinks(List<Link> entityLinks) {
        // All links are alternate links here in geo json format
        // Other types like icon or enclosure are handle in extensions (example : media)
        String title = String.format("GeoJson link for %s", feature.getId());
        this.feature.setLinks(entityLinks.stream()
                                  .map(link -> new GeoJsonLink(link,
                                                            GeoJsonLink.LINK_ALTERNATE_REL,
                                                            title,
                                                            GeoJsonMediaType.APPLICATION_GEOJSON_VALUE))
                                  .collect(Collectors.toList()));
    }

    @Override
    protected void addFeatureId(UniformResourceName id) {
        this.feature.setId(id.toString());
    }

    @Override
    protected Feature buildFeature() {
        return new Feature();
    }

    @Override
    protected void addToResponse(Feature entity) {
        response.add(entity);
    }
}
