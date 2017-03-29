package fr.cnes.regards.modules.indexer.dao;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.elasticsearch.action.DocWriteResponse.Result;
import org.elasticsearch.action.admin.indices.mapping.get.GetFieldMappingsResponse;
import org.elasticsearch.action.admin.indices.mapping.get.GetFieldMappingsResponse.FieldMappingMetaData;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.transport.NoNodeAvailableException;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.cluster.node.DiscoveryNode;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.collect.Tuple;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.bucket.range.Range.Bucket;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.metrics.percentiles.Percentiles;
import org.elasticsearch.search.sort.SortOrder;
import org.elasticsearch.transport.client.PreBuiltTransportClient;
import org.jboss.netty.handler.timeout.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.google.common.base.Joiner;
import com.google.common.base.Throwables;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Iterables;
import com.google.common.collect.Range;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import fr.cnes.regards.framework.gson.adapters.LocalDateTimeAdapter;
import fr.cnes.regards.modules.indexer.dao.builder.AggregationBuilderFacetTypeVisitor;
import fr.cnes.regards.modules.indexer.dao.builder.QueryBuilderCriterionVisitor;
import fr.cnes.regards.modules.indexer.domain.IIndexable;
import fr.cnes.regards.modules.indexer.domain.SearchKey;
import fr.cnes.regards.modules.indexer.domain.criterion.ICriterion;
import fr.cnes.regards.modules.indexer.domain.facet.DateFacet;
import fr.cnes.regards.modules.indexer.domain.facet.FacetType;
import fr.cnes.regards.modules.indexer.domain.facet.IFacet;
import fr.cnes.regards.modules.indexer.domain.facet.NumericFacet;
import fr.cnes.regards.modules.indexer.domain.facet.StringFacet;

/**
 * Elasticsearch repository implementation
 */
