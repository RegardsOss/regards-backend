/*
 * Copyright 2017-2025 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
 * along with REGARDS. If not, see `<http://www.gnu.org/licenses/>`.
 */

package fr.cnes.regards.modules.indexer.service;

import fr.cnes.regards.modules.dam.domain.entities.DataObject;
import fr.cnes.regards.modules.indexer.dao.BulkSaveResult;
import fr.cnes.regards.modules.indexer.dao.CreateIndexConfiguration;
import fr.cnes.regards.modules.indexer.dao.FacetPage;
import fr.cnes.regards.modules.indexer.dao.IEsRepository;
import fr.cnes.regards.modules.indexer.dao.mapping.AttributeDescription;
import fr.cnes.regards.modules.indexer.domain.*;
import fr.cnes.regards.modules.indexer.domain.aggregation.QueryableAttribute;
import fr.cnes.regards.modules.indexer.domain.criterion.ICriterion;
import fr.cnes.regards.modules.indexer.domain.facet.FacetType;
import fr.cnes.regards.modules.indexer.domain.summary.DocFilesSummary;
import org.elasticsearch.index.IndexNotFoundException;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.Aggregations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import static fr.cnes.regards.modules.indexer.service.IndexAliasResolver.resolveAliasName;

/**
 * Facade for Elasticsearch repository operations.
 * <p>
 * This service provides a higher-level abstraction on top of the low-level {@link IEsRepository}.
 * <p>
 * It is intended to be used by service-layer and above, while the DAO layer should directly access
 * {@link IEsRepository}.
 * Thus, this facade is intentionally thin: it mainly adds tenant/alias semantics and alias+building index
 * composition, without re-implementing DAO logic. *
 *
 * @author mnguyen0
 */
@Service
public class EsRepositoryFacade {

    private IEsRepository esRepository;

    private IndexAliasResolver indexAliasResolver;

    public EsRepositoryFacade(IEsRepository esRepository, IndexAliasResolver indexAliasResolver) {
        this.esRepository = esRepository;
        this.indexAliasResolver = indexAliasResolver;
    }

    /* =========================
       Index & alias management
       ========================= */

    /**
     * Check if an index exists
     *
     * @param indexName raw index name, not alias
     */
    public boolean indexExists(String indexName) {
        return esRepository.indexExists(indexName);
    }

    /**
     * Create index with default configuration
     *
     * @param index raw index name
     */
    public boolean createIndex(String index) {
        return esRepository.createIndex(index);
    }

    /**
     * Create index with the given configuration
     *
     * @param index raw index name
     */
    public boolean createIndex(String index, CreateIndexConfiguration configuration) {
        return esRepository.createIndex(index, configuration);
    }

    /**
     * If parameter is an index name, delete this index if present
     * Beware: Deleting an index also removes any aliases exclusively pointing to this index
     * If parameter is an alias name, delete this alias if present
     *
     * @param indexOrAlias index or alias name
     */
    public boolean deleteIndexOrAlias(String indexOrAlias) throws IndexNotFoundException {
        return esRepository.deleteIndex(indexOrAlias);
    }

    /**
     * Check if an alias exists
     */
    public boolean aliasExists(String aliasName) {
        return esRepository.aliasExists(aliasName);
    }

    /**
     * Create an alias pointing to the given index.
     */
    public boolean createAlias(String index, String aliasName) {
        return esRepository.createAlias(index, aliasName);
    }

    /**
     * Apply mappings to a specific index
     *
     * @param index raw index name
     */
    public boolean putMappingsToOneIndex(String index, Set<AttributeDescription> mappings) {
        return esRepository.putMappings(index, mappings);
    }

    /**
     * Get mappings from a specific index or from an alias
     */
    public Map<String, Object> getMappingFromIndexOrAlias(String indexOrAlias) {
        return esRepository.getMappings(indexOrAlias);
    }

    /**
     * Get the unique index behind an alias, or throw if alias points to 0 or >1 indices.
     */
    public String getSingleIndexPointedByAlias(String aliasName) throws IllegalStateException {
        return esRepository.getSingleIndexPointedByAlias(aliasName);
    }

    /**
     * Atomically switch an alias from one index to another
     */
    public boolean switchAlias(String oldIndex, String newIndex, String alias) {
        return esRepository.switchAlias(oldIndex, newIndex, alias);
    }

    /* =========================
       Deletes / Upserts / Save
       ========================= */

    /**
     * Execute a delete-by-query on both the building index (if present) and the tenant alias
     */
    public long deleteByQueryOnAliasAndBuildingIndex(String tenant, ICriterion criterion) {
        return indexAliasResolver.resolveBuildingIndex(tenant)
                                 .map(idx -> esRepository.deleteByQuery(idx, criterion))
                                 .orElse(0L) + esRepository.deleteByQuery(resolveAliasName(tenant), criterion);
    }

    /**
     * Get by id via the tenant (always resolved to alias)
     */
    public <T extends IIndexable> T get(String tenant, String docType, String docId, Class<T> clazz) {
        return esRepository.get(resolveAliasName(tenant), docType, docId, clazz);
    }

