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
package fr.cnes.regards.modules.indexer.service;

import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.oais.urn.OaisUniformResourceName;
import fr.cnes.regards.framework.urn.DataType;
import fr.cnes.regards.framework.urn.UniformResourceName;
import fr.cnes.regards.modules.dam.domain.entities.DataObject;
import fr.cnes.regards.modules.dam.domain.entities.Dataset;
import fr.cnes.regards.modules.dam.domain.entities.StaticProperties;
import fr.cnes.regards.modules.indexer.dao.FacetPage;
import fr.cnes.regards.modules.indexer.dao.IEsRepository;
import fr.cnes.regards.modules.indexer.dao.spatial.ProjectGeoSettings;
import fr.cnes.regards.modules.indexer.domain.*;
import fr.cnes.regards.modules.indexer.domain.aggregation.QueryableAttribute;
import fr.cnes.regards.modules.indexer.domain.criterion.ICriterion;
import fr.cnes.regards.modules.indexer.domain.criterion.StringMatchType;
import fr.cnes.regards.modules.indexer.domain.facet.FacetType;
import fr.cnes.regards.modules.indexer.domain.summary.DocFilesSummary;
import org.elasticsearch.search.aggregations.Aggregations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Service
public class SearchService implements ISearchService {

    @Autowired
    private IEsRepository repository;

    @Autowired
    private IRuntimeTenantResolver tenantResolver;

    @Autowired
    private ProjectGeoSettings projectGeoSettings;

    @SuppressWarnings("unchecked")
    @Override
    public <T extends IIndexable> T get(UniformResourceName urn) {
        Class<? extends IIndexable> clazz;
        switch (urn.getEntityType()) {
            case COLLECTION:
                clazz = fr.cnes.regards.modules.dam.domain.entities.Collection.class;
                break;
            case DATA:
                clazz = DataObject.class;
                break;
            case DATASET:
                clazz = Dataset.class;
                break;
            default:
                throw new IllegalArgumentException();
        }
        if (urn.isLast()) {
            return (T) repository.getByVirtualId(urn.getEntityType().toString(), urn.toString(), clazz);
        } else {
            return (T) repository.get(urn.getEntityType().toString(), urn.toString(), clazz);
        }
    }

    /**
     * Add project informations as index or associated CRS into searchKey
     */
    private void addProjectInfos(SearchKey<?, ?> searchKey) {
        searchKey.setSearchIndex(tenantResolver.getTenant());
        searchKey.setCrs(projectGeoSettings.getCrs());
    }

    @Override
    public <T extends IIndexable> FacetPage<T> search(SimpleSearchKey<T> searchKey,
                                                      Pageable pageRequest,
                                                      ICriterion criterion,
                                                      Map<String, FacetType> facetsMap) {
        addProjectInfos(searchKey);
        return repository.search(searchKey, pageRequest, criterion, facetsMap);
    }

    @Override
    public <S, T extends IIndexable> FacetPage<T> search(JoinEntitySearchKey<S, T> searchKey,
                                                         Pageable pageRequest,
                                                         ICriterion criterion,
                                                         ICriterion searchResultCriterion,
                                                         Map<String, FacetType> facetsMap) {
        addProjectInfos(searchKey);
        // Create a new SearchKey to search on asked type but to only retrieve tags of found results
        SearchKey<S, String[]> tagSearchKey = new SearchKey<>(searchKey.getSearchTypeMap(), String[].class);
        addProjectInfos(tagSearchKey);
        // Predicate to filter each tag : it must be a valid URN and this URN must concern wanted result type
        Predicate<String> askedTypePredicate = tag -> OaisUniformResourceName.isValidUrn(tag) && (Searches.TYPE_MAP.get(
            OaisUniformResourceName.fromString(tag).getEntityType()) == searchKey.getResultClass());
        // Create a new search key to search elements based on the result class of the joinedSearchKey
        SearchKey<T, T> outputSearchKey = new SearchKey<>(Collections.singletonMap(Searches.TYPE_MAP.inverse()
                                                                                                    .get(searchKey.getResultClass())
                                                                                                    .toString(),
                                                                                   searchKey.getResultClass()));
        addProjectInfos(outputSearchKey);
        // Retrieve objects with matching URN from ES Repository
        Function<Set<String>, Page<T>> toAskEntityFct = inputObjects -> repository.search(outputSearchKey,
                                                                                          pageRequest,
                                                                                          ICriterion.and(ICriterion.in(
                                                                                                             StaticProperties.IP_ID,
                                                                                                             StringMatchType.KEYWORD,
                                                                                                             inputObjects.stream()
                                                                                                                         .map(String.class::cast)
                                                                                                                         .toArray(
                                                                                                                             String[]::new)),
                                                                                                         searchResultCriterion));
        return repository.search(tagSearchKey,
                                 criterion,
                                 StaticProperties.FEATURE_TAGS,
                                 askedTypePredicate,
                                 toAskEntityFct,
                                 facetsMap,
                                 pageRequest);
    }

