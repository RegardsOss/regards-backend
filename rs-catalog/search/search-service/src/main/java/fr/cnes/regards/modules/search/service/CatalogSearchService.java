/*
 * Copyright 2017 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.search.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.StringJoiner;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.google.common.collect.Sets;
import com.google.common.reflect.TypeToken;

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.EntityOperationForbiddenException;
import fr.cnes.regards.framework.oais.urn.EntityType;
import fr.cnes.regards.framework.oais.urn.UniformResourceName;
import fr.cnes.regards.modules.entities.domain.AbstractEntity;
import fr.cnes.regards.modules.entities.domain.DataObject;
import fr.cnes.regards.modules.entities.domain.Dataset;
import fr.cnes.regards.modules.indexer.dao.FacetPage;
import fr.cnes.regards.modules.indexer.domain.IIndexable;
import fr.cnes.regards.modules.indexer.domain.JoinEntitySearchKey;
import fr.cnes.regards.modules.indexer.domain.SearchKey;
import fr.cnes.regards.modules.indexer.domain.SimpleSearchKey;
import fr.cnes.regards.modules.indexer.domain.criterion.ICriterion;
import fr.cnes.regards.modules.indexer.domain.facet.FacetType;
import fr.cnes.regards.modules.indexer.domain.summary.DocFilesSubSummary;
import fr.cnes.regards.modules.indexer.domain.summary.DocFilesSummary;
import fr.cnes.regards.modules.indexer.service.ISearchService;
import fr.cnes.regards.modules.indexer.service.Searches;
import fr.cnes.regards.modules.opensearch.service.IOpenSearchService;
import fr.cnes.regards.modules.opensearch.service.exception.OpenSearchParseException;
import fr.cnes.regards.modules.search.service.accessright.AccessRightFilterException;
import fr.cnes.regards.modules.search.service.accessright.IAccessRightFilter;

/**
 * Implementation of {@link ICatalogSearchService}
 * @author Xavier-Alexandre Brochard
 */
@Service
@MultitenantTransactional
public class CatalogSearchService implements ICatalogSearchService {

    /**
     * Class logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(CatalogSearchService.class);

    /**
     * Query parameter to limit join search
     */
    public static final String THRESHOLD_QUERY_PARAMETER = "threshold";

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
     * Facet converter
     */
    private final IFacetConverter facetConverter;

