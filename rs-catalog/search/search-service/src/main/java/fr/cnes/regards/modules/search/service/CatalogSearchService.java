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
package fr.cnes.regards.modules.search.service;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.EntityOperationForbiddenException;
import fr.cnes.regards.framework.urn.DataType;
import fr.cnes.regards.framework.urn.EntityType;
import fr.cnes.regards.framework.urn.UniformResourceName;
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
import fr.cnes.regards.modules.indexer.domain.criterion.EmptyCriterion;
import fr.cnes.regards.modules.indexer.domain.criterion.ICriterion;
import fr.cnes.regards.modules.indexer.domain.criterion.StringMatchType;
import fr.cnes.regards.modules.indexer.domain.facet.FacetType;
import fr.cnes.regards.modules.indexer.domain.facet.IFacet;
import fr.cnes.regards.modules.indexer.domain.summary.DocFilesSubSummary;
import fr.cnes.regards.modules.indexer.domain.summary.DocFilesSummary;
import fr.cnes.regards.modules.indexer.service.ISearchService;
import fr.cnes.regards.modules.indexer.service.Searches;
import fr.cnes.regards.modules.model.domain.attributes.AttributeModel;
import fr.cnes.regards.modules.opensearch.service.IOpenSearchService;
import fr.cnes.regards.modules.opensearch.service.cache.attributemodel.IAttributeFinder;
import fr.cnes.regards.modules.opensearch.service.exception.OpenSearchUnknownParameter;
import fr.cnes.regards.modules.search.domain.PropertyBound;
import fr.cnes.regards.modules.search.domain.plugin.CollectionWithStats;
import fr.cnes.regards.modules.search.domain.plugin.SearchType;
import fr.cnes.regards.modules.search.service.accessright.AccessRightFilterException;
import fr.cnes.regards.modules.search.service.accessright.IAccessRightFilter;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.metrics.ParsedStats;
import org.elasticsearch.search.aggregations.metrics.StatsAggregationBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