    @Override
    public <T extends IIndexable> Page<T> multiFieldsSearch(SearchKey<T, T> searchKey,
                                                            Pageable pageRequest,
                                                            Object value,
                                                            String... fields) {
        addProjectInfos(searchKey);
        return repository.multiFieldsSearch(searchKey, pageRequest, value, fields);
    }

    @Override
    public <T extends IIndexable & IDocFiles> DocFilesSummary computeDataFilesSummary(SearchKey<T, T> searchKey,
                                                                                      ICriterion criterion,
                                                                                      String discriminantProperty,
                                                                                      Optional<String> discriminentPropertyInclude,
                                                                                      List<DataType> dataTypes) {

        String[] fileTypes = new String[dataTypes.size()];
        for (int i = 0; i < dataTypes.size(); i++) {
            fileTypes[i] = dataTypes.get(i).toString();
        }

        addProjectInfos(searchKey);
        DocFilesSummary summary = new DocFilesSummary();
        // Adjust criterion to search for internal data
        ICriterion internalCrit = ICriterion.and(criterion.copy(), ICriterion.eq("internal", true));
        repository.computeInternalDataFilesSummary(searchKey,
                                                   internalCrit,
                                                   discriminantProperty,
                                                   discriminentPropertyInclude,
                                                   summary,
                                                   fileTypes);
        // Adjust criterion to search for external data (=> internal is false and all at least one searched file type
        // has an uri starting with http or https
        ICriterion filterUriCrit = ICriterion.or(Arrays.stream(fileTypes)
                                                       .map(fileType -> ICriterion.regexp(StaticProperties.FEATURE_FILES_PATH
                                                                                          + "."
                                                                                          + fileType
                                                                                          + ".uri",
                                                                                          "https?://.*",
                                                                                          StringMatchType.KEYWORD))
                                                       .collect(Collectors.toList()));
        ICriterion externalCrit = ICriterion.and(criterion.copy(), ICriterion.eq("internal", false), filterUriCrit);
        repository.computeExternalDataFilesSummary(searchKey,
                                                   externalCrit,
                                                   discriminantProperty,
                                                   discriminentPropertyInclude,
                                                   summary,
                                                   fileTypes);
        return summary;
    }

    @Override
    public <T extends IIndexable> List<String> searchUniqueTopValues(SearchKey<T, T> searchKey,
                                                                     ICriterion criterion,
                                                                     String attName,
                                                                     int maxCount) {
        addProjectInfos(searchKey);
        SortedSet<String> values = repository.uniqueAlphaSorted(searchKey, criterion, attName, maxCount);
        return values.stream().limit(maxCount).collect(Collectors.toList());
    }

    @Override
    public <T extends IIndexable> Aggregations getAggregations(SimpleSearchKey<T> searchKey,
                                                               ICriterion criterion,
                                                               Collection<QueryableAttribute> attributes) {
        addProjectInfos(searchKey);
        return repository.getAggregations(searchKey, criterion, attributes);
    }
}
