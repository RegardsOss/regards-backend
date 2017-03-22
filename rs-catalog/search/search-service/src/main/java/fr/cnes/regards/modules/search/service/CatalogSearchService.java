package fr.cnes.regards.modules.search.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.lucene.queryparser.flexible.core.QueryNodeException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.google.common.collect.ImmutableMap;

import fr.cnes.regards.framework.module.rest.exception.SearchException;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.modules.entities.domain.AbstractEntity;
import fr.cnes.regards.modules.entities.domain.Collection;
import fr.cnes.regards.modules.entities.domain.DataObject;
import fr.cnes.regards.modules.entities.domain.Dataset;
import fr.cnes.regards.modules.entities.domain.Document;
import fr.cnes.regards.modules.indexer.domain.IIndexable;
import fr.cnes.regards.modules.indexer.domain.SearchKey;
import fr.cnes.regards.modules.indexer.domain.criterion.ICriterion;
import fr.cnes.regards.modules.indexer.domain.facet.FacetType;
import fr.cnes.regards.modules.indexer.service.ISearchService;
import fr.cnes.regards.modules.models.domain.EntityType;
import fr.cnes.regards.modules.search.domain.SearchType;
import fr.cnes.regards.modules.search.service.queryparser.RegardsQueryParser;

/**
 * Implementation of {@link ICatalogSearchService}
 *
 * @author Xavier-Alexandre Brochard
 */
@Service
public class CatalogSearchService implements ICatalogSearchService {

    /**
     * Service perfoming the ElasticSearch search from criterions
     */
    private final ISearchService searchService;

    /**
     * The custom OpenSearch query parser building {@link ICriterion} from tu string query
     */
    private final RegardsQueryParser queryParser;

    /**
     * Get current tenant at runtime and allows tenant forcing
     */
    private final IRuntimeTenantResolver runtimeTenantResolver;

    /**
     * Map associating a {@link SearchType} and the corresponding class
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static final ImmutableMap<String, Class<?>> TO_RESULT_CLASS = new ImmutableMap.Builder()
            .put(EntityType.COLLECTION, Collection.class).put(EntityType.DATASET, Dataset.class)
            .put(EntityType.DATA, DataObject.class).put(EntityType.DOCUMENT, Document.class).build();

    /**
     * Map associating an {@link EntityType} and the corresponding class
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static final ImmutableMap<Class<?>, Optional<String>> TO_ENTITY_TYPE = new ImmutableMap.Builder()
            .put(AbstractEntity.class, Optional.empty()).put(Collection.class, Optional.of(EntityType.COLLECTION))
            .put(Dataset.class, Optional.of(EntityType.DATASET)).put(DataObject.class, Optional.of(EntityType.DATA))
            .put(Document.class, Optional.of(EntityType.DOCUMENT)).build();

    /**
     * @param pSearchService
     * @param pQueryParser
     * @param pRuntimeTenantResolver
     */
    public CatalogSearchService(ISearchService pSearchService, RegardsQueryParser pQueryParser,
            IRuntimeTenantResolver pRuntimeTenantResolver) {
        super();
        searchService = pSearchService;
        queryParser = pQueryParser;
        runtimeTenantResolver = pRuntimeTenantResolver;
    }

    /*
     * (non-Javadoc)
     *
     * @see fr.cnes.regards.modules.search.service.ICatalogSearchService#search(java.lang.String,
     * org.elasticsearch.action.search.SearchType, java.lang.Class, java.util.List,
     * org.springframework.data.domain.Pageable, org.springframework.data.web.PagedResourcesAssembler)
     */
    @Override
    // public <T extends IIndexable> PagedResources<Resource<T>> search(String pQ, SearchType pSearchType,
    public <T extends IIndexable> Page<T> search(String pQ, EntityType pSearchType, Class<T> pResultClass,
            List<String> pFacets, Pageable pPageable) throws SearchException {
        try {
            // Build criterion from query
            ICriterion criterion;
            criterion = queryParser.parse(pQ);

            // Apply security filters
            // criterion = accessRightFilter.removeGroupFilter(criterion);
            // criterion = accessRightFilter.addGroupFilter(criterion);
            // criterion = accessRightFilter.addAccessRightsFilter(criterion);

            // Perform the search
            Page<T> entities;
            String searchType = null;
            if (pSearchType != null) {
                searchType = pSearchType.toString();
            }
            SearchKey<T> searchKey = new SearchKey<>(runtimeTenantResolver.getTenant(), searchType, pResultClass);
            if (!TO_ENTITY_TYPE.get(pResultClass).equals(Optional.ofNullable(pSearchType))) {
                entities = searchService.searchAndReturnJoinedEntities(searchKey, pPageable.getPageSize(), criterion);
            } else {
                LinkedHashMap<String, Boolean> ascSortMap = null;
                ImmutableMap.Builder<String, FacetType> facetMapBuilder = new ImmutableMap.Builder<>();
                facetMapBuilder.put("properties.tags", FacetType.STRING);
                facetMapBuilder.put("properties.ints", FacetType.NUMERIC);
                Map<String, FacetType> facetsMap = facetMapBuilder.build(); // Use pFacets
                entities = searchService.search(searchKey, pPageable, criterion, facetsMap, ascSortMap);
            }

            // Format output response
            // entities = converter.convert(entities);

            // Return
            return entities;
        } catch (QueryNodeException e) {
            throw new SearchException(pQ, e);
        }
    }

}