@Repository
//@PropertySource("classpath:es.properties")
public class EsRepository implements IEsRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(EsRepository.class);

    /**
     * Scrolling keeping alive Time in ms when searching into Elasticsearch
     */
    private static final int KEEP_ALIVE_SCROLLING_TIME_MS = 10000;

    /**
     * Default number of hits retrieved by scrolling
     */
    private static final int DEFAULT_SCROLLING_HITS_SIZE = 100;

    /**
     * Maximum number of retries after a timeout
     */
    private static final int MAX_TIMEOUT_RETRIES = 3;

    /**
     * Target forwarding search {@link EsRepository#searchAll(String, Class, Consumer, ICriterion, String)} need to
     * put in cache search because of pagination restrictions.
     * This constant specifies duration cache time in minutes (from last access)
     */
    private static final int TARGET_FORWARDING_CACHE_MN = 3;

    /**
     * QueryBuilder visitor used for Elasticsearch search requests
     */
    private static final QueryBuilderCriterionVisitor CRITERION_VISITOR = new QueryBuilderCriterionVisitor();

    /**
     * AggregationBuilder visitor used for Elasticsearch search requests with facets
     */
    private static final AggregationBuilderFacetTypeVisitor FACET_VISITOR = new AggregationBuilderFacetTypeVisitor();

    /**
     * Empty JSon object
     */
    private static final String EMPTY_JSON = "{}";

    /**
     * Geometry field name
     */
    private static final String GEOM_NAME = "geometry";

    /**
     * Geometry field mapping properties
     */
    private static final String GEOM_TYPE_PROP = "type=geo_shape";

    /**
     * Elasticsearch port
     */
    private String esClusterName;

    /**
     * Elasticsearch host
     */
    private String esHost;

    /**
     * Elasticsearch address
     */
    private String esAddress;

    /**
     * Elasticsearch TCP port
     */
    private int esPort = 9300;

    /**
     * Client to ElasticSearch base
     */
    private final TransportClient client;

    /**
     * Json mapper
     */
    private final Gson gson;

    /**
     * Constructor
     *
     * @param pGson
     *            JSon mapper bean
     */
    public EsRepository(@Autowired Gson pGson, @Value("${elasticsearch.host:}") String pEsHost,
            @Value("${elasticsearch.address:}") String pEsAddress, @Value("${elasticsearch.tcp.port}") int pEsPort,
            @Value("${elasticsearch.cluster.name}") String pEsClusterName) {
        this.gson = pGson;
        this.esHost = Strings.isEmpty(pEsHost) ? null : pEsHost;
        this.esAddress = Strings.isEmpty(pEsAddress) ? null : pEsAddress;
        this.esPort = pEsPort;
        this.esClusterName = pEsClusterName;
        client = new PreBuiltTransportClient(Settings.builder().put("cluster.name", esClusterName).build());
        try {
            client.addTransportAddress(new InetSocketTransportAddress(
                    InetAddress.getByName((esHost != null) ? esHost : esAddress), esPort));
        } catch (final UnknownHostException e) {
            Throwables.propagate(e);
        }
        // Testinf availability of ES
        List<DiscoveryNode> nodes = client.connectedNodes();
        if (nodes.isEmpty()) {
            throw new NoNodeAvailableException("Elasticsearch is down");
        }
    }

    @Override
    public void close() {
        client.close();
    }

    @Override
    public boolean createIndex(String pIndex) {
        return client.admin().indices().prepareCreate(pIndex.toLowerCase()).get().isAcknowledged();
    }

    @Override
    public boolean setGeometryMapping(String pIndex, String... types) {
        String index = pIndex.toLowerCase();
        return Arrays.stream(types).map(type -> client.admin().indices().preparePutMapping(index).setType(type)
                .setSource(GEOM_NAME, GEOM_TYPE_PROP).get().isAcknowledged()).allMatch(ack -> (ack == true));
    }

    @Override
    public boolean delete(String pIndex, String pType, String pId) {
        final DeleteResponse response = client.prepareDelete(pIndex.toLowerCase(), pType, pId).get();
        return ((response.getResult() == Result.DELETED) || (response.getResult() == Result.NOT_FOUND));
    }

    @Override
    public boolean deleteIndex(String pIndex) {
        return client.admin().indices().prepareDelete(pIndex.toLowerCase()).get().isAcknowledged();

    }

    @Override
    public String[] findIndices() {
        return Iterables
                .toArray(Iterables.transform(client.admin().indices().prepareGetSettings().get().getIndexToSettings(),
                                             (pSetting) -> pSetting.key),
                         String.class);
    }

    @Override
    public <T extends IIndexable> T get(String pIndex, String pType, String pId, Class<T> pClass) {
        try {
            final GetResponse response = client.prepareGet(pIndex.toLowerCase(), pType, pId).get();
            if (!response.isExists()) {
                return null;
            }
            return gson.fromJson(response.getSourceAsString(), pClass);
        } catch (final JsonSyntaxException e) {
            throw Throwables.propagate(e);
        }
    }

    @Override
    public boolean indexExists(String pName) {
        return client.admin().indices().prepareExists(pName.toLowerCase()).get().isExists();
    }

    @Override
    public boolean merge(String pIndex, String pType, String pId, Map<String, Object> pMergedPropertiesMap) {
        try {
            final Map<String, Map<String, Object>> mapMap = new HashMap<>();
            try (XContentBuilder builder = XContentFactory.jsonBuilder().startObject()) {
                for (final Map.Entry<String, Object> entry : pMergedPropertiesMap.entrySet()) {
                    // Simple key = value
                    if (!entry.getKey().contains(".")) {
                        builder.field(entry.getKey(), entry.getValue());
                    } else { // Complex key => key.subKey = value
                        final String name = entry.getKey().substring(0, entry.getKey().indexOf('.'));
                        if (!mapMap.containsKey(name)) {
                            mapMap.put(name, new HashMap<>());
                        }
                        final Map<String, Object> subMap = mapMap.get(name);
                        subMap.put(entry.getKey().substring(entry.getKey().indexOf('.') + 1), entry.getValue());
                    }
                }
                // Pending sub objects ?
                if (!mapMap.isEmpty()) {
                    for (final Map.Entry<String, Map<String, Object>> entry : mapMap.entrySet()) {
                        builder.field(entry.getKey(), entry.getValue());
                    }
                }
                final UpdateResponse response = client.prepareUpdate(pIndex.toLowerCase(), pType, pId)
                        .setDoc(builder.endObject()).get();
                return (response.getResult() == Result.UPDATED);
            }
        } catch (final IOException jpe) {
            throw Throwables.propagate(jpe);
        }
    }

    private void checkDocument(IIndexable pDoc) {
        if (Strings.isNullOrEmpty(pDoc.getDocId()) || Strings.isNullOrEmpty(pDoc.getType())) {
            throw new IllegalArgumentException("docId and type are mandatory on an IIndexable object");
        }
    }

    @Override
    public boolean save(String pIndex, IIndexable pDocument) {
        checkDocument(pDocument);
        final IndexResponse response = client
                .prepareIndex(pIndex.toLowerCase(), pDocument.getType(), pDocument.getDocId())
                .setSource(gson.toJson(pDocument)).get();
        return (response.getResult() == Result.CREATED);
    }

    @Override
    public void refresh(String pIndex) {
        // To make just saved documents searchable, the associated index must be refreshed
        client.admin().indices().prepareRefresh(pIndex.toLowerCase()).get();
    }

    @Override
    public <T extends IIndexable> int saveBulk(String pIndex, @SuppressWarnings("unchecked") T... documents) {
        if (documents.length == 0) {
            return 0;
        }
        String index = pIndex.toLowerCase();
        for (T doc : documents) {
            checkDocument(doc);
        }
        int savedDocCount = 0;
        final BulkRequestBuilder bulkRequest = client.prepareBulk();
        for (T doc : documents) {
            bulkRequest.add(client.prepareIndex(index, doc.getType(), doc.getDocId()).setSource(gson.toJson(doc)));
        }
        final BulkResponse response = bulkRequest.get();
        for (final BulkItemResponse itemResponse : response.getItems()) {
            if (itemResponse.isFailed()) {
                LOGGER.warn(String.format("Document of type %s of id %s cannot be saved", documents[0].getClass(),
                                          itemResponse.getId()),
                            itemResponse.getFailure().getCause());
            } else {
                savedDocCount++;
            }
        }
        // To make just saved documents searchable, the associated index must be refreshed
        client.admin().indices().prepareRefresh(index).get();
        return savedDocCount;
    }

    @Override
    public <T> void searchAll(SearchKey<T, T> searchKey, Consumer<T> pAction, ICriterion pCrit) {
        SearchRequestBuilder requestBuilder = client.prepareSearch(searchKey.getSearchIndex().toLowerCase());
        requestBuilder = requestBuilder.setTypes(searchKey.getSearchTypes());
        SearchResponse scrollResp = requestBuilder.setScroll(new TimeValue(KEEP_ALIVE_SCROLLING_TIME_MS))
                .setQuery(pCrit.accept(CRITERION_VISITOR)).setSize(DEFAULT_SCROLLING_HITS_SIZE).get();
        // Scroll until no hits are returned
        do {
            for (final SearchHit hit : scrollResp.getHits().getHits()) {
                pAction.accept(gson.fromJson(hit.getSourceAsString(), searchKey.fromType(hit.getType())));
            }

            scrollResp = client.prepareSearchScroll(scrollResp.getScrollId())
                    .setScroll(new TimeValue(KEEP_ALIVE_SCROLLING_TIME_MS)).execute().actionGet();
        } while (scrollResp.getHits().getHits().length != 0); // Zero hits mark the end of the scroll and the while
                                                              // loop.
    }

    @Override
    public <R> void searchAll(SearchKey<?, R> searchKey, Consumer<R> action, ICriterion crit, String attributeSource) {
        // If attribute source is 'toto.titi.tutu', result from ES is '{"toto":{"titi":{"tutu":{...}}}}'
        // We just want "{...}"
        String startJsonResultStr = attributeSource;
        // BY default, if attributeSource does not contain '.', only one closing brace exists
        int closingBracesCount = 1;
        if (startJsonResultStr.contains(".")) {
            String[] terms = startJsonResultStr.split("\\.");
            startJsonResultStr = Joiner.on("\":{\"").join(terms);
            closingBracesCount = terms.length;
        }
        startJsonResultStr = "{\"" + startJsonResultStr + "\":";

        SearchRequestBuilder searchRequest = client.prepareSearch(searchKey.getSearchIndex().toLowerCase());
        searchRequest = searchRequest.setTypes(searchKey.getSearchTypes());
        SearchResponse scrollResp = searchRequest.setScroll(new TimeValue(KEEP_ALIVE_SCROLLING_TIME_MS))
                .setQuery(crit.accept(CRITERION_VISITOR)).setFetchSource(attributeSource, null)
                .setSize(DEFAULT_SCROLLING_HITS_SIZE).get();
        int startIdx = startJsonResultStr.length();
        // Scroll until no hits are returned
        do {
            for (final SearchHit hit : scrollResp.getHits().getHits()) {
                String source = hit.getSourceAsString();
                if (!source.equals(EMPTY_JSON)) {
                    action.accept(gson.fromJson(source.substring(startIdx, source.length() - closingBracesCount),
                                                searchKey.getResultClass()));
                }
            }

            scrollResp = client.prepareSearchScroll(scrollResp.getScrollId())
                    .setScroll(new TimeValue(KEEP_ALIVE_SCROLLING_TIME_MS)).execute().actionGet();
        } while (scrollResp.getHits().getHits().length != 0); // Zero hits mark the end of the scroll and the while
                                                              // loop.
    }

    @Override
    public <T> Page<T> searchAllLimited(String pIndex, Class<T> pClass, Pageable pPageRequest) {
        try {
            final List<T> results = new ArrayList<>();
            SearchRequestBuilder request = client.prepareSearch(pIndex.toLowerCase()).setFrom(pPageRequest.getOffset())
                    .setSize(pPageRequest.getPageSize());
            SearchResponse response = getWithTimeouts(request);
            SearchHits hits = response.getHits();
            for (SearchHit hit : hits) {
                results.add(gson.fromJson(hit.getSourceAsString(), pClass));
            }
            return new PageImpl<>(results, pPageRequest, response.getHits().getTotalHits());
        } catch (final JsonSyntaxException e) {
            throw Throwables.propagate(e);
        }
    }

    @Override
    public <T> Page<T> search(SearchKey<T, T> searchKey, Pageable pPageRequest, ICriterion criterion,
            Map<String, FacetType> pFacetsMap, LinkedHashMap<String, Boolean> pAscSortMap) {
        String index = searchKey.getSearchIndex().toLowerCase();
        try {
            final List<T> results = new ArrayList<>();
            // Use filter instead of "direct" query (in theory, quickest because no score is computed)
            QueryBuilder critBuilder = QueryBuilders.boolQuery().must(QueryBuilders.matchAllQuery())
                    .filter(criterion.accept(CRITERION_VISITOR));
            SearchRequestBuilder request = client.prepareSearch(index).setTypes(searchKey.getSearchTypes());
            request = request.setQuery(critBuilder).setFrom(pPageRequest.getOffset())
                    .setSize(pPageRequest.getPageSize());
            if (pAscSortMap != null) {
                manageSortRequest(index, request, pAscSortMap);
            }
            // Managing aggregations if some facets are asked
            boolean twoPassRequestNeeded = manageFirstPassRequestAggregations(pFacetsMap, request);
            // Launch the request
            SearchResponse response = getWithTimeouts(request);

            // At least one numeric facet is present, we need to replace all numeric facets by associated range facets
            if (twoPassRequestNeeded) {
                // Rebuild request
                request = client.prepareSearch(index).setTypes(searchKey.getSearchTypes());
                request = request.setQuery(critBuilder).setFrom(pPageRequest.getOffset())
                        .setSize(pPageRequest.getPageSize());
                Map<String, Aggregation> aggsMap = response.getAggregations().asMap();
                manageSecondPassRequestAggregations(pFacetsMap, request, aggsMap);
                // Relaunch the request with replaced facets
                response = getWithTimeouts(request);
            }

            Set<IFacet<?>> facetResults = null;
            if (pFacetsMap != null) {
                // Get the new aggregations result map
                Map<String, Aggregation> aggsMap = response.getAggregations().asMap();
                // Create the facet map
                facetResults = new HashSet<>();
                for (Map.Entry<String, FacetType> entry : pFacetsMap.entrySet()) {
                    FacetType facetType = entry.getValue();
                    String attributeName = entry.getKey();
                    fillFacets(aggsMap, facetResults, facetType, attributeName);
                }
            }

            SearchHits hits = response.getHits();
            for (SearchHit hit : hits) {
                results.add(gson.fromJson(hit.getSourceAsString(), searchKey.fromType(hit.getType())));
            }
            // If no facet, juste returns a "simple" Page
            if (facetResults == null) {
                return new PageImpl<>(results, pPageRequest, response.getHits().getTotalHits());
            } else { // else returns a FacetPage
                return new FacetPage<>(results, facetResults, pPageRequest, response.getHits().getTotalHits());
            }
        } catch (final JsonSyntaxException e) {
            throw Throwables.propagate(e);
        }
    }

    /**
     * Utility class to create a quadruple key (a pair of pairs) used by loading cache mechanism
     */
    private static class CacheKey extends Tuple<SearchKey<?, ?>, Tuple<ICriterion, String>> {

        public CacheKey(SearchKey<?, ?> searchKey, ICriterion v2, String v3) {
            super(searchKey, new Tuple<>(v2, v3));
        }

        public SearchKey<?, ?> getV1() {
            return v1();
        }

        public ICriterion getV2() {
            return v2().v1();
        }

        public String getV3() {
            return v2().v2();
        }
    }

    /**
     * SearchAll cache used by {@link EsRepository#searchAll(String, Class, Consumer, ICriterion, String)} to
     * avoid redo same ES request while changing page. SortedSet is necessary to be sure several consecutive
     * calls return same ordered set
     */
    private LoadingCache<CacheKey, SortedSet<Object>> searchAllCache = CacheBuilder.newBuilder()
            .expireAfterAccess(TARGET_FORWARDING_CACHE_MN, TimeUnit.MINUTES)
            .build(new CacheLoader<CacheKey, SortedSet<Object>>() {

                @Override
                public SortedSet<Object> load(CacheKey key) throws Exception {
                    // Using method Objects.hashCode(Object) to compare to be sure that the set will always be returned
                    // with same order
                    SortedSet<Object> results = new TreeSet<>(Comparator.comparing(Objects::hashCode));
                    searchAll(key.getV1(), results::add, key.getV2(), key.getV3());
                    return results;
                };

            });

    @SuppressWarnings("unchecked")
    @Override
    public <R> List<R> search(SearchKey<?, R> searchKey, ICriterion criterion, String sourceAttribute) {
        try {
            SortedSet<Object> objects = searchAllCache
                    .getUnchecked(new CacheKey(searchKey, criterion, sourceAttribute));
            return objects.stream().map(o -> (R) o).collect(Collectors.toList());
        } catch (final JsonSyntaxException e) {
            throw Throwables.propagate(e);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <R, U> List<U> search(SearchKey<?, R> searchKey, ICriterion criterion, String sourceAttribute,
            Function<R, U> transformFct) {
        try {
            SortedSet<Object> objects = searchAllCache
                    .getUnchecked(new CacheKey(searchKey, criterion, sourceAttribute));
            return objects.stream().map(o -> (R) o).map(transformFct).collect(Collectors.toList());
        } catch (final JsonSyntaxException e) {
            throw Throwables.propagate(e);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <R, U> List<U> search(SearchKey<?, R[]> searchKey, ICriterion criterion, String sourceAttribute,
            Predicate<R> filterPredicate, Function<R, U> transformFct) {
        try {
            SortedSet<Object> objects = searchAllCache
                    .getUnchecked(new CacheKey(searchKey, criterion, sourceAttribute));
            return objects.stream().flatMap(o -> Arrays.stream((R[]) o)).distinct().filter(filterPredicate)
                    .map(transformFct).collect(Collectors.toList());
        } catch (final JsonSyntaxException e) {
            throw Throwables.propagate(e);
        }
    }

    /**
     * Add sort to the request
     * @param request search request
     * @param pAscSortMap map(attribute name, true if ascending)
     */
    private void manageSortRequest(String pIndex, SearchRequestBuilder request,
            LinkedHashMap<String, Boolean> pAscSortMap) {
        // Because string attributes are not indexed with Elasticsearch, it is necessary to add ".keyword" at
        // end of attribute name into sort request. So we need to know string attributes
        GetFieldMappingsResponse response = client.admin().indices().prepareGetFieldMappings(pIndex)
                .setFields(pAscSortMap.keySet().toArray(new String[pAscSortMap.size()])).get();
        // map(type, map(field, metadata)) for asked index
        Map<String, Map<String, FieldMappingMetaData>> mappings = response.mappings().get(pIndex);
        // All types mappings are retrieved.
        // NOTE: in our context, attributes have same metada for all types so once we found one, we stop.
        // To do that, we create a new LinkedHashMap to KEEPS keys order !!! (crucial)
        LinkedHashMap<String, Boolean> updatedAscSortMap = new LinkedHashMap<>(pAscSortMap.size());
        for (Map.Entry<String, Boolean> sortEntry : pAscSortMap.entrySet()) {
            String attributeName = sortEntry.getKey();
            // "terminal" field name ie. for "toto.titi.tutu" => "tutu"
            String lastPathAttName = attributeName.contains(".")
                    ? attributeName.substring(attributeName.lastIndexOf('.') + 1) : attributeName;
            // For all type mappings
            boolean typeText = false;
            for (Map.Entry<String, Map<String, FieldMappingMetaData>> typeEntry : mappings.entrySet()) {
                // Once we found attribute name on one type, we stop to next attribute
                if (typeEntry.getValue().containsKey(attributeName)) {
                    FieldMappingMetaData attMetaData = typeEntry.getValue().get(attributeName);
                    // If field type is String, we must add ".keyword" to attribute name
                    Map<String, Object> metaDataMap = attMetaData.sourceAsMap();
                    if ((metaDataMap.get(lastPathAttName) != null)
                            && (metaDataMap.get(lastPathAttName) instanceof Map)) {
                        Map<?, ?> mappingMap = (Map<?, ?>) metaDataMap.get(lastPathAttName);
                        // Should contains "type" field but...
                        if (mappingMap.containsKey("type")) {
                            if (mappingMap.get("type").equals("text")) {
                                updatedAscSortMap.put(attributeName + ".keyword", sortEntry.getValue());
                                typeText = true;
                                break;
                            } else { // "type" field found => it's not "text"
                                typeText = false;
                                break;
                            }
                        }
                    }
                }
            }
            // Not "text" type => add the key/value to keep the sort order
            if (!typeText) {
                updatedAscSortMap.put(attributeName, sortEntry.getValue());
            }
        }
        // Add sort to request
        updatedAscSortMap.entrySet()
                .forEach(entry -> request.addSort(entry.getKey(), entry.getValue() ? SortOrder.ASC : SortOrder.DESC));
    }

    /**
     * Add aggregations to the search request.
     * @param pFacetsMap asked facets
     * @param request search request
     * @return true if a second pass is needed (managing range facets)
     */
    private boolean manageFirstPassRequestAggregations(Map<String, FacetType> pFacetsMap,
            SearchRequestBuilder request) {
        boolean twoPassRequestNeeded = false;
        if (pFacetsMap != null) {
            // Numeric/date facets needs :
            // First to add a percentiles aggregation to retrieved 9 values corresponding to
            // 10%, 20 %, ..., 90 % of the values
            // Secondly to add a range aggregation with these 9 values in order to retrieve 10 buckets of equal
            // size of values
            for (Map.Entry<String, FacetType> entry : pFacetsMap.entrySet()) {
                FacetType facetType = entry.getValue();
                if ((facetType == FacetType.NUMERIC) || (facetType == FacetType.DATE)) {
                    twoPassRequestNeeded = true;
                }
                request.addAggregation(facetType.accept(FACET_VISITOR, entry.getKey()));
            }
        }
        return twoPassRequestNeeded;
    }

    /**
     * Add aggregations to the search request (second pass). For range aggregations, use percentiles results
     * (from first pass request results) to create range aggregagtions.
     * @param pFacetsMap asked facets
     * @param request search request
     * @param aggsMap first pass aggregagtions results
     */
    private void manageSecondPassRequestAggregations(Map<String, FacetType> pFacetsMap, SearchRequestBuilder request,
            Map<String, Aggregation> aggsMap) {
        for (Map.Entry<String, FacetType> entry : pFacetsMap.entrySet()) {
            FacetType facetType = entry.getValue();
            String attributeName = entry.getKey();
            // Replace percentiles aggregations by range aggregagtions
            if (facetType == FacetType.NUMERIC) {
                Percentiles percentiles = (Percentiles) aggsMap
                        .get(attributeName + AggregationBuilderFacetTypeVisitor.NUMERIC_FACET_POSTFIX);
                request.addAggregation(FacetType.RANGE.accept(FACET_VISITOR, attributeName, percentiles));
            } else if (facetType == FacetType.DATE) {
                Percentiles percentiles = (Percentiles) aggsMap
                        .get(attributeName + AggregationBuilderFacetTypeVisitor.DATE_FACET_POSTFIX);
                request.addAggregation(FacetType.RANGE.accept(FACET_VISITOR, attributeName, percentiles));
            } else { // Let it as upper
                request.addAggregation(facetType.accept(FACET_VISITOR, attributeName));
            }
        }
    }

    /**
     * Compute aggregations results to fill results facet map of an attribute
     * @param aggsMap aggregation resuls map
     * @param facets map of results facets
     * @param facetType type of facet for given attribute
     * @param attributeName given attribute
     */
    private void fillFacets(Map<String, Aggregation> aggsMap, Set<IFacet<?>> facets, FacetType facetType,
            String attributeName) {
        switch (facetType) {
            case STRING: {
                Terms terms = (Terms) aggsMap
                        .get(attributeName + AggregationBuilderFacetTypeVisitor.STRING_FACET_POSTFIX);
                if (terms.getBuckets().isEmpty()) {
                    return;
                }
                Map<String, Long> valueMap = new LinkedHashMap<>(terms.getBuckets().size());
                terms.getBuckets().forEach(b -> valueMap.put(b.getKeyAsString(), b.getDocCount()));
                facets.add(new StringFacet(attributeName, valueMap));
                break;
            }
            case NUMERIC: {
                org.elasticsearch.search.aggregations.bucket.range.Range numRange = (org.elasticsearch.search.aggregations.bucket.range.Range) aggsMap
                        .get(attributeName + AggregationBuilderFacetTypeVisitor.RANGE_FACET_POSTFIX);
                Map<Range<Double>, Long> valueMap = new LinkedHashMap<>();
                for (Bucket bucket : numRange.getBuckets()) {
                    // Case with no value : every bucket has a NaN value (as from, to or both)
                    if (Objects.equals(bucket.getTo(), Double.NaN) || Objects.equals(bucket.getFrom(), Double.NaN)) {
                        // If first bucket contains NaN value, it means there are no value at all
                        return;
                    }
                    Range<Double> valueRange;
                    // (-∞ -> ?
                    if (Objects.equals(bucket.getFrom(), Double.NEGATIVE_INFINITY)) {
                        // (-∞ -> +∞) (completely dumb but...who knows ?)
                        if (Objects.equals(bucket.getTo(), Double.POSITIVE_INFINITY)) {
                            valueRange = Range.all();
                        } else { // (-∞ -> value]
                            valueRange = Range.atMost((Double) bucket.getTo());
                        }
                    } else if (Objects.equals(bucket.getTo(), Double.POSITIVE_INFINITY)) { // ? -> +∞)
                        valueRange = Range.greaterThan((Double) bucket.getFrom());
                    } else { // [value -> value)
                        valueRange = Range.closedOpen((Double) bucket.getFrom(), (Double) bucket.getTo());
                    }
                    valueMap.put(valueRange, bucket.getDocCount());
                }
                facets.add(new NumericFacet(attributeName, valueMap));
                break;
            }
            case DATE: {
                org.elasticsearch.search.aggregations.bucket.range.Range dateRange = (org.elasticsearch.search.aggregations.bucket.range.Range) aggsMap
                        .get(attributeName + AggregationBuilderFacetTypeVisitor.RANGE_FACET_POSTFIX);
                Map<com.google.common.collect.Range<LocalDateTime>, Long> valueMap = new LinkedHashMap<>();
                for (Bucket bucket : dateRange.getBuckets()) {
                    Range<LocalDateTime> valueRange;
                    // Case with no value : every bucket has a NaN value (as from, to or both)
                    if (Objects.equals(bucket.getTo(), Double.NaN) || Objects.equals(bucket.getFrom(), Double.NaN)) {
                        // If first bucket contains NaN value, it means there are no value at all
                        return;
                    }
                    // (-∞ -> ?
                    if (bucket.getFromAsString() == null) {
                        // (-∞ -> +∞) (completely dumb but...who knows ?)
                        if (bucket.getToAsString() == null) {
                            valueRange = Range.all();
                        } else { // (-∞ -> value]
                            valueRange = Range.atMost(LocalDateTimeAdapter.parse(bucket.getToAsString()));
                        }
                    } else if (bucket.getToAsString() == null) { // ? -> +∞)
                        valueRange = Range.greaterThan(LocalDateTimeAdapter.parse(bucket.getFromAsString()));
                    } else { // [value -> value)
                        valueRange = Range.closedOpen(LocalDateTimeAdapter.parse(bucket.getFromAsString()),
                                                      LocalDateTimeAdapter.parse(bucket.getToAsString()));
                    }
                    valueMap.put(valueRange, bucket.getDocCount());
                }
                facets.add(new DateFacet(attributeName, valueMap));
                break;
            }
            default:
                break;
        }
    }

    @Override
    public <T> Page<T> multiFieldsSearch(SearchKey<T, T> searchKey, Pageable pPageRequest, Object pValue,
            String... pFields) {
        try {
            final List<T> results = new ArrayList<>();
            // LocalDateTime must be formatted to be correctly used following Gson mapping
            Object value = (pValue instanceof LocalDateTime) ? LocalDateTimeAdapter.format((LocalDateTime) pValue)
                    : pValue;
            QueryBuilder queryBuilder = QueryBuilders.boolQuery().must(QueryBuilders.matchAllQuery())
                    .filter(QueryBuilders.multiMatchQuery(value, pFields));
            SearchRequestBuilder request = client.prepareSearch(searchKey.getSearchIndex().toLowerCase());
            request = request.setTypes(searchKey.getSearchTypes());
            request = request.setQuery(queryBuilder).setFrom(pPageRequest.getOffset())
                    .setSize(pPageRequest.getPageSize());
            SearchResponse response = getWithTimeouts(request);
            SearchHits hits = response.getHits();
            for (SearchHit hit : hits) {
                results.add(gson.fromJson(hit.getSourceAsString(), searchKey.fromType(hit.getType())));
            }
            return new PageImpl<>(results, pPageRequest, response.getHits().getTotalHits());
        } catch (final JsonSyntaxException e) {
            throw Throwables.propagate(e);
        }
    }

    /**
     * Call specified request at least MAX_TIMEOUT_RETRIES
     * @param request
     * @return
     */
    private SearchResponse getWithTimeouts(SearchRequestBuilder request) {
        SearchResponse response;
        int errorCount = 0;
        do {
            response = request.get();
            errorCount += response.isTimedOut() ? 1 : 0;
            if (errorCount == MAX_TIMEOUT_RETRIES) {
                throw new TimeoutException(
                        String.format("Get %d timeouts while attempting to retrieve data", MAX_TIMEOUT_RETRIES));
            }
        } while (response.isTimedOut());
        return response;
    }
}
