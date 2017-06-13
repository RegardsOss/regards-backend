package fr.cnes.regards.modules.search.service;

import java.util.Map;
import java.util.Map.Entry;
import java.util.StringJoiner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import com.google.common.reflect.TypeToken;
import fr.cnes.regards.framework.module.rest.exception.SearchException;
import fr.cnes.regards.modules.entities.domain.Dataset;
import fr.cnes.regards.modules.indexer.dao.FacetPage;
import fr.cnes.regards.modules.indexer.domain.IIndexable;
import fr.cnes.regards.modules.indexer.domain.JoinEntitySearchKey;
import fr.cnes.regards.modules.indexer.domain.SearchKey;
import fr.cnes.regards.modules.indexer.domain.SimpleSearchKey;
import fr.cnes.regards.modules.indexer.domain.criterion.ICriterion;
import fr.cnes.regards.modules.indexer.domain.facet.FacetType;
import fr.cnes.regards.modules.indexer.service.ISearchService;
import fr.cnes.regards.modules.indexer.service.Searches;
import fr.cnes.regards.modules.models.domain.EntityType;
import fr.cnes.regards.modules.opensearch.service.IOpenSearchService;
import fr.cnes.regards.modules.opensearch.service.exception.OpenSearchParseException;
import fr.cnes.regards.modules.search.service.accessright.IAccessRightFilter;

/**
 * Implementation of {@link ICatalogSearchService}
 *
 * @author Xavier-Alexandre Brochard
 */
@Service
public class CatalogSearchService implements ICatalogSearchService {

    /**
     * Class logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(CatalogSearchService.class);

    /**
     * Service perfoming the ElasticSearch search from criterions. Autowired.
     */
    private final ISearchService searchService;

    /**
     * The OpenSearch service building {@link ICriterion} from a request string. Autowired by Spring.
     */
    private final IOpenSearchService openSearchService;

    /**
     * Service handling the access groups in criterion. Autowired by Spring.
     */
    private final IAccessRightFilter accessRightFilter;

    /**
     * @param pSearchService Service perfoming the ElasticSearch search from criterions. Autowired by Spring. Must not be null.
     * @param pOpenSearchService The OpenSearch service building {@link ICriterion} from a request string. Autowired by Spring. Must not be null.
     * @param pAccessRightFilter Service handling the access groups in criterion. Autowired by Spring. Must not be null.
     */
    public CatalogSearchService(ISearchService pSearchService, IOpenSearchService pOpenSearchService,
            IAccessRightFilter pAccessRightFilter) {
        super();
        Assert.notNull(pSearchService);
        Assert.notNull(pOpenSearchService);
        Assert.notNull(pAccessRightFilter);
        searchService = pSearchService;
        openSearchService = pOpenSearchService;
        accessRightFilter = pAccessRightFilter;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <S, R extends IIndexable> FacetPage<R> search(Map<String, String> pOpensearchParams,
            SearchKey<S, R> pSearchKey, Map<String, FacetType> pFacets, Pageable pPageable) throws SearchException {
        try {
            SearchKey<?, ?> searchKey = pSearchKey;
            // Build criterion from query
            ICriterion criterion = openSearchService.parse(pOpensearchParams);

            if (LOGGER.isDebugEnabled() && (pOpensearchParams != null)) {
                for (Entry<String, String> osEntry : pOpensearchParams.entrySet()) {
                    LOGGER.debug("OpenSearch entry key \"{}\" mapped to query \"{}\"", osEntry.getKey(),
                                 osEntry.getValue());
                }
            }

            // Optimisation: when searching for datasets via another searchType (ie searchKey is a
            // JoinEntitySearchKey<?, Dataset> without any criterion on searchType => just directly search
            // datasets (ie SimpleSearchKey<DataSet>)
            // This is correct because all
            if ((criterion == null) && (searchKey instanceof JoinEntitySearchKey) && (
                    TypeToken.of(searchKey.getResultClass()).getRawType() == Dataset.class)) {
                searchKey = Searches.onSingleEntity(searchKey.getSearchIndex(), Searches.fromClass(searchKey.getResultClass()));
            }

            // Apply security filter
            criterion = accessRightFilter.addUserGroups(criterion);

            // Perform search
            if (searchKey instanceof SimpleSearchKey) {
                return searchService.search((SimpleSearchKey<R>) searchKey, pPageable, criterion, pFacets);
            } else {
                return searchService.search((JoinEntitySearchKey<S, R>) searchKey, pPageable, criterion);
            }

        } catch (OpenSearchParseException e) {
            StringJoiner sj = new StringJoiner("&");
            pOpensearchParams.forEach((key, value) -> sj.add(key + "=" + value));
            throw new SearchException(sj.toString(), e);
        }
    }

}