    /**
     * Get a document from a specific index
     *
     * @param indexOrAlias either building index or alias name
     */
    public <T extends IIndexable> T get(String indexOrAlias, final T document) {
        return esRepository.get(indexOrAlias, document);
    }

    /**
     * Get by id on a raw type/class, without tenant notion
     */
    public <T extends IIndexable> T get(String docType, String docId, Class<T> clazz) {
        return esRepository.get(docType, docId, clazz);
    }

    /**
     * Retrieve a document by virtualId (not tenant-aware)
     */
    public <T extends IIndexable> T getByVirtualId(String docType,
                                                   String virtualId,
                                                   Class<? extends IIndexable> clazz) {
        return esRepository.getByVirtualId(docType, virtualId, clazz);
    }

    /**
     * Delete by id in a specific index
     *
     * @param indexOrAlias either building index or alias name
     */
    public boolean deleteFromIndexOrAlias(String indexOrAlias, String type, String id) {
        return esRepository.delete(indexOrAlias, type, id);
    }

    /**
     * Save a document to a specific index
     *
     * @param indexOrAlias either building index or alias name
     */
    public boolean saveToIndexOrAlias(String indexOrAlias, IIndexable doc) {
        return esRepository.save(indexOrAlias, doc);
    }

    /**
     * Bulk save to a specific index
     *
     * @param indexOrAlias either building index or alias name
     */
    public BulkSaveResult saveBulkToIndexOrAlias(String indexOrAlias, Collection<? extends IIndexable> documents)
        throws IllegalArgumentException {
        return esRepository.saveBulk(indexOrAlias, documents);
    }

    /**
     * Upsert into a specific index
     *
     * @param indexOrAlias either building index or alias name
     */
    public void upsertToIndexOrAlias(String indexOrAlias,
                                     BulkSaveResult bulkSaveResult,
                                     Set<DataObject> toSaveObjects,
                                     StringBuilder buf) {
        esRepository.upsert(indexOrAlias, bulkSaveResult, toSaveObjects, buf);
    }

    /**
     * Update-by-query on a specific index (defined by {@code searchKey}).
     */
    public <T extends IIndexable> void updateByQueryInOneIndex(SimpleSearchKey<DataObject> searchKey,
                                                               ICriterion subsettingCrit,
                                                               String scriptId,
                                                               Map<String, Object> params) {
        esRepository.updateByQuery(searchKey, subsettingCrit, scriptId, params);
    }

    /**
     * Delete by datasource on both alias and building index (if present)
     */
    public void deleteByDatasourceInAliasAndBuildingIndex(String tenant, Long datasourceId) {
        runOnAliasAndBuildingIndex(tenant, index -> esRepository.deleteByDatasource(index, datasourceId));
    }

    /**
     * Refresh a specific index or alias
     */
    public void refreshIndex(String index) {
        esRepository.refresh(index);
    }

    /* =========================
      Search / Count / Aggs
    ========================= */

    /**
     * Retrieve sum of given attributes
     * Index is specified in the searchKey parameter
     */
    public <T extends IIndexable> double sum(SearchKey<?, T> searchKey, ICriterion criterion, String attName) {
        return esRepository.sum(searchKey, criterion, attName);
    }

    /**
     * Retrieve minimum date of given date attribute
     * Index is specified in the searchKey parameter
     */
    public <T extends IIndexable> OffsetDateTime minDate(SearchKey<?, T> searchKey,
                                                         ICriterion criterion,
                                                         String attName) {
        return esRepository.minDate(searchKey, criterion, attName);
    }

    /**
     * Retrieve maximum date of given date attribute
     * Index is specified in the searchKey parameter
     */
    public <T extends IIndexable> OffsetDateTime maxDate(SearchKey<?, T> searchKey,
                                                         ICriterion criterion,
                                                         String attName) {
        return esRepository.maxDate(searchKey, criterion, attName);
    }

    /**
     * Execute specified action for all search results
     * Index is specified in the searchKey parameter
     */
    public <T extends IIndexable> void searchAll(SearchKey<T, T> searchKey, Consumer<T> action, ICriterion inCrit) {
        esRepository.searchAll(searchKey, action, inCrit);
    }

    /**
     * Searching specified page of elements from index with facets. <b>This method fails if asked for offset greater than
     * 10000 (Elasticsearch limitation)</b>
     * <p>
     * Index is specified in the searchKey parameter
     */
    public <T extends IIndexable> FacetPage<T> search(SearchKey<T, T> searchKey,
                                                      Pageable pageRequest,
                                                      ICriterion crit,
                                                      Map<String, FacetType> facetsMap) {
        return esRepository.search(searchKey, pageRequest, crit, facetsMap);
    }

    /**
     * Searching specified page of elements from index without facets nor sort.
     * This method fails if asked for offset greater than 10000 (Elasticsearch limitation)
     * <p>
     * Index is specified in the searchKey parameter
     */
    public <T extends IIndexable> Page<T> search(final SearchKey<T, T> searchKey,
                                                 final Pageable pageRequest,
                                                 final ICriterion crit) {
        return esRepository.search(searchKey, pageRequest, crit);
    }

