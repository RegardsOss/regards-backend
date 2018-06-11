package fr.cnes.regards.modules.search.rest.engine.plugin.opensearch;

import java.util.List;

import org.springframework.http.MediaType;

import fr.cnes.regards.modules.entities.domain.AbstractEntity;
import fr.cnes.regards.modules.indexer.dao.FacetPage;
import fr.cnes.regards.modules.search.domain.plugin.SearchContext;
import fr.cnes.regards.modules.search.rest.engine.plugin.opensearch.extension.IOpenSearchExtension;

public interface IOpenSearchResponseBuilder<R> {

    void addMetadata(String searchId, String searchTitle, String searchDescription, String openSearchDescriptionUrl,
            SearchContext context, FacetPage<AbstractEntity> page);

    void addEntity(AbstractEntity entity);

    void clear();

    R build();

    boolean supports(List<MediaType> mediaTypes);

    void addExtension(IOpenSearchExtension configuration);

}
