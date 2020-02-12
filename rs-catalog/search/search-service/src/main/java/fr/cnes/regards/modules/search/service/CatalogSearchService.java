/*
 * Copyright 2017-2019 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.StringJoiner;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.metrics.stats.ParsedStats;
import org.elasticsearch.search.aggregations.metrics.stats.StatsAggregationBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.MultiValueMap;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.reflect.TypeToken;

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.EntityOperationForbiddenException;
import fr.cnes.regards.framework.oais.urn.OaisUniformResourceName;
import fr.cnes.regards.framework.urn.DataType;
import fr.cnes.regards.framework.urn.EntityType;
import fr.cnes.regards.modules.dam.domain.entities.AbstractEntity;
import fr.cnes.regards.modules.dam.domain.entities.DataObject;
import fr.cnes.regards.modules.dam.domain.entities.Dataset;
import fr.cnes.regards.modules.dam.domain.entities.StaticProperties;
import fr.cnes.regards.modules.dam.domain.entities.criterion.IFeatureCriterion;
import fr.cnes.regards.modules.indexer.dao.FacetPage;
import fr.cnes.regards.modules.indexer.domain.IIndexable;
import fr.cnes.regards.modules.indexer.domain.JoinEntitySearchKey;
import fr.cnes.regards.modules.indexer.domain.SearchKey;
import fr.cnes.regards.modules.indexer.domain.SimpleSearchKey;
import fr.cnes.regards.modules.indexer.domain.aggregation.QueryableAttribute;
import fr.cnes.regards.modules.indexer.domain.criterion.ICriterion;
import fr.cnes.regards.modules.indexer.domain.facet.FacetType;
import fr.cnes.regards.modules.indexer.domain.facet.IFacet;
import fr.cnes.regards.modules.indexer.domain.summary.DocFilesSubSummary;
import fr.cnes.regards.modules.indexer.domain.summary.DocFilesSummary;
import fr.cnes.regards.modules.indexer.service.ISearchService;
import fr.cnes.regards.modules.indexer.service.Searches;
import fr.cnes.regards.modules.model.domain.attributes.AttributeModel;
import fr.cnes.regards.modules.opensearch.service.IOpenSearchService;
import fr.cnes.regards.modules.opensearch.service.cache.attributemodel.IAttributeFinder;
import fr.cnes.regards.modules.opensearch.service.exception.OpenSearchParseException;
import fr.cnes.regards.modules.opensearch.service.exception.OpenSearchUnknownParameter;
import fr.cnes.regards.modules.search.domain.PropertyBound;
import fr.cnes.regards.modules.search.domain.plugin.SearchType;
import fr.cnes.regards.modules.search.service.accessright.AccessRightFilterException;
import fr.cnes.regards.modules.search.service.accessright.IAccessRightFilter;

/**
 * Implementation of {@link ICatalogSearchService}
 * @author Xavier-Alexandre Brochard
 */
@Service
@MultitenantTransactional
public class CatalogSearchService implements ICatalogSearchService {

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
     * Facet converter
     */
    private final IFacetConverter facetConverter;

    /**
     * Pageable converter
     */
    private final IPageableConverter pageableConverter;

    @Autowired
    private IAttributeFinder finder;

    /**
     * @param searchService Service perfoming the ElasticSearch search from criterions. Autowired by Spring. Must not be
     *            null.
     * @param openSearchService The OpenSearch service building {@link ICriterion} from a request string. Autowired by
     *            Spring. Must not be null.
     * @param accessRightFilter Service handling the access groups in criterion. Autowired by Spring. Must not be null.
     * @param facetConverter manage facet conversion
     */
    public CatalogSearchService(ISearchService searchService, IOpenSearchService openSearchService,
            IAccessRightFilter accessRightFilter, IFacetConverter facetConverter,
            IPageableConverter pageableConverter) {
        this.searchService = searchService;
        this.openSearchService = openSearchService;
        this.accessRightFilter = accessRightFilter;
        this.facetConverter = facetConverter;
        this.pageableConverter = pageableConverter;
    }

