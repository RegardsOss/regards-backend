package fr.cnes.regards.modules.search.rest.engine.plugin.opensearch.geojson;

import java.util.List;

import org.apache.commons.compress.utils.Lists;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import fr.cnes.regards.framework.geojson.Feature;
import fr.cnes.regards.framework.geojson.FeatureWithPropertiesCollection;
import fr.cnes.regards.modules.entities.domain.AbstractEntity;
import fr.cnes.regards.modules.indexer.dao.FacetPage;
import fr.cnes.regards.modules.search.domain.plugin.SearchContext;
import fr.cnes.regards.modules.search.rest.engine.plugin.opensearch.IOpenSearchResponseBuilder;
import fr.cnes.regards.modules.search.rest.engine.plugin.opensearch.extension.IOpenSearchExtension;

@Component
public class GeojsonResponseBuilder implements IOpenSearchResponseBuilder<FeatureWithPropertiesCollection> {

    private static final String ID = "id";

    private static final String TITLE = "title";

    private static final String TOTAL_RESULTS = "totalResults";

    private static final String START_INDEX = "startIndex";

    private static final String ITEMS_PER_PAGE = "itemsPerPage";

    private static final String DESCRIPTION = "description";

    private final List<IOpenSearchExtension> extensions = Lists.newArrayList();

    private final FeatureWithPropertiesCollection response = new FeatureWithPropertiesCollection();

    @Override
    public void addMetadata(String searchId, String searchTitle, String searchDescription,
            String openSearchDescriptionUrl, SearchContext context, FacetPage<AbstractEntity> page) {
        response.addProperty(ID, searchId);
        response.addProperty(TITLE, searchTitle);
        response.addProperty(DESCRIPTION, searchDescription);
        response.addProperty(TOTAL_RESULTS, page.getTotalElements());
        response.addProperty(START_INDEX, page.getNumber() * page.getSize());
        response.addProperty(ITEMS_PER_PAGE, context.getPageable().getPageSize());

        // TODO handle query

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
    public boolean supports(List<MediaType> mediaTypes) {
        // TODO Geo json ?
        if (mediaTypes.contains(MediaType.APPLICATION_JSON)) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void addExtension(IOpenSearchExtension configuration) {
        extensions.add(configuration);
    }

}
