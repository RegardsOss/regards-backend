package fr.cnes.regards.modules.search.service;

import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.lucene.queryparser.flexible.core.QueryNodeException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import fr.cnes.regards.framework.module.rest.exception.SearchException;
import fr.cnes.regards.modules.indexer.domain.IIndexable;
import fr.cnes.regards.modules.indexer.domain.JoinEntitySearchKey;
import fr.cnes.regards.modules.indexer.domain.SearchKey;
import fr.cnes.regards.modules.indexer.domain.SimpleSearchKey;
import fr.cnes.regards.modules.indexer.domain.criterion.ICriterion;
import fr.cnes.regards.modules.indexer.domain.facet.FacetType;
import fr.cnes.regards.modules.indexer.service.ISearchService;
import fr.cnes.regards.modules.search.service.accessright.IAccessRightFilter;
import fr.cnes.regards.modules.search.service.queryparser.RegardsQueryParser;

/**
 * Implementation of {@link ICatalogSearchService}
 *
 * @author Xavier-Alexandre Brochard
 */
@Service
public class CatalogSearchService implements ICatalogSearchService {

    /**
     * Service perfoming the ElasticSearch search from criterions. Autowired.
     */
    private final ISearchService searchService;

    /**
     * The custom OpenSearch query parser building {@link ICriterion} from a string query. Autowired.
     */
    private final RegardsQueryParser queryParser;

    /**
     * Service handling the access groups in criterion
     */
    private final IAccessRightFilter accessRightFilter;

    /**
     * @param pSearchService
     * @param pQueryParser
     * @param pAccessRightFilter
     */
    public CatalogSearchService(ISearchService pSearchService, RegardsQueryParser pQueryParser,
            IAccessRightFilter pAccessRightFilter) {
        super();
        searchService = pSearchService;
        queryParser = pQueryParser;
        accessRightFilter = pAccessRightFilter;
    }

    /*
     * (non-Javadoc)
     *
     * @see fr.cnes.regards.modules.search.service.ICatalogSearchService#search(java.lang.String,
     * org.elasticsearch.action.search.SearchType, java.lang.Class, java.util.List,
     * org.springframework.data.domain.Pageable, org.springframework.data.web.PagedResourcesAssembler)
     */
    @SuppressWarnings("unchecked")
    @Override
    public <S, R extends IIndexable> Page<R> search(String pQ, SearchKey<S, R> pSearchKey,
            Map<String, FacetType> pFacets, Pageable pPageable) throws SearchException {
        try {
            // Build criterion from query
            ICriterion criterion = queryParser.parse(pQ);

            // Apply security filter
            criterion = accessRightFilter.addUserGroups(criterion);

            // Sort
            LinkedHashMap<String, Boolean> ascSortMap = null;

            // Perform search
            if (pSearchKey instanceof SimpleSearchKey) {
                return searchService.search((SimpleSearchKey<R>) pSearchKey, pPageable, criterion, pFacets, ascSortMap);
            } else {
                return searchService.search((JoinEntitySearchKey<S, R>) pSearchKey, pPageable, criterion);
            }

        } catch (QueryNodeException e) {
            throw new SearchException(pQ, e);
        }
    }

}