    /**
     * @param searchService Service perfoming the ElasticSearch search from criterions. Autowired by Spring. Must not be
     *            null.
     * @param openSearchService The OpenSearch service building {@link ICriterion} from a request string. Autowired by
     *            Spring. Must not be null.
     * @param accessRightFilter Service handling the access groups in criterion. Autowired by Spring. Must not be null.
     * @param facetConverter manage facet conversion
     */
    public CatalogSearchService(ISearchService searchService, IOpenSearchService openSearchService,
            IAccessRightFilter accessRightFilter, IFacetConverter facetConverter) {
        this.searchService = searchService;
        this.openSearchService = openSearchService;
        this.accessRightFilter = accessRightFilter;
        this.facetConverter = facetConverter;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <S, R extends IIndexable> FacetPage<R> search(Map<String, String> allParams, SearchKey<S, R> pSearchKey,
            String[] facets, Pageable pPageable) throws SearchException {
        try {
            SearchKey<?, ?> searchKey = pSearchKey;
            // Build criterion from query
            ICriterion criterion = openSearchService.parse(allParams);

            if (LOGGER.isDebugEnabled() && (allParams != null)) {
                for (Entry<String, String> osEntry : allParams.entrySet()) {
                    LOGGER.debug("Query param \"{}\" mapped to value \"{}\"", osEntry.getKey(), osEntry.getValue());
                }
            }

            // Apply security filter
            criterion = accessRightFilter.addAccessRights(criterion);

            // Build search facets from query facets
            Map<String, FacetType> searchFacets = facetConverter.convert(facets);
            // Optimisation: when searching for datasets via another searchType (ie searchKey is a
            // JoinEntitySearchKey<?, Dataset> without any criterion on searchType => just directly search
            // datasets (ie SimpleSearchKey<DataSet>)
            // This is correct because all
            if ((criterion == null) && (searchKey instanceof JoinEntitySearchKey)
                    && (TypeToken.of(searchKey.getResultClass()).getRawType() == Dataset.class)) {
                searchKey = Searches.onSingleEntity(searchKey.getSearchIndex(),
                                                    Searches.fromClass(searchKey.getResultClass()));
            }

            // Perform search
            if (searchKey instanceof SimpleSearchKey) {
                return searchService.search((SimpleSearchKey<R>) searchKey, pPageable, criterion, searchFacets);
            } else {
                // It may be necessary to filter returned objects (before pagination !!!) by user access groups to avoid
                // getting datasets on which user has no right
                final Set<String> accessGroups = accessRightFilter.getUserAccessGroups();
                if ((TypeToken.of(searchKey.getResultClass()).getRawType() == Dataset.class)
                        && (accessGroups != null)) { // accessGroups null means superuser
                    Predicate<Dataset> datasetGroupAccessFilter = ds -> !Sets.intersection(ds.getGroups(), accessGroups)
                            .isEmpty();
                    return searchService.search((JoinEntitySearchKey<S, R>) searchKey, pPageable, criterion,
                                                (Predicate<R>) datasetGroupAccessFilter);
                } else {
                    return searchService.search((JoinEntitySearchKey<S, R>) searchKey, pPageable, criterion);
                }
            }

        } catch (OpenSearchParseException e) {
            String message = "No query parameter";
            if (allParams != null) {
                StringJoiner sj = new StringJoiner("&");
                allParams.forEach((key, value) -> sj.add(key + "=" + value));
                message = sj.toString();
            }
            throw new SearchException(message, e);
        } catch (AccessRightFilterException e) {
            LOGGER.debug("Falling back to empty page", e);
            return new FacetPage<>(new ArrayList<>(), null);
        }
    }

    @Override
    public <E extends AbstractEntity> E get(UniformResourceName urn)
            throws EntityOperationForbiddenException, EntityNotFoundException {
        Set<String> userGroups = null;
        E entity = searchService.get(urn);
        if (entity == null) {
            throw new EntityNotFoundException(urn.toString(), AbstractEntity.class);
        }
        try {
            userGroups = accessRightFilter.getUserAccessGroups();
        } catch (AccessRightFilterException e) {
            throw new EntityOperationForbiddenException(urn.toString(), entity.getClass(),
                    "You do not have access to this " + entity.getClass().getSimpleName());
        }

        if (userGroups == null) {
            // According to the doc it means that current user is an admin, admins always has rights to access entities!
            return entity;
        }
        // To know if we have access to the entity, lets intersect the entity groups with user group
        if (!Sets.intersection(entity.getGroups(), userGroups).isEmpty()) {
            // then we have access
            return entity;
        }
        throw new EntityOperationForbiddenException(urn.toString(), entity.getClass(),
                "You do not have access to this " + entity.getClass().getSimpleName());
    }

    @Override
    public DocFilesSummary computeDatasetsSummary(Map<String, String> allParams, SimpleSearchKey<DataObject> searchKey,
            String datasetIpId, String[] fileTypes) throws SearchException {
        try {
            // Build criterion from query
            ICriterion criterion = openSearchService.parse(allParams);

            if (LOGGER.isDebugEnabled() && (allParams != null)) {
                for (Entry<String, String> osEntry : allParams.entrySet()) {
                    LOGGER.debug("Query param \"{}\" mapped to value \"{}\"", osEntry.getKey(), osEntry.getValue());
                }
            }

            // Apply security filter (ie user groups)
            criterion = accessRightFilter.addAccessRights(criterion);

            // Perform compute
            DocFilesSummary summary = searchService.computeDataFilesSummary(searchKey, criterion, "tags", fileTypes);

            // It is necessary to filter sub summaries first to keep only datasets and seconds to keep only datasets
            // on which user has right
            final Set<String> accessGroups = accessRightFilter.getUserAccessGroups();

            // Retrieve all datasets that permit data objects retrieval (ie groups with FULL_ACCESS privilege)
            Page<Dataset> page = searchService
                    .search(Searches.onSingleEntity(searchKey.getSearchIndex(), EntityType.DATASET), Integer.MAX_VALUE,
                            ICriterion.in("metadata.dataObjectsGroups",
                                          accessGroups.toArray(new String[accessGroups.size()])));
            Set<String> datasetIpids = page.getContent().stream().map(Dataset::getIpId)
                    .map(UniformResourceName::toString).collect(Collectors.toSet());
            // If summary is restricted to a specified datasetIpId, it must be taken into account
            if (datasetIpId != null) {
                if (datasetIpids.contains(datasetIpId)) {
                    datasetIpids = Collections.singleton(datasetIpId);
                } else { // no dataset => summary contains normaly only 0 values as total
                    // we just need to clear map of sub summaries
                    summary.getSubSummariesMap().clear();
                }
            }

            for (Iterator<Entry<String, DocFilesSubSummary>> i = summary.getSubSummariesMap().entrySet().iterator(); i
                    .hasNext();) {
                Entry<String, DocFilesSubSummary> entry = i.next();
                // Remove it if subSummary discriminant isn't a dataset or isn't a dataset on which data can be
                // retrieved for current user
                if (!datasetIpids.contains(entry.getKey())) {
                    i.remove();
                }
            }
            return summary;
        } catch (OpenSearchParseException e) {
            String message = "No query parameter";
            if (allParams != null) {
                StringJoiner sj = new StringJoiner("&");
                allParams.forEach((key, value) -> sj.add(key + "=" + value));
                message = sj.toString();
            }
            throw new SearchException(message, e);
        } catch (AccessRightFilterException e) {
            LOGGER.debug("Falling back to empty summary", e);
            return new DocFilesSummary();
        }
    }
}
