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

import org.apache.commons.compress.utils.Lists;

import fr.cnes.regards.framework.geojson.Feature;
import fr.cnes.regards.framework.geojson.FeatureWithPropertiesCollection;
import fr.cnes.regards.modules.entities.domain.AbstractEntity;
import fr.cnes.regards.modules.indexer.dao.FacetPage;
import fr.cnes.regards.modules.search.domain.plugin.SearchContext;
import fr.cnes.regards.modules.search.rest.engine.plugin.opensearch.OpenSearchConfiguration;
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

    private static final String ID = "id";

    private static final String TITLE = "title";

    private static final String TOTAL_RESULTS = "totalResults";

    private static final String START_INDEX = "startIndex";

    private static final String ITEMS_PER_PAGE = "itemsPerPage";

    private static final String DESCRIPTION = "description";

    private static final String QUERY = "query";

    private final List<IOpenSearchExtension> extensions = Lists.newArrayList();

    private final FeatureWithPropertiesCollection response = new FeatureWithPropertiesCollection();

    @Override
    public void addMetadata(String searchId, String searchTitle, String searchDescription,
            String openSearchDescriptionUrl, SearchContext context, OpenSearchConfiguration configuration,
            FacetPage<AbstractEntity> page) {
        response.addProperty(ID, searchId);
        response.addProperty(TITLE, searchTitle);
        response.addProperty(DESCRIPTION, searchDescription);
        response.addProperty(TOTAL_RESULTS, page.getTotalElements());
        response.addProperty(START_INDEX, page.getNumber() * page.getSize());
        response.addProperty(ITEMS_PER_PAGE, context.getPageable().getPageSize());

        Query query = new Query();
        context.getQueryParams().forEach((name, values) -> values.forEach(value -> query.addFilter(name, value)));
        response.addProperty(QUERY, query);

        // TODO handle links
    }

    @Override
    public void addEntity(AbstractEntity entity) {
        Feature feature = new Feature();
        feature.setId(entity.getIpId().toString());

        // Handle extensions
        for (IOpenSearchExtension extension : extensions) {
            if (extension.isActivated()) {
                extension.applyExtensionToGeoJsonFeature(entity, feature);
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