/**
 * Implementation of {@link ICatalogSearchService}
 *
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
     * @param searchService     Service perfoming the ElasticSearch search from criterions. Autowired by Spring. Must not be
     *                          null.
     * @param openSearchService The OpenSearch service building {@link ICriterion} from a request string. Autowired by
     *                          Spring. Must not be null.
     * @param accessRightFilter Service handling the access groups in criterion. Autowired by Spring. Must not be null.
     * @param facetConverter    manage facet conversion
     */
    public CatalogSearchService(ISearchService searchService,
                                IOpenSearchService openSearchService,
                                IAccessRightFilter accessRightFilter,
                                IFacetConverter facetConverter,
                                IPageableConverter pageableConverter) {
        this.searchService = searchService;
        this.openSearchService = openSearchService;
        this.accessRightFilter = accessRightFilter;
        this.facetConverter = facetConverter;
        this.pageableConverter = pageableConverter;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <S, R extends IIndexable> FacetPage<R> search(ICriterion criterion,
                                                         SearchKey<S, R> inSearchKey,
                                                         List<String> facets,
                                                         Pageable pageable)
        throws SearchException, OpenSearchUnknownParameter {
        try {
            SearchKey<?, ?> searchKey = inSearchKey;

            // Build search facets from query facets
            Map<String, String> reverseFacetNames = new HashMap<>();
            Map<String, FacetType> searchFacets = facetConverter.convert(facets, reverseFacetNames);
            // Translate sort query
            Pageable convertedPageable = pageableConverter.convert(pageable);
            // Optimisation: when searching for datasets via another searchType (ie searchKey is a
            // JoinEntitySearchKey<?, Dataset> without any criterion on searchType => just directly search
            // datasets (ie SimpleSearchKey<DataSet>)
            // This is correct because all
            if ((criterion == null || criterion instanceof EmptyCriterion) && (searchKey instanceof JoinEntitySearchKey)
                && (searchKey.getResultClass() == Dataset.class)) {
                searchKey = Searches.onSingleEntity(Searches.fromClass(searchKey.getResultClass()));
            }

            // Apply security filters before performing a search to limit the results that can be seen by the user
            // Retrieve current user access groups. Null means superuser with all rights
            final Set<String> accessGroups = accessRightFilter.getUserAccessGroups();
            criterion = accessRightFilter.addAccessRights(criterion);

            // Perform search
            FacetPage<R> facetPage;
            if (searchKey instanceof SimpleSearchKey) {
                facetPage = searchService.search((SimpleSearchKey<R>) searchKey,
                                                 convertedPageable,
                                                 criterion,
                                                 searchFacets);
            } else {
                facetPage = searchService.search((JoinEntitySearchKey<S, R>) searchKey,
                                                 convertedPageable,
                                                 criterion,
                                                 accessRightFilter.addAccessRights(ICriterion.all()),
                                                 searchFacets);
            }

            // Rebuilding facets
            if (facetPage.getFacets() != null) {
                for (IFacet<?> facet : facetPage.getFacets()) {
                    facet.setAttributeName(reverseFacetNames.get(facet.getAttributeName()));
                }
            }

            // Filter data file according to access rights when searching for data objects
            if (((searchKey instanceof SimpleSearchKey) && searchKey.getSearchTypeMap().containsValue(DataObject.class))
                || ((searchKey.getResultClass() != null) && (searchKey.getResultClass() == DataObject.class))) {
                for (R entity : facetPage.getContent()) {
                    if (entity instanceof DataObject) {
                        filterDataFiles(accessGroups, ((DataObject) entity));
                    }
                }
            }
            return facetPage;
        } catch (AccessRightFilterException e) {
            LOGGER.debug("Falling back to empty page", e);
            return new FacetPage<>(new ArrayList<>(), null, pageable, 0);
        }
    }

    @Override
    public <R extends IIndexable> FacetPage<R> search(ICriterion criterion,
                                                      SearchType searchType,
                                                      List<String> facets,
                                                      Pageable pageable)
        throws SearchException, OpenSearchUnknownParameter {
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
            if (userGroups.stream()
                          .noneMatch(userGroup -> (groupsAccessRightMap.containsKey(userGroup)
                                                   && groupsAccessRightMap.get(userGroup)))) {
                dataObject.getFiles().removeAll(DataType.RAWDATA);
            }
        }
    }

    @Override
    public <E extends AbstractEntity<?>> E get(UniformResourceName urn)
        throws EntityOperationForbiddenException, EntityNotFoundException {
        E entity = searchService.get(urn);
        if (entity == null) {
            throw new EntityNotFoundException(urn.toString(), AbstractEntity.class);
        }
        Set<String> userGroups;
        try {
            userGroups = accessRightFilter.getUserAccessGroups();
        } catch (AccessRightFilterException e) {
            LOGGER.error("Forbidden operation", e);
            throw new EntityOperationForbiddenException(urn.toString(),
                                                        entity.getClass(),
                                                        "You do not have access to this " + entity.getClass()
                                                                                                  .getSimpleName());
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
        throw new EntityOperationForbiddenException(urn.toString(),
                                                    entity.getClass(),
                                                    "You do not have access to this " + entity.getClass()
                                                                                              .getSimpleName());
    }

    @Override
    public DocFilesSummary computeDatasetsSummary(ICriterion criterion,
                                                  SimpleSearchKey<DataObject> searchKey,
                                                  UniformResourceName dataset,
                                                  List<DataType> dataTypes) {
        try {
            // Apply security filter (ie user groups)
            criterion = accessRightFilter.addDataAccessRights(criterion);
            // Perform compute
            DocFilesSummary summary = searchService.computeDataFilesSummary(searchKey,
                                                                            criterion,
                                                                            "tags",
                                                                            Optional.of("URN:AIP:DATASET.*"),
                                                                            dataTypes);
            keepOnlyDatasetsWithGrantedAccess(searchKey, dataset, summary);

            return summary;
        } catch (AccessRightFilterException e) {
            LOGGER.debug("Falling back to empty summary", e);
            return new DocFilesSummary();
        }
    }

    @Override
    public DocFilesSummary computeDatasetsSummary(ICriterion criterion,
                                                  SearchType searchType,
                                                  UniformResourceName dataset,
                                                  List<DataType> dataTypes) {
        Assert.isTrue(SearchType.DATAOBJECTS.equals(searchType), "Only dataobject target is supported.");
        return computeDatasetsSummary(criterion, getSimpleSearchKey(searchType), dataset, dataTypes);
    }

    private void keepOnlyDatasetsWithGrantedAccess(SimpleSearchKey<DataObject> searchKey,
                                                   UniformResourceName dataset,
                                                   DocFilesSummary summary) throws AccessRightFilterException {
        // Be careful ! "tags" is used to discriminate docFiles summaries because dataset URN is set into it BUT
        // all tags are used.
        // So we must remove all summaries that are not from dataset
        for (Iterator<String> i = summary.getSubSummariesMap().keySet().iterator(); i.hasNext(); ) {
            String tag = i.next();
            if (!UniformResourceName.isValidUrn(tag)) {
                i.remove();
                continue;
            }
            UniformResourceName urn = UniformResourceName.fromString(tag);
            if (urn.getEntityType() != EntityType.DATASET) {
                i.remove();
            }
        }

        // It is necessary to filter sub summaries first to keep only datasets and seconds to keep only datasets
        // on which user has right
        final Set<String> accessGroups = accessRightFilter.getUserAccessGroups();
        // If accessGroups is null, user is admin
        if (accessGroups != null) {
            // Retrieve all datasets that permit data objects retrieval (ie datasets with at least one of
            // the user access rights group)
            // page size to max value because datasets count isn't too large...
            ICriterion dataObjectsGrantedCrit = ICriterion.or(accessGroups.stream()
                                                                          .map(group -> ICriterion.contains("groups",
                                                                                                            group,
                                                                                                            StringMatchType.KEYWORD))
                                                                          .collect(Collectors.toSet()));
            Page<Dataset> page = searchService.search(Searches.onSingleEntity(EntityType.DATASET),
                                                      ISearchService.MAX_PAGE_SIZE,
                                                      dataObjectsGrantedCrit);

            Set<String> datasetIpids = page.getContent()
                                           .stream()
                                           .map(Dataset::getIpId)
                                           .map(UniformResourceName::toString)
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
            for (Iterator<Entry<String, DocFilesSubSummary>> i = summary.getSubSummariesMap()
                                                                        .entrySet()
                                                                        .iterator(); i.hasNext(); ) {
                // Remove it if subSummary discriminant isn't a dataset or isn't a dataset on which data can be
                // retrieved for current user
                if (!datasetIpids.contains(i.next().getKey())) {
                    i.remove();
                }
            }
        }
    }

    @Override
    public <T extends IIndexable> List<String> retrieveEnumeratedPropertyValues(ICriterion criterion,
                                                                                SearchKey<T, T> searchKey,
                                                                                String propertyPath,
                                                                                int maxCount,
                                                                                String partialText) {
        AttributeModel attModel = null;
        String attributePath = propertyPath;
        try {
            attModel = finder.findByName(propertyPath);
            attributePath = attModel.getFullJsonPath();
        } catch (OpenSearchUnknownParameter e) {
            LOGGER.debug("Unknown attribute. Not from an existing model : {}", propertyPath);
        }

        try {
            // Apply security filter (ie user groups)
            criterion = accessRightFilter.addAccessRights(criterion);
            // Add partialText contains criterion if not empty
            if (!Strings.isNullOrEmpty(partialText)) {
                if (attModel != null) {
                    criterion = ICriterion.and(criterion,
                                               IFeatureCriterion.contains(attModel,
                                                                          partialText,
                                                                          StringMatchType.KEYWORD));
                } else {
                    criterion = ICriterion.and(criterion,
                                               ICriterion.contains(propertyPath, partialText, StringMatchType.KEYWORD));
                }
            }
            return searchService.searchUniqueTopValues(searchKey, criterion, attributePath, maxCount);
        } catch (AccessRightFilterException e) {
            LOGGER.debug("Falling back to empty list of values", e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<String> retrieveEnumeratedPropertyValues(ICriterion criterion,
                                                         SearchType searchType,
                                                         String propertyPath,
                                                         int maxCount,
                                                         String partialText) {
        return retrieveEnumeratedPropertyValues(criterion,
                                                getSimpleSearchKey(searchType),
                                                propertyPath,
                                                maxCount,
                                                partialText);
    }

    @Override
    public List<Aggregation> retrievePropertiesStats(ICriterion criterion,
                                                     SearchType searchType,
                                                     Collection<QueryableAttribute> attributes) {
        try {
            // Apply security filter (ie user groups)
            criterion = accessRightFilter.addAccessRights(criterion);
            // Run search
            return searchService.getAggregations(getSimpleSearchKey(searchType), criterion, attributes).asList();
        } catch (AccessRightFilterException e) {
            LOGGER.debug("Falling back to empty list of values", e);
            return Collections.emptyList();
        }
    }

    @Override
    public CollectionWithStats getCollectionWithDataObjectsStats(UniformResourceName urn,
                                                                 SearchType searchType,
                                                                 Collection<QueryableAttribute> attributes)
        throws EntityOperationForbiddenException, EntityNotFoundException {

        AbstractEntity<?> abstractEntity = get(urn);
        //We look for collection's dataobjects by urn in tags
        ICriterion tags = ICriterion.contains("tags", urn.toString(), StringMatchType.KEYWORD);
        List<Aggregation> aggregations = retrievePropertiesStats(tags, searchType, attributes);
        return new CollectionWithStats(abstractEntity, aggregations);
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
    public List<PropertyBound<?>> retrievePropertiesBounds(Set<String> propertyNames,
                                                           ICriterion criterion,
                                                           SearchType type) {
        List<PropertyBound<?>> bounds = Lists.newArrayList();
        Map<AttributeModel, QueryableAttribute> qas = Maps.newHashMap();
        propertyNames.forEach(property -> {
            AttributeModel attr;
            try {
                attr = finder.findByName(property);
                qas.put(attr,
                        new QueryableAttribute(StaticProperties.FEATURE_NS + attr.getJsonPath(),
                                               null,
                                               attr.isTextAttribute(),
                                               0,
                                               attr.isBooleanAttribute()));
            } catch (OpenSearchUnknownParameter e) {
                LOGGER.warn(e.getMessage(), e);
            }
        });
        retrievePropertiesStats(criterion, type, qas.values());
        qas.forEach((attribute, value) -> {
            Aggregation aggregation = value.getAggregation();
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
                        bounds.add(new PropertyBound<>(attribute.getJsonPath(), minAsString, maxAsString));
                        break;
                    case DOUBLE:
                    case DOUBLE_ARRAY:
                    case DOUBLE_INTERVAL:
                        bounds.add(new PropertyBound<>(attribute.getJsonPath(), min, max));
                        break;
                    case INTEGER:
                    case INTEGER_ARRAY:
                    case INTEGER_INTERVAL:
                        bounds.add(new PropertyBound<>(attribute.getJsonPath(), minAsInt, maxAsInt));
                        break;
                    case LONG:
                    case LONG_ARRAY:
                    case LONG_INTERVAL:
                        bounds.add(new PropertyBound<>(attribute.getJsonPath(), minAsLong, maxAsLong));
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
}