    /**
     * Search objects with criteria retrieved from other objects.
     * <p>
     * Index is specified in the searchKey parameter
     */
    public <R, U extends IIndexable> FacetPage<U> search(SearchKey<?, R[]> sourceSearchKey,
                                                         ICriterion sourceSearchCriterion,
                                                         String sourceAttribute,
                                                         Predicate<R> sourceFilterPredicate,
                                                         Function<Set<R>, Page<U>> toAskEntityFct,
                                                         Map<String, FacetType> facetsMap,
                                                         Pageable pageRequest) {
        return esRepository.search(sourceSearchKey,
                                   sourceSearchCriterion,
                                   sourceAttribute,
                                   sourceFilterPredicate,
                                   toAskEntityFct,
                                   facetsMap,
                                   pageRequest);
    }

    /**
     * Count result
     * <p>
     * Index is specified in the searchKey parameter
     */
    public <T extends IIndexable> Long count(SearchKey<?, T> searchKey, ICriterion criterion) {
        return esRepository.count(searchKey, criterion);
    }

    public <T extends IIndexable> Aggregations getAggregationsFor(SearchKey<?, T> searchKey,
                                                                  ICriterion criterion,
                                                                  Collection<AggregationBuilder> aggs,
                                                                  int limit) {
        return esRepository.getAggregationsFor(searchKey, criterion, aggs, limit);
    }

    /**
     * Retrieve the desired specific aggregations.
     * Index is specified in the searchKey parameter
     */
    public <T extends IIndexable> Aggregations getAggregations(SearchKey<?, T> searchKey,
                                                               ICriterion criterion,
                                                               Collection<QueryableAttribute> attributes) {
        return esRepository.getAggregations(searchKey, criterion, attributes);
    }

    /**
     * Retrieve the desired specific aggregations using parallel search requests.
     * Index is specified in the searchKey parameter
     */
    public <T extends IIndexable> Map<String, AggregationSearchContextResponse> getMultiAggregationsFor(Map<String, AggregationSearchContext<T>> searchRequests) {
        return esRepository.getMultiAggregationsFor(searchRequests);
    }

    /**
     * Retrieve unique sorted string attribute values following given request
     * <p>
     * Index is specified in the searchKey parameter
     */
    public <T extends IIndexable> SortedSet<String> uniqueAlphaSorted(SearchKey<?, T> searchKey,
                                                                      ICriterion crit,
                                                                      String attName,
                                                                      int maxCount) {
        return esRepository.uniqueAlphaSorted(searchKey, crit, attName, maxCount);
    }

    /**
     * Searching specified page of elements from index giving page size
     * <p>
     * Index is specified in the searchKey parameter
     */
    public <T extends IIndexable> Page<T> multiFieldsSearch(SearchKey<T, T> searchKey,
                                                            Pageable pageRequest,
                                                            Object inValue,
                                                            String... fields) {
        return esRepository.multiFieldsSearch(searchKey, pageRequest, inValue, fields);
    }

    /**
     * Fill DocFilesSummary for given request distributing results based on discriminantProperty for given file
     * types. Only external data files with an http or https uri are taken into account. This uri is used to count
     * files. No sum is computed.
     * <p>
     * Index is specified in the searchKey parameter
     */
    public <T extends IIndexable & IDocFiles> void computeExternalDataFilesSummary(SearchKey<T, T> searchKey,
                                                                                   ICriterion crit,
                                                                                   String discriminantProperty,
                                                                                   Optional<String> discriminentPropertyInclude,
                                                                                   DocFilesSummary summary,
                                                                                   String... fileTypes) {
        esRepository.computeExternalDataFilesSummary(searchKey,
                                                     crit,
                                                     discriminantProperty,
                                                     discriminentPropertyInclude,
                                                     summary,
                                                     fileTypes);
    }

    /**
     * Fill DocFilesSummary for given request distributing results based on discriminantProperty for given file
     * types. Only internal data files with a strictly positive size are taken into account. This size is used to count
     * files and to compute sum.
     * <p>
     * Index is specified in the searchKey parameter
     */
    public <T extends IIndexable & IDocFiles> void computeInternalDataFilesSummary(SearchKey<T, T> searchKey,
                                                                                   ICriterion crit,
                                                                                   String discriminantProperty,
                                                                                   Optional<String> discriminentPropertyInclude,
                                                                                   DocFilesSummary summary,
                                                                                   String... fileTypes) {
        esRepository.computeInternalDataFilesSummary(searchKey,
                                                     crit,
                                                     discriminantProperty,
                                                     discriminentPropertyInclude,
                                                     summary,
                                                     fileTypes);
    }


    /* =========================
         Helper
       ========================= */

    /**
     * Execute the operation on the building index (if present) and then on the tenant alias
     */
    public void runOnAliasAndBuildingIndex(String tenant, Consumer<String> op) {
        indexAliasResolver.resolveBuildingIndex(tenant).ifPresent(op);
        op.accept(resolveAliasName(tenant));
    }

}
