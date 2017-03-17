package fr.cnes.regards.modules.search.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.lucene.queryparser.flexible.core.QueryNodeException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.stereotype.Service;

import com.google.common.collect.ImmutableMap;

import fr.cnes.regards.framework.module.rest.exception.SearchException;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.modules.crawler.domain.IIndexable;
import fr.cnes.regards.modules.crawler.domain.SearchKey;
import fr.cnes.regards.modules.crawler.domain.criterion.ICriterion;
import fr.cnes.regards.modules.crawler.domain.facet.FacetType;
import fr.cnes.regards.modules.entities.domain.AbstractEntity;
import fr.cnes.regards.modules.entities.domain.Collection;
import fr.cnes.regards.modules.entities.domain.DataObject;
import fr.cnes.regards.modules.entities.domain.Dataset;
import fr.cnes.regards.modules.entities.domain.Document;
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
            .put(SearchType.ALL, AbstractEntity.class).put(SearchType.COLLECTION, Collection.class)
            .put(SearchType.DATASET, Dataset.class).put(SearchType.DATAOBJECT, DataObject.class)
            .put(SearchType.DOCUMENT, Document.class).build();

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
    public <T extends IIndexable> Page<T> search(String pQ, SearchType pSearchType, Class<T> pResultClass,
            List<String> pFacets, Pageable pPageable, PagedResourcesAssembler<T> pAssembler) throws SearchException {
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
            SearchKey<T> searchKey = new SearchKey<>(runtimeTenantResolver.getTenant(), pSearchType.toString(),
                    pResultClass);
            if (!TO_RESULT_CLASS.get(pSearchType).equals(pResultClass)) {
                entities = searchService.searchAndReturnJoinedEntities(searchKey, pPageable.getPageSize(), criterion);
            } else {
                LinkedHashMap<String, Boolean> ascSortMap = null;
                Map<String, FacetType> facetsMap = null; // Use pFacets
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