    @Deprecated // Only use method with ICriterion
    @Override
    public <S, R extends IIndexable> FacetPage<R> search(MultiValueMap<String, String> allParams,
            SearchKey<S, R> inSearchKey, List<String> facets, Pageable pageable)
            throws SearchException, OpenSearchUnknownParameter {
        try {
            // Build criterion from query
            ICriterion criterion = openSearchService.parse(allParams);
            return this.search(criterion, inSearchKey, facets, pageable);
        } catch (OpenSearchParseException e) {
            String message = "No query parameter";
            if (allParams != null) {
                StringJoiner sj = new StringJoiner("&");
                allParams.forEach((key, value) -> sj.add(key + "=" + value));
                message = sj.toString();
            }
            throw new SearchException(message, e);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <S, R extends IIndexable> FacetPage<R> search(ICriterion criterion, SearchKey<S, R> inSearchKey,
            List<String> facets, Pageable pageable) throws SearchException, OpenSearchUnknownParameter {
        try {
            SearchKey<?, ?> searchKey = inSearchKey;

            // Apply security filter
            criterion = accessRightFilter.addAccessRights(criterion);

            // Build search facets from query facets
            Map<String, String> reverseFacetNames = new HashMap<>();
            Map<String, FacetType> searchFacets = facetConverter.convert(facets, reverseFacetNames);
            // Translate sort query
            Pageable convertedPageable = pageableConverter.convert(pageable);
            // Optimisation: when searching for datasets via another searchType (ie searchKey is a
            // JoinEntitySearchKey<?, Dataset> without any criterion on searchType => just directly search
            // datasets (ie SimpleSearchKey<DataSet>)
            // This is correct because all
            if ((criterion == null) && (searchKey instanceof JoinEntitySearchKey)
                    && (TypeToken.of(searchKey.getResultClass()).getRawType() == Dataset.class)) {
                searchKey = Searches.onSingleEntity(Searches.fromClass(searchKey.getResultClass()));
            }

            // Perform search
            FacetPage<R> facetPage;
            if (searchKey instanceof SimpleSearchKey) {
                facetPage = searchService.search((SimpleSearchKey<R>) searchKey, convertedPageable, criterion,
                                                 searchFacets);
            } else {
                // It may be necessary to filter returned objects (before pagination !!!) by user access groups to avoid
                // getting datasets on which user has no right
                final Set<String> accessGroups = accessRightFilter.getUserAccessGroups();
                if ((TypeToken.of(searchKey.getResultClass()).getRawType() == Dataset.class)
                        && (accessGroups != null)) { // accessGroups null means superuser
                    Predicate<Dataset> datasetGroupAccessFilter = ds -> !Sets.intersection(ds.getGroups(), accessGroups)
                            .isEmpty();
                    facetPage = searchService.search((JoinEntitySearchKey<S, R>) searchKey, convertedPageable,
                                                     criterion, (Predicate<R>) datasetGroupAccessFilter, searchFacets);
                } else {
                    facetPage = searchService.search((JoinEntitySearchKey<S, R>) searchKey, convertedPageable,
                                                     criterion, searchFacets);
                }
            }

            // Rebuilding facets
            if (facetPage.getFacets() != null) {
                for (IFacet<?> facet : facetPage.getFacets()) {
                    facet.setAttributeName(reverseFacetNames.get(facet.getAttributeName()));
                }
            }

            // Filter data file according to access rights when searching for data objects
            if (((searchKey instanceof SimpleSearchKey)
                    && searchKey.getSearchTypeMap().values().contains(DataObject.class))
                    || ((searchKey.getResultClass() != null)
                            && (TypeToken.of(searchKey.getResultClass()).getRawType() == DataObject.class))) {
                Set<String> userGroups = accessRightFilter.getUserAccessGroups();
                for (R entity : facetPage.getContent()) {
                    if (entity instanceof DataObject) {
                        filterDataFiles(userGroups, (DataObject) entity);
                    }
                }
            }
            return facetPage;
        } catch (AccessRightFilterException e) {
            LOGGER.debug("Falling back to empty page", e);
            return new FacetPage<>(new ArrayList<>(), null);
        }
    }

    @Override
    public <R extends IIndexable> FacetPage<R> search(ICriterion criterion, SearchType searchType, List<String> facets,
            Pageable pageable) throws SearchException, OpenSearchUnknownParameter {
        return search(criterion, getSearchKey(searchType), facets, pageable);
    }

    /**
     * Filter data files according to data access rights
     */
    private void filterDataFiles(Set<String> userGroups, DataObject dataObject) {
        // Get data access rights
        // Map of { group -> data access right }
        Map<String, Boolean> groupsAccessRightMap = dataObject.getMetadata().getGroupsAccessRightsMap();

        // If user groups is null, it's an ADMIN request (Look at AccessRightFilter#getUserAccessGroups)
        // so do not filter data
        if (userGroups != null) {
            if (!userGroups.stream().anyMatch(userGroup -> (groupsAccessRightMap.containsKey(userGroup)
                    && groupsAccessRightMap.get(userGroup)))) {
                dataObject.getFiles().removeAll(DataType.RAWDATA);
            }
        }
    }

    @Override
    public <E extends AbstractEntity<?>> E get(OaisUniformResourceName urn)
            throws EntityOperationForbiddenException, EntityNotFoundException {
        E entity = searchService.get(urn);
        if (entity == null) {
            throw new EntityNotFoundException(urn.toString(), AbstractEntity.class);
        }
        Set<String> userGroups = null;
        try {
            userGroups = accessRightFilter.getUserAccessGroups();
        } catch (AccessRightFilterException e) {
            LOGGER.error("Forbidden operation", e);
            throw new EntityOperationForbiddenException(urn.toString(), entity.getClass(),
                    "You do not have access to this " + entity.getClass().getSimpleName());
        }
        // Fill downloadable property if entity is a DataObject
        if (entity instanceof DataObject) {
            filterDataFiles(userGroups, (DataObject) entity);
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

    @Deprecated // Only use method with ICriterion
    @Override
    public DocFilesSummary computeDatasetsSummary(MultiValueMap<String, String> allParams,
            SimpleSearchKey<DataObject> searchKey, OaisUniformResourceName dataset, List<DataType> dataTypes)
            throws SearchException {
        try {
            // Build criterion from query
            ICriterion criterion = openSearchService.parse(allParams);
            return this.computeDatasetsSummary(criterion, searchKey, dataset, dataTypes);
        } catch (OpenSearchParseException e) {
            String message = "No query parameter";
            if (allParams != null) {
                StringJoiner sj = new StringJoiner("&");
                allParams.forEach((key, value) -> sj.add(key + "=" + value));
                message = sj.toString();
            }
            throw new SearchException(message, e);
        }
    }

    @Override
    public DocFilesSummary computeDatasetsSummary(ICriterion criterion, SimpleSearchKey<DataObject> searchKey,
            OaisUniformResourceName dataset, List<DataType> dataTypes) throws SearchException {
        try {
            // Apply security filter (ie user groups)
            criterion = accessRightFilter.addDataAccessRights(criterion);
            // Perform compute
            DocFilesSummary summary = searchService.computeDataFilesSummary(searchKey, criterion, "tags", dataTypes);
            keepOnlyDatasetsWithGrantedAccess(searchKey, dataset, summary);

            return summary;
        } catch (AccessRightFilterException e) {
            LOGGER.debug("Falling back to empty summary", e);
            return new DocFilesSummary();
        }
    }

    @Override
    public DocFilesSummary computeDatasetsSummary(ICriterion criterion, SearchType searchType,
            OaisUniformResourceName dataset, List<DataType> dataTypes) throws SearchException {
        Assert.isTrue(SearchType.DATAOBJECTS.equals(searchType), "Only dataobject target is supported.");
        return computeDatasetsSummary(criterion, getSimpleSearchKey(searchType), dataset, dataTypes);
    }

    private void keepOnlyDatasetsWithGrantedAccess(SimpleSearchKey<DataObject> searchKey,
            OaisUniformResourceName dataset, DocFilesSummary summary) throws AccessRightFilterException {
        // Be careful ! "tags" is used to discriminate docFiles summaries because dataset URN is set into it BUT
        // all tags are used.
        // So we must remove all summaries that are not from dataset
        for (Iterator<String> i = summary.getSubSummariesMap().keySet().iterator(); i.hasNext();) {
            String tag = i.next();
            if (!OaisUniformResourceName.isValidUrn(tag)) {
                i.remove();
                continue;
            }
            OaisUniformResourceName urn = OaisUniformResourceName.fromString(tag);
            if (urn.getEntityType() != EntityType.DATASET) {
                i.remove();
                continue;
            }
        }

        // It is necessary to filter sub summaries first to keep only datasets and seconds to keep only datasets
        // on which user has right
        final Set<String> accessGroups = accessRightFilter.getUserAccessGroups();
        // If accessGroups is null, user is admin
        if (accessGroups != null) {
            // Retrieve all datasets that permit data objects retrieval (ie datasets with at least one groups with
            // data access right)
            // page size to max value because datasets count isn't too large...
            ICriterion dataObjectsGrantedCrit = ICriterion.or(accessGroups.stream()
                    .map(group -> ICriterion.eq("metadata.dataObjectsGroups." + group + ".dataObjectAccess", true))
                    .collect(Collectors.toSet()));
            Page<Dataset> page = searchService.search(Searches.onSingleEntity(EntityType.DATASET),
                                                      ISearchService.MAX_PAGE_SIZE, dataObjectsGrantedCrit);

            Set<String> datasetIpids = page.getContent().stream().map(Dataset::getIpId).map(urn -> urn.toString())
                    .collect(Collectors.toSet());
            // If summary is restricted to a specified datasetIpId, it must be taken into account
            if (dataset != null) {
                if (datasetIpids.contains(dataset.toString())) {
                    datasetIpids = Collections.singleton(dataset.toString());
                } else { // no dataset => summary contains normaly only 0 values as total
                    // we just need to clear map of sub summaries
                    summary.getSubSummariesMap().clear();
                }
            }
            for (Iterator<Entry<String, DocFilesSubSummary>> i = summary.getSubSummariesMap().entrySet().iterator(); i
                    .hasNext();) {
                // Remove it if subSummary discriminant isn't a dataset or isn't a dataset on which data can be
                // retrieved for current user
                if (!datasetIpids.contains(i.next().getKey())) {
                    i.remove();
                }
            }
        }
    }

    @Deprecated // Only use method with ICriterion
    @Override
    public <T extends IIndexable> List<String> retrieveEnumeratedPropertyValues(MultiValueMap<String, String> allParams,
            SearchKey<T, T> searchKey, String propertyPath, int maxCount, String partialText)
            throws SearchException, OpenSearchUnknownParameter {
        try {
            // Build criterion from query
            ICriterion criterion = openSearchService.parse(allParams);
            return retrieveEnumeratedPropertyValues(criterion, searchKey, propertyPath, maxCount, partialText);
        } catch (OpenSearchParseException e) {
            String message = "No query parameter";
            if (allParams != null) {
                StringJoiner sj = new StringJoiner("&");
                allParams.forEach((key, value) -> sj.add(key + "=" + value));
                message = sj.toString();
            }
            throw new SearchException(message, e);
        }
    }

    @Override
    public <T extends IIndexable> List<String> retrieveEnumeratedPropertyValues(ICriterion criterion,
            SearchKey<T, T> searchKey, String propertyPath, int maxCount, String partialText)
            throws SearchException, OpenSearchUnknownParameter {

        AttributeModel attModel = finder.findByName(propertyPath);

        try {
            // Apply security filter (ie user groups)
            criterion = accessRightFilter.addAccessRights(criterion);
            // Add partialText contains criterion if not empty
            if (!Strings.isNullOrEmpty(partialText)) {
                criterion = ICriterion.and(criterion, IFeatureCriterion.contains(attModel, partialText));
            }
            return searchService.searchUniqueTopValues(searchKey, criterion, attModel.getFullJsonPath(), maxCount);
        } catch (AccessRightFilterException e) {
            LOGGER.debug("Falling back to empty list of values", e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<String> retrieveEnumeratedPropertyValues(ICriterion criterion, SearchType searchType,
            String propertyPath, int maxCount, String partialText) throws SearchException, OpenSearchUnknownParameter {
        return retrieveEnumeratedPropertyValues(criterion, getSimpleSearchKey(searchType), propertyPath, maxCount,
                                                partialText);
    }

    @Override
    public List<Aggregation> retrievePropertiesStats(ICriterion criterion, SearchType searchType,
            Collection<QueryableAttribute> attributes) throws SearchException {
        try {
            // Apply security filter (ie user groups)
            criterion = accessRightFilter.addAccessRights(criterion);
            // Run search
            List<Aggregation> aggregations = searchService
                    .getAggregations(getSimpleSearchKey(searchType), criterion, attributes).asList();
            return aggregations;
        } catch (AccessRightFilterException e) {
            LOGGER.debug("Falling back to empty list of values", e);
            return Collections.emptyList();
        }
    }

    @SuppressWarnings("unchecked")
    private <T extends IIndexable> SimpleSearchKey<T> getSimpleSearchKey(SearchType searchType) {
        switch (searchType) {
            case ALL:
                return (SimpleSearchKey<T>) Searches.onAllEntities();
            case COLLECTIONS:
                return (SimpleSearchKey<T>) Searches.onSingleEntity(EntityType.COLLECTION);
            case DATAOBJECTS:
                return (SimpleSearchKey<T>) Searches.onSingleEntity(EntityType.DATA);
            case DATASETS:
                return (SimpleSearchKey<T>) Searches.onSingleEntity(EntityType.DATASET);
            default:
                throw new UnsupportedOperationException("Unsupported search type : " + searchType);
        }
    }

    @SuppressWarnings("unchecked")
    private <S, R extends IIndexable> SearchKey<S, R> getSearchKey(SearchType searchType) {
        switch (searchType) {
            case ALL:
                return (SearchKey<S, R>) Searches.onAllEntities();
            case COLLECTIONS:
                return (SearchKey<S, R>) Searches.onSingleEntity(EntityType.COLLECTION);
            case DATAOBJECTS:
                return (SearchKey<S, R>) Searches.onSingleEntity(EntityType.DATA);
            case DATASETS:
                return (SearchKey<S, R>) Searches.onSingleEntity(EntityType.DATASET);
            case DATAOBJECTS_RETURN_DATASETS:
                return (SearchKey<S, R>) Searches.onSingleEntityReturningJoinEntity(EntityType.DATA,
                                                                                    EntityType.DATASET);
            default:
                throw new UnsupportedOperationException("Unsupported search type : " + searchType);
        }
    }

    @Override
    public List<PropertyBound<?>> retrievePropertiesBounds(Set<String> propertyNames, ICriterion criterion,
            SearchType type) throws SearchException {
        List<PropertyBound<?>> bounds = Lists.newArrayList();
        Map<AttributeModel, QueryableAttribute> qas = Maps.newHashMap();
        propertyNames.forEach(property -> {
            AttributeModel attr;
            try {
                attr = finder.findByName(property);
                qas.put(attr, new QueryableAttribute(StaticProperties.FEATURE_NS + attr.getJsonPath(), null, false, 0));
            } catch (OpenSearchUnknownParameter e) {
                LOGGER.warn(e.getMessage(), e);
            }
        });
        retrievePropertiesStats(criterion, type, qas.values());
        qas.entrySet().forEach(qa -> {
            AttributeModel attribute = qa.getKey();
            Aggregation aggregation = qa.getValue().getAggregation();
            if ((aggregation != null) && aggregation.getType().equals(StatsAggregationBuilder.NAME)) {
                ParsedStats stats = (ParsedStats) aggregation;
                Double min = stats.getMin();
                String minAsString = stats.getMinAsString();
                Integer minAsInt = min.intValue();
                Long minAsLong = min.longValue();
                Double max = stats.getMax();
                String maxAsString = stats.getMaxAsString();
                Integer maxAsInt = max.intValue();
                Long maxAsLong = max.longValue();
                if ((Double.NEGATIVE_INFINITY == stats.getMin()) || (Double.POSITIVE_INFINITY == stats.getMin())) {
                    min = null;
                    minAsString = null;
                    minAsInt = null;
                    minAsLong = null;
                }
                if ((Double.POSITIVE_INFINITY == stats.getMax()) || (Double.NEGATIVE_INFINITY == stats.getMax())) {
                    max = null;
                    maxAsString = null;
                    maxAsInt = null;
                    maxAsLong = null;
                }
                switch (attribute.getType()) {
                    case DATE_ARRAY:
                    case DATE_INTERVAL:
                    case DATE_ISO8601:
                        bounds.add(new PropertyBound<String>(attribute.getJsonPath(), minAsString, maxAsString));
                        break;
                    case DOUBLE:
                    case DOUBLE_ARRAY:
                    case DOUBLE_INTERVAL:
                        bounds.add(new PropertyBound<Double>(attribute.getJsonPath(), min, max));
                        break;
                    case INTEGER:
                    case INTEGER_ARRAY:
                    case INTEGER_INTERVAL:
                        bounds.add(new PropertyBound<Integer>(attribute.getJsonPath(), minAsInt, maxAsInt));
                        break;
                    case LONG:
                    case LONG_ARRAY:
                    case LONG_INTERVAL:
                        bounds.add(new PropertyBound<Long>(attribute.getJsonPath(), minAsLong, maxAsLong));
                        break;
                    case STRING:
                    case STRING_ARRAY:
                    case URL:
                    case BOOLEAN:
                    default:
                        break;
                }
            }
        });
        return bounds;
    }

    @Override
    public boolean hasAccess(OaisUniformResourceName urn) throws EntityNotFoundException {
        try {
            get(urn);
            return true;
        } catch (EntityOperationForbiddenException e) {
            return false;
        }
    }
}