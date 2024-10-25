/*
 * Copyright 2017-2024 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.indexer.dao;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Joiner;
import com.google.common.base.Throwables;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Range;
import com.google.gson.*;
import fr.cnes.regards.framework.geojson.geometry.IGeometry;
import fr.cnes.regards.framework.geojson.geometry.MultiPolygon;
import fr.cnes.regards.framework.geojson.geometry.Polygon;
import fr.cnes.regards.framework.gson.adapters.OffsetDateTimeAdapter;
import fr.cnes.regards.framework.module.rest.exception.TooManyResultsException;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.utils.RsRuntimeException;
import fr.cnes.regards.modules.dam.domain.entities.DataObject;
import fr.cnes.regards.modules.dam.domain.entities.StaticProperties;
import fr.cnes.regards.modules.dam.domain.entities.feature.DataObjectFeature;
import fr.cnes.regards.modules.indexer.dao.builder.AggregationBuilderFacetTypeVisitor;
import fr.cnes.regards.modules.indexer.dao.builder.GeoCriterionWithCircleVisitor;
import fr.cnes.regards.modules.indexer.dao.builder.GeoCriterionWithPolygonOrBboxVisitor;
import fr.cnes.regards.modules.indexer.dao.builder.QueryBuilderCriterionVisitor;
import fr.cnes.regards.modules.indexer.dao.converter.SortToLinkedHashMap;
import fr.cnes.regards.modules.indexer.dao.deser.JsonDeserializeStrategy;
import fr.cnes.regards.modules.indexer.dao.exception.ESIndexNotFoundRuntimeException;
import fr.cnes.regards.modules.indexer.dao.exception.FieldNotIndexedRuntimeException;
import fr.cnes.regards.modules.indexer.dao.mapping.AttributeDescription;
import fr.cnes.regards.modules.indexer.dao.mapping.utils.AttrDescToJsonMapping;
import fr.cnes.regards.modules.indexer.dao.mapping.utils.JsonConverter;
import fr.cnes.regards.modules.indexer.dao.mapping.utils.JsonMerger;
import fr.cnes.regards.modules.indexer.dao.spatial.GeoHelper;
import fr.cnes.regards.modules.indexer.domain.*;
import fr.cnes.regards.modules.indexer.domain.aggregation.QueryableAttribute;
import fr.cnes.regards.modules.indexer.domain.criterion.*;
import fr.cnes.regards.modules.indexer.domain.facet.*;
import fr.cnes.regards.modules.indexer.domain.reminder.SearchAfterReminder;
import fr.cnes.regards.modules.indexer.domain.spatial.Crs;
import fr.cnes.regards.modules.indexer.domain.spatial.ILocalizable;
import fr.cnes.regards.modules.indexer.domain.summary.DocFilesSubSummary;
import fr.cnes.regards.modules.indexer.domain.summary.DocFilesSummary;
import fr.cnes.regards.modules.indexer.domain.summary.FilesSummary;
import org.apache.http.HttpHost;
import org.apache.http.HttpStatus;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.nio.entity.NStringEntity;
import org.apache.logging.log4j.util.Strings;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.DocWriteResponse.Result;
import org.elasticsearch.action.admin.indices.alias.IndicesAliasesRequest;
import org.elasticsearch.action.admin.indices.alias.IndicesAliasesRequest.AliasActions;
import org.elasticsearch.action.admin.indices.alias.IndicesAliasesRequest.AliasActions.Type;
import org.elasticsearch.action.admin.indices.alias.get.GetAliasesRequest;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.admin.indices.refresh.RefreshRequest;
import org.elasticsearch.action.admin.indices.settings.put.UpdateSettingsRequest;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.*;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.client.*;
import org.elasticsearch.client.HttpAsyncResponseConsumerFactory.HeapBufferedResponseConsumerFactory;
import org.elasticsearch.client.RequestOptions.Builder;
import org.elasticsearch.client.core.CountRequest;
import org.elasticsearch.client.core.CountResponse;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.CreateIndexResponse;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.client.indices.PutMappingRequest;
import org.elasticsearch.client.transport.NoNodeAvailableException;
import org.elasticsearch.cluster.metadata.AliasMetadata;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentHelper;
import org.elasticsearch.core.TimeValue;
import org.elasticsearch.core.Tuple;
import org.elasticsearch.index.IndexNotFoundException;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.filter.Filter;
import org.elasticsearch.search.aggregations.bucket.filter.FilterAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.range.Range.Bucket;
import org.elasticsearch.search.aggregations.bucket.terms.IncludeExclude;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.aggregations.metrics.*;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.elasticsearch.xcontent.XContentBuilder;
import org.elasticsearch.xcontent.XContentType;
import org.hipparchus.util.FastMath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.*;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.io.InputStream;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static fr.cnes.regards.modules.indexer.dao.builder.AggregationBuilderFacetTypeVisitor.DATE_FACET_SUFFIX;
import static fr.cnes.regards.modules.indexer.dao.builder.AggregationBuilderFacetTypeVisitor.NUMERIC_FACET_SUFFIX;
import static fr.cnes.regards.modules.indexer.dao.mapping.utils.AttrDescToJsonMapping.stringMapping;
import static fr.cnes.regards.modules.indexer.dao.mapping.utils.GsonBetter.*;
import static fr.cnes.regards.modules.indexer.dao.spatial.GeoHelper.AUTHALIC_SPHERE_RADIUS;

/**
 * Elasticsearch repository implementation
 *
 * @author oroussel
 */
@Repository
public class EsRepository implements IEsRepository {

    public static final String TOTAL_PREFIX = "total_";

    public static final String REF_FILES_COUNT_SUFFIX = "_ref_files_count";

    public static final String REF_FILES_SIZE_SUFFIX = "_ref_files_size";

    public static final String REF_SUFFIX = "_ref";

    public static final String NOT_REF_FILES_COUNT_SUFFIX = "_!ref_files_count";

    public static final String NOT_REF_FILES_SIZE_SUFFIX = "_!ref_files_size";

    public static final String NOT_REF_SUFFIX = "_!ref";

    public static final String FEATURE_FILES_PREFIX = "feature.files.";

    public static final String FILESIZE_SUFFIX = ".filesize";

    public static final String REFERENCE_SUFFIX = ".reference";

    public static final String INDEX_NUMBER_OF_SHARDS = "index.number_of_shards";

    public static final String INDEX_NUMBER_OF_REPLICAS = "index.number_of_replicas";

    public static final String INDEX_REFRESH_INTERVAL = "index.refresh_interval";

    public static final String MAPPING = "mapping";

    public static final String DOUBLE = "double";

    public static final String PROPERTIES = "properties";

    public static final String BOOLEAN = "boolean";

    public static final String VERSION = "version";

    private static final Logger LOGGER = LoggerFactory.getLogger(EsRepository.class);

    /**
     * Scrolling keeping alive Time in ms when searching into Elasticsearch
     * Set it to 10 minutes to avoid timeouts while scrolling.
     */
    private static final int KEEP_ALIVE_SCROLLING_TIME_MN = 10;

    /**
     * Default number of hits retrieved by scrolling (10 is the default value and according to doc is the best value)
     */
    private static final int DEFAULT_SCROLLING_HITS_SIZE = 100;

    /**
     * Target forwarding search {@link EsRepository#searchAll} need to put in cache search because of pagination
     * restrictions. This constant specifies duration cache time in minutes (from last access)
     */
    private static final int TARGET_FORWARDING_CACHE_MN = 3;

    /**
     * QueryBuilder visitor used for Elasticsearch search requests
     */
    private static final QueryBuilderCriterionVisitor CRITERION_VISITOR = new QueryBuilderCriterionVisitor();

    /**
     * Index used to store search after cursors
     */
    private static final String REMINDER_IDX = "reminder";

    /**
     * Suffix for text attributes
     */
    private static final String KEYWORD_SUFFIX = ".keyword";

    /**
     * Maximum duration for idle connections in the http config for ES client.
     * <br>
     * Setting a short "connection unused duration" to prevent "Connection Reset By Peer" IOException.
     * <br>
     * See https://stackoverflow.com/a/53003627/118437 for a discussion regarding this problem.
     * <br>
     * Internal ticket https://thor.si.c-s.fr/gf/project/regards/tracker/?action=TrackerItemEdit&tracker_item_id=196140
     * <br>
     * Setting the value to 10 minutes to be kept coherent with {@link #KEEP_ALIVE_SCROLLING_TIME_MN}.
     */
    private static final Long HTTP_KEEP_ALIVE_MAX_IDLE_DURATION_MS = 10L * 60 * 1000L;

    /**
     * Error raised by ES when REGARDS never sent objects to ES
     */
    private static final String INDEX_NOT_FOUND_EXCEPTION = "index_not_found_exception";

    /**
     * Error raised by ES when REGARDS attempts to search on a non-indexed field
     */
    private static final String QUERY_SHARD_EXCEPTION = "query_shard_exception";

    /**
     * Cause message sent by ES when REGARDS attempts to search on a non-indexed field
     */
    private static final String QUERY_SHARD_EXCEPTION_CAUSE_PATTERN = "Cannot search on field \\[(.*?)\\] since it is not indexed";

    /**
     * Single scheduled executor service to clean reminder tasks once expiration date is reached
     */
    private final ScheduledExecutorService reminderCleanExecutor = Executors.newSingleThreadScheduledExecutor();

    /**
     * AggregationBuilder visitor used for Elasticsearch search requests with facets
     */
    private final AggregationBuilderFacetTypeVisitor aggBuilderFacetTypeVisitor;

    /**
     * High level Rest API client
     */
    private final RestHighLevelClient client;

    /**
     * Json mapper
     */
    private final Gson gson;

    private final AttrDescToJsonMapping toMapping;

    private final JsonDeserializeStrategy<IIndexable> deserializeHitsStrategy;

    private IRuntimeTenantResolver tenantResolver;

    private RequestOptions searchOptions;

    /**
     * SearchAll cache used by {@link EsRepository#searchAll} to avoid redo same ES request while changing page.
     * SortedSet is necessary to be sure several consecutive calls return same ordered set
     */
    private final LoadingCache<CacheKey, Tuple<SortedSet<Object>, Set<IFacet<?>>>> searchAllCache = CacheBuilder.newBuilder()
                                                                                                                .expireAfterAccess(
                                                                                                                    TARGET_FORWARDING_CACHE_MN,
                                                                                                                    TimeUnit.MINUTES)
                                                                                                                .build(
                                                                                                                    new EsCacheLoader());

    private class EsCacheLoader extends CacheLoader<CacheKey, Tuple<SortedSet<Object>, Set<IFacet<?>>>> {

        @Override
        public Tuple<SortedSet<Object>, Set<IFacet<?>>> load(CacheKey key) {
            // Using method Objects.hashCode(Object) to compare to be sure that the set will always be returned
            // with same order
            return searchJoined(key.getSearchKey(), key.getCriterion(), key.getSourceAttribute(), key.getFacetsMap());
        }
    }

    private DefaultScrollClearResponseActionListener scrollClearListener = new DefaultScrollClearResponseActionListener();

    public EsRepository(Gson gson,
                        JsonDeserializeStrategy<IIndexable> deserStrategy,
                        AggregationBuilderFacetTypeVisitor aggBuilderFacetTypeVisitor,
                        AttrDescToJsonMapping toMapping,
                        IRuntimeTenantResolver tenantResolver,
                        @Value("${regards.elasticsearch.hosts:#{T(java.util.Collections).emptyList()}}")
                        List<String> esHosts,
                        @Value("${regards.elasticsearch.host:}") String esHost,
                        @Value("${regards.elasticsearch.http.port}") int esPort,
                        @Value("${regards.elasticsearch.http.protocol:http}") String esProtocol,
                        @Value("${regards.elasticsearch.http.username:}") String username,
                        @Value("${regards.elasticsearch.http.password:}") String password,
                        @Value("${regards.elasticsearch.http.buffer.limit:104857600}") int elasticClientBufferLimit,
                        @Value("${regards.elasticsearch.search.request.timeout:15000}") int searchRequestTimeout,
                        @Value("${regards.elasticsearch.index.request.timeout:1200000}") int indexRequestTimeout) {
        this.gson = gson;
        this.deserializeHitsStrategy = deserStrategy;
        this.aggBuilderFacetTypeVisitor = aggBuilderFacetTypeVisitor;
        this.toMapping = toMapping;
        this.tenantResolver = tenantResolver;

        // Connection properties
        List<String> httpHosts = esHosts;
        if (httpHosts.isEmpty()) {
            httpHosts = List.of(esHost);
        }
        String connectionInfoMessage = String.format(
            "Elastic search connection properties : protocol \"%s\", host \"%s\", port \"%d\"",
            esProtocol,
            esHost,
            esPort);
        LOGGER.info(connectionInfoMessage);

        // Timeouts are set to 20 minutes particularly for bulk save containing geo_shape
        RestClientBuilder restClientBuilder = RestClient.builder(httpHosts.stream()
                                                                          .map(host -> new HttpHost(host,
                                                                                                    esPort,
                                                                                                    esProtocol))
                                                                          .toArray(HttpHost[]::new))
                                                        .setRequestConfigCallback(requestConfigBuilder -> requestConfigBuilder.setSocketTimeout(
                                                            indexRequestTimeout));

        // Add auth when provided
        if (Strings.isNotBlank(username) && Strings.isNotBlank(password)) {
            final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
            credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(username, password));
            restClientBuilder.setHttpClientConfigCallback(httpClientBuilder -> httpClientBuilder.setDefaultCredentialsProvider(
                credentialsProvider));
        }

        Builder builder = RequestOptions.DEFAULT.toBuilder();
        if (elasticClientBufferLimit > 0) {
            builder.setHttpAsyncResponseConsumerFactory(new HeapBufferedResponseConsumerFactory(elasticClientBufferLimit));
        }
        // Specific search option to lower timeout
        searchOptions = builder.setRequestConfig(RequestConfig.custom().setSocketTimeout(searchRequestTimeout).build())
                               .build();

        client = new RestHighLevelClient(restClientBuilder);

        try {
            // Testing availability of ES
            if (!client.ping(RequestOptions.DEFAULT)) {
                throw new NoNodeAvailableException("Elasticsearch is down. " + connectionInfoMessage);
            }
        } catch (IOException | RuntimeException e) {
            throw new NoNodeAvailableException("Error while pinging Elasticsearch (" + connectionInfoMessage + ")", e);
        }
    }

    /**
     * Add document type (Elasticsearch prior to version 6 type) into criterion
     */
    private static ICriterion addTypes(ICriterion criterion, String... types) {
        // Beware if crit is null
        criterion = criterion == null ? ICriterion.all() : criterion;
        // Then add type
        switch (types.length) {
            case 0:
                return criterion;
            case 1:
                return ICriterion.and(ICriterion.eq("type", types[0], StringMatchType.KEYWORD), criterion);
            default:
                ICriterion orCrit = ICriterion.or(Arrays.stream(types)
                                                        .map(type -> ICriterion.eq("type",
                                                                                   type,
                                                                                   StringMatchType.KEYWORD))
                                                        .toArray(ICriterion[]::new));
                return ICriterion.and(orCrit, criterion);
        }
    }

    /**
     * If you think this method is horrible and completely useless (and bordel line) look at next method...
     */
    @SuppressWarnings("unchecked")
    private static Map<String, Object> toMap(Object o) {
        return (Map<String, Object>) o;
    }

    /**
     * Tell if given attribute is of type "text" from all types mappings of specified index.
     *
     * @param map response of "mappings" rest request
     * @return true is first type mapping found from given attribute is of type "text"
     */
    private static boolean isTextMapping(Map<String, Object> map, String attribute) {
        String lastPathAttName = attribute.contains(".") ?
            attribute.substring(attribute.lastIndexOf('.') + 1) :
            attribute;
        try {
            // Extract types mapping from the ES mapping response
            Iterator<Object> i = map.values().iterator();
            if (i.hasNext()) {
                Map<String, Object> allTypesMapping = toMap(toMap(i.next()).get("mappings"));
                // Check if the given attribute is present in the mappings, then return true if it is a "text" attribute
                if (allTypesMapping.containsKey(attribute)) {
                    return toMap(toMap(toMap(allTypesMapping.get(attribute)).get(MAPPING)).get(lastPathAttName)).get(
                        "type").equals("text");
                }
            }
            return false;
        } catch (NullPointerException e) { // NOSONAR (better catch a NPE than testing all imbricated maps)
            return false;
        }
    }

    @Override
    public void close() {
        LOGGER.info("Closing connection");
        try {
            client.close();
        } catch (IOException e) {
            LOGGER.error(e.getMessage(), e);
            throw new RsRuntimeException(e);
        }
    }

    @Override
    public boolean createIndex(String index) {
        return createIndex(index, CreateIndexConfiguration.DEFAULT);
    }

    @Override
    public boolean createIndex(String index, CreateIndexConfiguration configuration) {
        try {
            CreateIndexRequest request = new CreateIndexRequest(index.toLowerCase());

            // settings (define shards number)
            Settings settings = Settings.builder()
                                        .put(INDEX_NUMBER_OF_SHARDS, configuration.getNumberOfShards())
                                        .put(INDEX_NUMBER_OF_REPLICAS, configuration.getNumberOfReplicas())
                                        .build();
            request.settings(settings);

            // mapping (properties)
            XContentBuilder source = new JsonConverter().toXContentBuilder(baseJsonMapping());
            request.mapping(source);
            CreateIndexResponse response = client.indices().create(request, RequestOptions.DEFAULT);
            return response.isAcknowledged();
        } catch (IOException e) {
            LOGGER.error(e.getMessage(), e);
            throw new RsRuntimeException(e);
        }
    }

    private JsonObject baseJsonMapping() {
        //@formatter:off
        return object(kv("dynamic_templates",
                        array(object(kv("doubles",
                                object(kv("match_mapping_type", DOUBLE),
                                        kv(MAPPING, object("type", DOUBLE))))))),
                kv(PROPERTIES,
                        //These are AbstractEntity standard attributes mappings
                        object( // first root attributes from AbstractEntity hierrachy
                                kv("id", object("type", "long")),
                                kv("creationDate", optionalDatetimeMapping()),
                                kv("ipId", stringMapping()),
                                kv("type", stringMapping()),
                                kv("wgs84", object("type", "geo_shape")),
                                kv("nwPoint", object("type", "geo_point")),
                                kv("sePoint", object("type", "geo_point")),
                                kv("tags", stringMapping()),
                                kv("groups", stringMapping()),
                                kv("lastUpdate", optionalDatetimeMapping()),
                                // then feature attributes
                                kv("feature", feautrePropertiesMapping()),
                                // then model attributes
                                kv("model", modelPropertiesMapping()),
                                // then DataObject specific attributes
                                kv("dataSourceId", object("type", "long")),
                                kv("internal", object("type", BOOLEAN)),
                                kv("datasetModelNames", stringMapping()),
                                // then metadata attributes
                                // metadata cannot be mapped that easily because it contains maps
                                // then Dataset specific attributes
                                kv("dataModel", stringMapping()),
                                kv("openSearchSubsettingClause", object("type", "text")),
                                // subsettingClause cannot be mapped that easily
                                kv("plgConfDataSource", pluginConfPropertiesMapping())
                        )
                )
                // mappings cannot be strict because of metadata and subsettingClause.
        );
        //@formatter:on
    }

    private JsonElement optionalDatetimeMapping() {
        return object(kv("type", "date"), kv("format", "date_optional_time"));
    }

    private JsonElement pluginConfPropertiesMapping() {
        return object(kv(PROPERTIES,
                         object(kv("active", object("type", BOOLEAN)),
                                kv("businessId", stringMapping()),
                                kv("id", object("type", "long")),
                                kv("label", stringMapping()),
                                kv("pluginId", stringMapping()),
                                kv("priorityOrder", object("type", "long")),
                                kv("parameters", object("type", "nested")),
                                kv(VERSION, stringMapping()))));
    }

    private JsonElement modelPropertiesMapping() {
        return object(kv(PROPERTIES,
                         object(kv("description", object("type", "text")),
                                kv("id", object("type", "long")),
                                kv("name", stringMapping()),
                                kv("type", stringMapping()),
                                kv(VERSION, stringMapping()))));
    }

    private JsonElement feautrePropertiesMapping() {
        return object(kv(PROPERTIES,
                         object(kv("entityType", stringMapping()),
                                kv("files", object("type", "object")),
                                kv("id", stringMapping()),
                                kv("label", stringMapping()),
                                kv("last", object("type", BOOLEAN)),
                                kv("model", stringMapping()),
                                kv("normalizedGeometry", object("type", "geo_shape")),
                                kv(PROPERTIES, object("type", "object")),
                                kv("providerId", stringMapping()),
                                kv("type", stringMapping()),
                                kv(VERSION, stringMapping()),
                                kv("crs", stringMapping()),
                                kv("tags", stringMapping()),
                                kv("virtualId", stringMapping()),
                                // DataObjectFeature specific attribute
                                kv("session", stringMapping()),
                                kv("sessionOwner", stringMapping()),
                                // DatasetFeature specific attribute
                                kv("licence", object("type", "text")))));
    }

    @Override
    public boolean putMappings(String index, Set<AttributeDescription> mappings) {
        JsonMerger merger = new JsonMerger();
        JsonObject mappingsGson = mappings.stream()
                                          .map(toMapping::toJsonMapping)
                                          .reduce(new JsonObject(), merger::merge, this::neverCalled);
        try {
            JsonConverter converter = new JsonConverter();
            XContentBuilder mappingBuilder = converter.toXContentBuilder(mappingsGson);
            PutMappingRequest request = new PutMappingRequest(index.toLowerCase());
            request.source(mappingBuilder);
            AcknowledgedResponse putMappingResponse = client.indices().putMapping(request, RequestOptions.DEFAULT);
            return putMappingResponse.isAcknowledged();
        } catch (IOException e) {
            LOGGER.error(e.getMessage(), e);
            throw new RsRuntimeException(e);
        }
    }

    private <U> U neverCalled(U one, U two) {
        throw new RsRuntimeException("Should never be called");
    }

    @Override
    public boolean createAlias(String index, String alias) {
        try {
            IndicesAliasesRequest request = new IndicesAliasesRequest();
            AliasActions createAliasAction = new AliasActions(Type.ADD).index(index.toLowerCase())
                                                                       .alias(alias.toLowerCase());
            request.addAliasAction(createAliasAction);
            AcknowledgedResponse response = client.indices().updateAliases(request, RequestOptions.DEFAULT);
            return response.isAcknowledged();
        } catch (IOException e) {
            LOGGER.error(e.getMessage(), e);
            throw new RsRuntimeException(e);
        }
    }

    @Override
    public boolean setSettingsForBulk(String index) {
        try {
            UpdateSettingsRequest request = Requests.updateSettingsRequest(index.toLowerCase());
            Settings.Builder builder = Settings.builder()
                                               .put(INDEX_REFRESH_INTERVAL, -1)
                                               .put(INDEX_NUMBER_OF_REPLICAS, 0);
            request.settings(builder);
            AcknowledgedResponse response = client.indices().putSettings(request, RequestOptions.DEFAULT);
            return response.isAcknowledged();
        } catch (IOException e) {
            LOGGER.error(e.getMessage(), e);
            throw new RsRuntimeException(e);
        }

    }

    @Override
    public boolean unsetSettingsForBulk(String index) {
        try {
            UpdateSettingsRequest request = Requests.updateSettingsRequest(index.toLowerCase());
            Settings.Builder builder = Settings.builder()
                                               .put(INDEX_REFRESH_INTERVAL, "1s")
                                               .put(INDEX_NUMBER_OF_REPLICAS, 1);
            request.settings(builder);
            AcknowledgedResponse response = client.indices().putSettings(request, RequestOptions.DEFAULT);
            return response.isAcknowledged();
        } catch (IOException e) {
            LOGGER.error(e.getMessage(), e);
            throw new RsRuntimeException(e);
        }

    }

    @Override
    public boolean indexExists(String name) {
        try {
            GetIndexRequest request = new GetIndexRequest(name.toLowerCase());
            return client.indices().exists(request, RequestOptions.DEFAULT);
        } catch (IOException e) {
            LOGGER.error(e.getMessage(), e);
            throw new RsRuntimeException(e);
        }
    }

    @Override
    public boolean deleteIndex(String inIndex) throws IndexNotFoundException {
        String index = inIndex.toLowerCase();
        try {
            return deleteIndex0(index);
        } catch (RsRuntimeException e) {
            if (Throwables.getRootCause(e) instanceof ElasticsearchException) {
                ElasticsearchException ee = (ElasticsearchException) Throwables.getRootCause(e);
                // It's probably because index is an alias
                if (ee.status() == RestStatus.BAD_REQUEST) {
                    try {
                        GetAliasesRequest request = new GetAliasesRequest(index);
                        GetAliasesResponse response = client.indices().getAlias(request, RequestOptions.DEFAULT);
                        // There should be only one index (an alias cannot be multi-index...i assume it)
                        if (response.getAliases().size() == 1) {
                            Map.Entry<String, Set<AliasMetadata>> entry = response.getAliases()
                                                                                  .entrySet()
                                                                                  .iterator()
                                                                                  .next();
                            // Given alias must be the only one for this index
                            if (entry.getValue().size() == 1) {
                                // Now it is ok to delete index (concerned alias is necessarily asked one)
                                return deleteIndex0(entry.getKey());
                            }
                        }
                        return false;
                    } catch (Exception e1) { // NOSONAR (let's focus on first exception)
                        // Ok, let it go, go back to first exception
                        throw e;
                    }
                } else {
                    throw e;
                }
            } else {
                throw e;
            }
        }
    }

    private boolean deleteIndex0(String index) throws IndexNotFoundException {
        try {
            DeleteIndexRequest request = Requests.deleteIndexRequest(index);
            AcknowledgedResponse response = client.indices().delete(request, RequestOptions.DEFAULT);
            return response.isAcknowledged();
        } catch (ElasticsearchException e) {
            if (e.status() == RestStatus.NOT_FOUND) {
                throw new IndexNotFoundException(index.toLowerCase());
            }
            LOGGER.error(e.getMessage(), e);
            throw new RsRuntimeException(e);
        } catch (IOException e) {
            LOGGER.error(e.getMessage(), e);
            throw new RsRuntimeException(e);
        }
    }

    @Override
    public long deleteAll(String inIndex) {
        return this.deleteByQuery(inIndex.toLowerCase(), ICriterion.all());
    }

    @Override
    public long deleteByQuery(String index, ICriterion criterion) {
        String query = criterion.accept(CRITERION_VISITOR).toString();
        LOGGER.info("Delete by query in Elasticsearch index [{}] with query: {}", index, query);
        try (NStringEntity entity = new NStringEntity("{ \"query\":" + query + "}", ContentType.APPLICATION_JSON)) {
            Request request = new Request("POST", "/" + index.toLowerCase() + "/_delete_by_query");
            request.setEntity(entity);

            Response response = client.getLowLevelClient().performRequest(request);
            try (InputStream is = response.getEntity().getContent()) {
                Map<String, Object> map = XContentHelper.convertToMap(XContentType.JSON.xContent(), is, true);
                return ((Number) map.get("deleted")).longValue();
            }
        } catch (IOException e) {
            LOGGER.error(e.getMessage(), e);
            throw new RsRuntimeException(e);
        }
    }

    @Override
    public long deleteByDatasource(String inIndex, Long datasourceId) {
        return this.deleteByQuery(inIndex.toLowerCase(), ICriterion.eq(StaticProperties.DATASOURCE_ID, datasourceId));
    }

    @Override
    public void refresh(String index) {
        // To make just saved documents searchable, the associated index must be refreshed
        try {
            RefreshRequest request = Requests.refreshRequest(index.toLowerCase());
            client.indices().refresh(request, RequestOptions.DEFAULT);
        } catch (IOException e) {
            LOGGER.error(e.getMessage(), e);
            throw new RsRuntimeException(e);
        }
    }

    @Override
    public <T extends IIndexable> T get(Optional<String> index, String type, String id, Class<T> clazz) {

        GetRequest request = new GetRequest(getIndex(index).toLowerCase(), id);

        try {
            GetResponse response = client.get(request, RequestOptions.DEFAULT);
            if (!response.isExists()) {
                return null;
            }
            String sourceAsString = response.getSourceAsString();
            return deserializeHitsStrategy.deserializeJson(sourceAsString, clazz);
        } catch (final JsonSyntaxException | IOException e) {
            LOGGER.error(e.getMessage(), e);
            throw new RsRuntimeException(e);
        }
    }

    @Override
    public <T extends IIndexable> T getByVirtualId(String docType,
                                                   String virtualId,
                                                   Class<? extends IIndexable> clazz) {
        // use search0
        SimpleSearchKey<T> searchKey = new SimpleSearchKey(docType, clazz);
        searchKey.setSearchIndex(getIndex(Optional.empty()));
        ICriterion virtualIdCrit = ICriterion.eq("feature.virtualId", virtualId, StringMatchType.KEYWORD);
        return search0(searchKey, PageRequest.of(0, 1), virtualIdCrit, null).getContent().get(0);
    }

    @Override
    public boolean delete(String index, String type, String id) {
        DeleteRequest request = new DeleteRequest(index.toLowerCase(), id);
        try {
            DeleteResponse response = client.delete(request, RequestOptions.DEFAULT);
            return (response.getResult() == Result.DELETED) || (response.getResult() == Result.NOT_FOUND);
        } catch (IOException e) {
            LOGGER.error(e.getMessage(), e);
            throw new RsRuntimeException(e);
        }
    }

    private void checkDocument(IIndexable doc) {
        if (Strings.isBlank(doc.getDocId()) || Strings.isBlank(doc.getType())) {
            throw new IllegalArgumentException("docId and type are mandatory on an IIndexable object");
        }
    }

    @Override
    public boolean save(String index, IIndexable doc) {
        checkDocument(doc);
        try {
            IndexRequest request = new IndexRequest(index.toLowerCase());
            request.id(doc.getDocId());
            IndexResponse response = client.index(request.source(gson.toJson(doc), XContentType.JSON),
                                                  RequestOptions.DEFAULT);
            return response.getResult() == Result.CREATED; // Else UPDATED
        } catch (IOException e) {
            LOGGER.error(e.getMessage(), e);
            throw new RsRuntimeException(e);
        }
    }

    @Override
    public <T extends IIndexable> BulkSaveResult saveBulk(String inIndex,
                                                          BulkSaveResult bulkSaveResult,
                                                          StringBuilder errorBuffer,
                                                          T... documents) {
        try {
            // Use existing one or create
            BulkSaveResult result = bulkSaveResult == null ? new BulkSaveResult() : bulkSaveResult;
            if (documents.length == 0) {
                return result;
            }
            String index = inIndex.toLowerCase();
            // Check mandatory properties (as docId, type, ...)
            for (T doc : documents) {
                checkDocument(doc);
            }
            // Create Save bulk request
            BulkRequest bulkRequest = new BulkRequest();
            Map<String, T> map = new HashMap<>();
            for (T doc : documents) {
                IndexRequest indexRequest = new IndexRequest(index);
                indexRequest.id(doc.getDocId());
                IndexRequest source = indexRequest.source(gson.toJson(doc), XContentType.JSON);
                bulkRequest.add(source);
                map.put(doc.getDocId(), doc);
            }
            // Bulk save
            BulkResponse response = client.bulk(bulkRequest, RequestOptions.DEFAULT);
            // Parse response to creata a more exploitable object
            for (BulkItemResponse itemResponse : response.getItems()) {
                T document = map.get(itemResponse.getId());
                if (itemResponse.isFailed()) {
                    // Add item it and its associated exception
                    if (document instanceof DataObject) {
                        DataObjectFeature docFeature = (((DataObject) document).getFeature());
                        result.addInErrorDoc(itemResponse.getId(),
                                             itemResponse.getFailure().getCause(),
                                             Optional.ofNullable(docFeature.getSession()),
                                             Optional.ofNullable(docFeature.getSessionOwner()));
                        if (itemResponse.getFailure().getMessage().contains(IMapping.GEO_SHAPE_ATTRIBUTE)) {
                            // Save the failling geometry in the log
                            IGeometry wgs84 = ((DataObject) document).getWgs84();
                            if (wgs84 instanceof Polygon) {
                                Polygon polygonWGS84 = (Polygon) wgs84;
                                if (errorBuffer.length() > 0) {
                                    errorBuffer.append('\n').append('\n');
                                }
                                String msg =
                                    "The here under geometry have not been accepted by ElasticSearch:\n{\"type\": \"FeatureCollection\",\"features\": [{\"type\": \"Feature\","
                                    + "\"properties\":{},\"geometry\": {\"type\": \"Polygon\",\"coordinates\": [["
                                    + polygonWGS84.getCoordinates().getExteriorRing().toString()
                                    + "]]}}]}";
                                errorBuffer.append(msg);
                            } else if (wgs84 instanceof MultiPolygon) {
                                MultiPolygon multiPolygonWGS84 = (MultiPolygon) wgs84;
                                if (errorBuffer.length() > 0) {
                                    errorBuffer.append('\n').append('\n');
                                }
                                String msg =
                                    "The here under geometry have not been accepted by ElasticSearch:\n{\"type\": \"FeatureCollection\",\"features\": [{\"type\": \"Feature\","
                                    + "\"properties\":{},\"geometry\": {\"type\": \"MultiPolygon\",\"coordinates\": [["
                                    + multiPolygonWGS84.getCoordinates()
                                                       .stream()
                                                       .map(p -> p.getExteriorRing().toString())
                                                       .collect(Collectors.joining("], [", "[", "]"))
                                    + "]]}}]}";
                                errorBuffer.append(msg);
                            }
                        }
                    } else {
                        result.addInErrorDoc(itemResponse.getId(),
                                             itemResponse.getFailure().getCause(),
                                             Optional.empty(),
                                             Optional.empty());
                    }
                    String msg = String.format("Document of type %s and id %s with label %s cannot be saved",
                                               documents[0].getClass(),
                                               itemResponse.getId(),
                                               map.get(itemResponse.getId()));

                    // Log error
                    LOGGER.warn(msg, itemResponse.getFailure().getCause());
                    // Add error msg to buffer
                    if (errorBuffer != null) {
                        if (errorBuffer.length() > 0) {
                            errorBuffer.append('\n');
                        }
                        errorBuffer.append(msg).append('\n').append("Cause: ");
                        // ElasticSearch creates Exception on exception (root one is more appropriate)
                        Throwable exception = Throwables.getRootCause(itemResponse.getFailure().getCause());
                        errorBuffer.append(exception.getMessage());
                    }
                } else {
                    if (document instanceof DataObject) {
                        DataObjectFeature docFeature = (((DataObject) document).getFeature());
                        result.addSavedDoc(itemResponse.getId(),
                                           itemResponse.getResponse().getResult(),
                                           Optional.ofNullable(docFeature.getSession()),
                                           Optional.ofNullable(docFeature.getSessionOwner()));
                    } else {
                        result.addSavedDoc(itemResponse.getId(),
                                           itemResponse.getResponse().getResult(),
                                           Optional.empty(),
                                           Optional.empty());
                    }
                }
            }
            // To make just saved documents searchable, the associated index must be refreshed
            this.refresh(index);
            return result;
        } catch (IOException e) {
            LOGGER.error(e.getMessage(), e);
            throw new RsRuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends IIndexable> void searchAll(SearchKey<T, T> searchKey, Consumer<T> action, ICriterion inCrit) {
        try {

            SearchSourceBuilder builder = createSourceBuilder4Agg(addTypes(inCrit, searchKey.getSearchTypes()));
            SearchRequest request = new SearchRequest(searchKey.getSearchIndex()).source(builder);
            ICriterion crit = inCrit == null ? ICriterion.all() : inCrit;
            crit = addTypes(crit, searchKey.getSearchTypes());
            builder.query(crit.accept(CRITERION_VISITOR)).size(DEFAULT_SCROLLING_HITS_SIZE);
            request.source(builder);
            request.scroll(TimeValue.timeValueMinutes(KEEP_ALIVE_SCROLLING_TIME_MN));
            SearchResponse scrollResp = getSearchResponse(request);
            ClearScrollRequest clearRequest = new ClearScrollRequest();

            // Scroll until no hits are returned
            do {
                String scrollId = scrollResp.getScrollId();
                for (final SearchHit hit : scrollResp.getHits().getHits()) {
                    action.accept(deserializeHitsStrategy.deserializeJson(hit.getSourceAsString(),
                                                                          (Class<T>) IIndexable.class));
                }
                // Add new scroll context in list of context to delete after process done.
                // There should be only one context as it is the same after each search with scroll api.
                if (clearRequest.getScrollIds() == null || !clearRequest.getScrollIds().contains(scrollId)) {
                    clearRequest.addScrollId(scrollId);
                }
                SearchScrollRequest scrollRequest = new SearchScrollRequest(scrollId);
                scrollRequest.scroll(TimeValue.timeValueMinutes(KEEP_ALIVE_SCROLLING_TIME_MN));
                scrollResp = client.scroll(scrollRequest, RequestOptions.DEFAULT);
                if (scrollResp.getHits().getHits().length == 0) {
                    clearRequest.addScrollId(scrollResp.getScrollId());
                }
            } while (scrollResp.getHits().getHits().length != 0); // Zero hits mark the end of the scroll and the while
            // Delete scroll context to avoid too many scroll context stored in Elasticsearch (limit 500)
            if (clearRequest.getScrollIds() != null && clearRequest.getScrollIds().size() > 0) {
                client.clearScrollAsync(clearRequest, RequestOptions.DEFAULT, scrollClearListener);
            }
            // loop.
        } catch (IOException e) {
            LOGGER.error(e.getMessage(), e);
            throw new RsRuntimeException(e);
        }
    }

    private SearchResponse getSearchResponse(SearchRequest request) throws IOException {
        try {
            return client.search(request, searchOptions);
        } catch (ElasticsearchException ee) {
            LOGGER.error(ee.getMessage(), ee);
            if (ee.getMessage().contains(INDEX_NOT_FOUND_EXCEPTION)) {
                throw new ESIndexNotFoundRuntimeException();
            }
            Optional<String> queryShardCause = Arrays.stream(ee.getSuppressed())
                                                     .map(t -> t.getMessage())
                                                     .filter(m -> m.contains(QUERY_SHARD_EXCEPTION))
                                                     .findFirst();
            if (queryShardCause.isPresent()) {
                Pattern pattern = Pattern.compile(QUERY_SHARD_EXCEPTION_CAUSE_PATTERN);
                Matcher matcher = pattern.matcher(queryShardCause.get());
                matcher.find();
                //If there is no result, it means that the thrown exception isn't the expected one (searching on a field which is not indexed)
                if (matcher.group(1) != null) {
                    throw new FieldNotIndexedRuntimeException(matcher.group(1));
                }
            }
            throw ee;
        }
    }

    /**
     * Returns a tuple containing at the same time attribute values from search documents and facets
     *
     * @param <R>             Type of document to apply search
     * @param searchKey       search key for documents of type R
     * @param criterion       search criterion on documents of type R
     * @param attributeSource document attribute to return
     * @param facetsMap       facets wanted
     * @return a tuple containing at the same time attribute values from search documents and facets
     */
    private <R> Tuple<SortedSet<Object>, Set<IFacet<?>>> searchJoined(SearchKey<?, R> searchKey,
                                                                      ICriterion criterion,
                                                                      String attributeSource,
                                                                      Map<String, FacetType> facetsMap) {
        try {
            // Add ".keyword" if attribute mapping type is of type text
            String attribute = isTextMapping(searchKey.getSearchIndex(), attributeSource) ?
                attributeSource + KEYWORD_SUFFIX :
                attributeSource;
            SortedSet<Object> uniqueValues = new TreeSet<>(Comparator.comparing(Objects::hashCode));
            Set<IFacet<?>> facets = unique(searchKey,
                                           addTypes(criterion, searchKey.getSearchTypes()),
                                           attribute,
                                           Integer.MAX_VALUE,
                                           uniqueValues,
                                           facetsMap);
            return new Tuple<>(uniqueValues, facets);
        } catch (IOException e) {
            LOGGER.error(e.getMessage(), e);
            throw new RsRuntimeException(e);
        }
    }

    @Override
    public <T extends IIndexable> FacetPage<T> search(SearchKey<T, T> searchKey,
                                                      Pageable pageRequest,
                                                      ICriterion crit,
                                                      Map<String, FacetType> facetsMap) {
        ICriterion criterion = addTypes(crit, searchKey.getSearchTypes());
        // Search context is different from Earth search context, check if it's a geo-search
        if (searchKey.getCrs() != Crs.WGS_84) {
            // Does criterion tree contain a BoundaryBox or Polygon criterion, if so => make a projection on WGS84
            if (GeoHelper.containsPolygonOrBboxCriterion(criterion)) {
                GeoCriterionWithPolygonOrBboxVisitor visitor = new GeoCriterionWithPolygonOrBboxVisitor(searchKey.getCrs());
                criterion = criterion.accept(visitor);
                PolygonCriterion polygonCrit = GeoHelper.findPolygonCriterion(criterion);
                if (polygonCrit != null) {
                    LOGGER.debug("Searching intersection with polygon {} projected on WGS84...",
                                 Arrays.stream(polygonCrit.getCoordinates()[0])
                                       .map(Arrays::toString)
                                       .collect(Collectors.joining(",")));
                }
            } else if (GeoHelper.containsCircleCriterion(criterion)) {
                // For Astro, circleCriterion radius is in fact the half-angle of the cone in degrees
                // We must change it into projected equivalent radius
                if (searchKey.getCrs() == Crs.ASTRO) {
                    CircleCriterion initialCircleCriterion = GeoHelper.findCircleCriterion(crit);
                    // Radius MUST NOT HAVE A UNIT
                    initialCircleCriterion.setRadius(FastMath.toRadians(Double.parseDouble(initialCircleCriterion.getRadius()))

                                                     * AUTHALIC_SPHERE_RADIUS);
                }
                return searchWithCircleCriterionInProjectedCrs(searchKey, pageRequest, facetsMap, criterion);
            }
        }
        return search0(searchKey, pageRequest, criterion, facetsMap);
    }

    /**
     * Particular case when search is asked with a circle criterion into a CRS which is not WGS84.
     */
    private <T extends IIndexable> FacetPage<T> searchWithCircleCriterionInProjectedCrs(SearchKey<T, T> searchKey,
                                                                                        Pageable pageRequest,
                                                                                        Map<String, FacetType> facetsMap,
                                                                                        ICriterion criterion) {
        // If criterion contains a Circle criterion, it is more complicated :
        // Mars and Earth flattenings are different (and so astro, it's a perfect sphere)
        // So we obtain a max radius cricle and a min radius circle for earth projected coordinates
        // Every shapes into min radius circle are guaranted to be ok
        // Every shapes outo max radius circle are guaranted to not be ok
        // Other ones must be unitary tested one by one
        GeoCriterionWithCircleVisitor visitor = new GeoCriterionWithCircleVisitor(searchKey.getCrs());
        Pair<ICriterion, ICriterion> critOnWgs84Pair = criterion.accept(visitor);
        // In case of symetry (=> same radius for inner and outer circles) same criterion is returned into pair
        if (critOnWgs84Pair.getFirst().equals(critOnWgs84Pair.getSecond())) {
            long start = System.currentTimeMillis();
            FacetPage<T> page = search0(searchKey, pageRequest, critOnWgs84Pair.getFirst(), facetsMap);
            LOGGER.debug("Simple symetric circle search with radius: {} (duration: {} ms)",
                         GeoHelper.findCircleCriterion(critOnWgs84Pair.getFirst()).getRadius(),
                         System.currentTimeMillis() - start);
            return page;
        }
        // Criterion permiting to retrieve shapes betwwen both circles
        ICriterion betweenInnerAndOuterCirclesCriterionOnWgs84 = critOnWgs84Pair.getSecond();

        // FIRST: retrieve all data into inner circle
        ICriterion innerCircleOnWgs84Criterion = critOnWgs84Pair.getFirst();
        long start = System.currentTimeMillis();
        FacetPage<T> intoInnerCirclePage = search0(searchKey, pageRequest, innerCircleOnWgs84Criterion, facetsMap);
        // If more than MAX_PAGE_SIZE => TooManyResultException (too complicated case)
        if (!intoInnerCirclePage.isLast()) {
            throw new RsRuntimeException(new TooManyResultsException(
                "Please refine criteria to avoid exceeding page size limit"));
        }
        CircleCriterion innerCircleCrit = GeoHelper.findCircleCriterion(innerCircleOnWgs84Criterion);
        LOGGER.debug(
            "Found {} points into inner circle with radius {} and center {} projected on WGS84 (search duration: {} ms)",
            intoInnerCirclePage.getNumberOfElements(),
            innerCircleCrit.getRadius(),
            Arrays.toString(innerCircleCrit.getCoordinates()),
            System.currentTimeMillis() - start);
        // SECOND: retrieve all data between inner and outer circles
        start = System.currentTimeMillis();
        FacetPage<T> betweenInnerAndOuterCirclesPage = search0(searchKey,
                                                               pageRequest,
                                                               betweenInnerAndOuterCirclesCriterionOnWgs84,
                                                               facetsMap);
        // If more than MAX_PAGE_SIZE => TooManyResultException (too complicated case)
        if (!intoInnerCirclePage.isLast()) {
            throw new RsRuntimeException(new TooManyResultsException(
                "Please refine criteria to avoid exceeding page size limit"));
        }
        LOGGER.debug("Found {} points between inner and outer circles (search duration: {} ms)",
                     betweenInnerAndOuterCirclesPage.getNumberOfElements(),
                     System.currentTimeMillis() - start);

        // THIRD: keep only entities with a shape nearer than specified radius from specified center
        // Retrieve radius of specified circle on given Crs
        CircleCriterion circleCriterionOnCrs = GeoHelper.findCircleCriterion(criterion);
        double maxRadiusOnCrs = EsHelper.toMeters(circleCriterionOnCrs.getRadius());
        double[] center = circleCriterionOnCrs.getCoordinates();

        // Test all data one by one
        List<T> inOuterCircleEntities = new ArrayList<>();
        for (T entity : betweenInnerAndOuterCirclesPage.getContent()) {
            if (entity instanceof ILocalizable) {
                IGeometry shape = ((ILocalizable) entity).getNormalizedGeometry();
                if (GeoHelper.isNearer(shape, center, maxRadiusOnCrs, searchKey.getCrs())) {
                    inOuterCircleEntities.add(entity);
                } else {
                    LOGGER.error("Remove " + entity);
                }
            }
        }
        // Merge data
        if (!inOuterCircleEntities.isEmpty()) {
            LOGGER.debug("Keep {} points between inner and outer circles", inOuterCircleEntities.size());
            List<T> resultList = new ImmutableList.Builder<T>().addAll(intoInnerCirclePage.getContent())
                                                               .addAll(inOuterCircleEntities)
                                                               .build();
            return new FacetPage<>(resultList, null, intoInnerCirclePage.getPageable(), resultList.size());
        } else {
            LOGGER.debug("Keep no points between inner and outer circles");
        }
        return intoInnerCirclePage;
    }

    /**
     * Inner search on WGS84 method.
     * <b>NOTE : criterion already contains restricition on types !!</b>
     */
    @SuppressWarnings("unchecked")
    private <T extends IIndexable> FacetPage<T> search0(SearchKey<T, T> searchKey,
                                                        Pageable pageRequest,
                                                        ICriterion criterion,
                                                        Map<String, FacetType> facetsMap) {
        ICriterion finalCriterion = criterion.accept(new VersioningSearchVisitor(this));
        String index = searchKey.getSearchIndex();
        try {
            final List<T> results = new ArrayList<>();

            Sort sort = pageRequest.getSort();

            //We add a sort by "ipId" by default because it is needed if we use a searchAfter pagination method
            //We add it in all case to ensure consistency of the sorting policy throughout the search
            sort = (sort == null) || sort.isUnsorted() ? Sort.by("ipId") : pageRequest.getSort();
            // To assure unicity, always add "ipId" as a sort parameter
            if (sort.getOrderFor("ipId") == null) {
                sort = sort.and(Sort.by("ipId"));
            }

            Object[] lastSearchAfterSortValues = null;
            // If page starts over index 10 000, advance with searchAfter just before last request
            if (pageRequest.getOffset() >= MAX_RESULT_WINDOW) {
                lastSearchAfterSortValues = advanceWithSearchAfter(finalCriterion, searchKey, pageRequest, index, sort);
            }

            final Object[] finalLastSearchAfterSortValues = lastSearchAfterSortValues;
            final Sort finalSort = sort;
            Consumer<SearchSourceBuilder> lastSearchAfterCustomizer = (builder) -> {
                try {
                    // If searchAfter has been executed (in that case manageSortRequest() has already been called)
                    if (finalLastSearchAfterSortValues != null) {
                        builder.searchAfter(finalLastSearchAfterSortValues).from(0);
                        manageSortRequest(index, builder, finalSort);
                    } else if ((finalSort != null) && finalSort.isSorted()) {
                        manageSortRequest(index, builder, finalSort);
                    }
                } catch (IOException e) {
                    //not nice but if IOException would be thrown (not using a consumer), it would be simply handle like that
                    throw new RsRuntimeException(e);
                }
            };

            Tuple<SearchResponse, Set<IFacet<?>>> responseNFacets = searchWithFacets(searchKey,
                                                                                     finalCriterion,
                                                                                     pageRequest,
                                                                                     lastSearchAfterCustomizer,
                                                                                     sort,
                                                                                     facetsMap);
            SearchResponse response = responseNFacets.v1();
            long start = System.currentTimeMillis();
            SearchHits hits = response.getHits();
            for (SearchHit hit : hits) {
                try {
                    JsonParser parser = new JsonParser();
                    JsonObject jo = parser.parse(hit.getSourceAsString()).getAsJsonObject();
                    String hitType = jo.get("type").getAsString();
                    results.add(deserializeHitsStrategy.deserializeJson(hit.getSourceAsString(),
                                                                        searchKey.getSearchTypeMap().get(hitType)));
                } catch (JsonParseException e) {
                    LOGGER.error("Unable to jsonify entity with id {}, source: \"{}\"",
                                 hit.getId(),
                                 hit.getSourceAsString());
                    throw new RsRuntimeException(e);
                }
            }
            LOGGER.debug("After Elasticsearch request execution, gsonification : {} ms",
                         System.currentTimeMillis() - start);
            return new FacetPage<T>(results,
                                    responseNFacets.v2(),
                                    pageRequest,
                                    response.getHits().getTotalHits().value);
        } catch (final JsonSyntaxException | IOException e) {
            throw new RsRuntimeException(e);
        }
    }

    private Tuple<SearchResponse, Set<IFacet<?>>> searchWithFacets(SearchKey<?, ?> searchKey,
                                                                   ICriterion criterion,
                                                                   Pageable pageRequest,
                                                                   Consumer<SearchSourceBuilder> searchSourceBuilderCustomizer,
                                                                   Sort sort,
                                                                   Map<String, FacetType> facetsMap)
        throws IOException {
        String index = searchKey.getSearchIndex();
        SearchRequest request = new SearchRequest(index);
        SearchSourceBuilder builder = createSourceBuilder4Agg(criterion,
                                                              (int) pageRequest.getOffset(),
                                                              pageRequest.getPageSize());

        if (searchSourceBuilderCustomizer != null) {
            searchSourceBuilderCustomizer.accept(builder);
        }

        // Managing aggregations if some facets are asked
        boolean twoPassRequestNeeded = manageFirstPassRequestAggregations(facetsMap, builder);
        request.source(builder);
        // Launch the request
        long start = System.currentTimeMillis();
        LOGGER.trace("ElasticsearchRequest: {}", request);
        SearchResponse response = getSearchResponse(request);
        LOGGER.debug("Elasticsearch request execution only : {} ms", System.currentTimeMillis() - start);

        start = System.currentTimeMillis();
        Set<IFacet<?>> facetResults = new HashSet<>();
        if (response.getHits().getTotalHits().value != 0) {
            // At least one numeric facet is present, we need to replace all numeric facets by associated range
            // facets
            if (twoPassRequestNeeded) {
                // Rebuild request
                request = new SearchRequest(index);
                builder = createSourceBuilder4Agg(criterion, (int) pageRequest.getOffset(), pageRequest.getPageSize());

                if (searchSourceBuilderCustomizer != null) {
                    searchSourceBuilderCustomizer.accept(builder);
                }
                Map<String, Aggregation> aggsMap = response.getAggregations().asMap();
                manageSecondPassRequestAggregations(facetsMap, builder, aggsMap);
                // Relaunch the request with replaced facets
                request.source(builder);
                LOGGER.trace("ElasticsearchRequest (2nd pass): {}", request);
                response = client.search(request, searchOptions);
            }

            // If offset >= MAX_RESULT_WINDOW or page size = MAX_RESULT_WINDOW, this means a next page should exist
            // (not necessarly)
            if ((pageRequest.getOffset() >= MAX_RESULT_WINDOW) || (pageRequest.getPageSize() == MAX_RESULT_WINDOW)) {
                saveReminder(searchKey, pageRequest, criterion, sort, response);
            }

            extractFacetsFromResponse(facetsMap, response, facetResults);
        }
        LOGGER.debug("After Elasticsearch request execution, aggs and searchAfter management : {} ms",
                     System.currentTimeMillis() - start);
        return new Tuple<>(response, facetResults);
    }

    /**
     * extract facets according to response and facetsMap and put them into facetResults
     */
    private void extractFacetsFromResponse(Map<String, FacetType> facetsMap,
                                           SearchResponse response,
                                           Set<IFacet<?>> facetResults) {
        if ((facetsMap != null) && (response.getAggregations() != null)) {
            // Get the new aggregations result map
            Map<String, Aggregation> aggsMap = response.getAggregations().asMap();
            // Fill the facet set
            for (Map.Entry<String, FacetType> entry : facetsMap.entrySet()) {
                FacetType facetType = entry.getValue();
                String attributeName = entry.getKey();
                fillFacets(aggsMap, facetResults, facetType, attributeName, response.getHits().getTotalHits().value);
            }
        }
    }

    private void saveReminder(SearchKey<?, ?> searchKey,
                              Pageable pageRequest,
                              ICriterion crit,
                              Sort sort,
                              SearchResponse response) {
        if (response.getHits().getHits().length != 0) {
            // Store last sort value in order to use searchAfter next time
            Object[] sortValues = response.getHits().getAt(response.getHits().getHits().length - 1).getSortValues();
            OffsetDateTime expirationDate = OffsetDateTime.now().plus(KEEP_ALIVE_SCROLLING_TIME_MN, ChronoUnit.MINUTES);
            // Create a AbstractReminder and save it into ES for next page
            SearchAfterReminder reminder = new SearchAfterReminder(crit, searchKey, sort, pageRequest.next());
            reminder.setExpirationDate(expirationDate);
            reminder.setSearchAfterSortValues(sortValues);

            save(REMINDER_IDX, reminder);
            // Create a task to be executed after KEEP_ALIVE_SCROLLING_TIME_MN that delete all reminders whom
            // expiration date has been reached
            // No need to add reminder type criterion because reminder type is useless since ES6
            reminderCleanExecutor.schedule(() -> deleteByQuery(REMINDER_IDX,
                                                               ICriterion.le("expirationDate", OffsetDateTime.now())),
                                           KEEP_ALIVE_SCROLLING_TIME_MN,
                                           TimeUnit.MINUTES);
        }
    }

    /**
     * <b>NOTE: critBuilder already contains restriction on types</b>
     */
    private <T extends IIndexable> Object[] advanceWithSearchAfter(ICriterion crit,
                                                                   SearchKey<T, T> searchKey,
                                                                   Pageable pageRequest,
                                                                   String index,
                                                                   Sort sort) {
        try {
            Object[] sortValues = null;
            int searchPageNumber = 0;
            Pageable searchReminderPageRequest;
            if (indexExists(REMINDER_IDX)) {
                // First check existence of AbstractReminder for exact given pageRequest from ES
                SearchAfterReminder reminder = new SearchAfterReminder(crit, searchKey, sort, pageRequest);
                reminder = get(Optional.of(REMINDER_IDX), reminder);
                if (reminder != null) {
                    LOGGER.debug("Found search after for offset {}", pageRequest.getOffset());
                    return reminder.getSearchAfterSortValues();
                }
                // Then check if a closer one exists (advance is done by MAX_RESULT_WINDOW steps so we must take this
                // into account)
                searchPageNumber = (int) ((pageRequest.getOffset() - (pageRequest.getOffset() % MAX_RESULT_WINDOW))
                                          / MAX_RESULT_WINDOW);
                while (searchPageNumber > 0) {
                    searchReminderPageRequest = PageRequest.of(searchPageNumber, MAX_RESULT_WINDOW);
                    reminder = new SearchAfterReminder(crit, searchKey, sort, searchReminderPageRequest);
                    reminder = get(Optional.of(REMINDER_IDX), reminder);
                    // A reminder has been found ! Let's start from it
                    if (reminder != null) {
                        LOGGER.debug("Found search after for offset {}", searchReminderPageRequest.getOffset());
                        sortValues = reminder.getSearchAfterSortValues();
                        break;
                    }
                    searchPageNumber--;
                }
            }

            // No reminder found (first request or last one is too old) => advance to next to last page
            SearchRequest request = new SearchRequest(index);
            // By default, launch request from 0 to 10_000 (without aggregations)...
            int offset = 0;
            SearchSourceBuilder builder = createSourceBuilder4Agg(crit, offset, MAX_RESULT_WINDOW);
            manageSortRequest(index, builder, sort);
            request.source(builder);
            // ...Except if a closer reminder has already been found
            if (sortValues != null) {
                offset = searchPageNumber * MAX_RESULT_WINDOW;
            } else {
                LOGGER.debug("Search (after) : offset {}", offset);
                SearchResponse response = getSearchResponse(request);
                sortValues = response.getHits().getAt(response.getHits().getHits().length - 1).getSortValues();
                offset += MAX_RESULT_WINDOW;
            }
            // hack: -1min because a conflict problem with scheduler in the method saveReminder(..)
            OffsetDateTime expirationDate = OffsetDateTime.now()
                                                          .plus(KEEP_ALIVE_SCROLLING_TIME_MN - 1, ChronoUnit.MINUTES);

            int nextToLastOffset = (int) (pageRequest.getOffset() - (pageRequest.getOffset() % MAX_RESULT_WINDOW));
            // Execute as many request with search after as necessary to advance to next to last page of
            // MAX_RESULT_WINDOW size until offset
            while (offset < nextToLastOffset) {
                // Change offset
                LOGGER.debug("Search after : offset {}", offset);
                builder.from(0).searchAfter(sortValues);
                SearchResponse response = getSearchResponse(request);
                sortValues = response.getHits().getAt(response.getHits().getHits().length - 1).getSortValues();
                // Create a AbstractReminder and save it into ES for next page
                SearchAfterReminder reminder = new SearchAfterReminder(crit,
                                                                       searchKey,
                                                                       sort,
                                                                       PageRequest.of(offset / MAX_RESULT_WINDOW,
                                                                                      MAX_RESULT_WINDOW).next());
                reminder.setExpirationDate(expirationDate);
                reminder.setSearchAfterSortValues(response.getHits()
                                                          .getAt(response.getHits().getHits().length - 1)
                                                          .getSortValues());

                save(REMINDER_IDX, reminder);
                offset += MAX_RESULT_WINDOW;
            }
            // Beware of offset that is a multiple of MAX_RESULT_WINDOW
            if (pageRequest.getOffset() != offset) {
                int size = (int) (pageRequest.getOffset() - offset);
                LOGGER.debug("Search after : offset {}, size {}", offset, size);
                builder.size(size).searchAfter(sortValues).from(0); // needed by searchAfter
                SearchResponse response = getSearchResponse(request);
                sortValues = response.getHits().getAt(response.getHits().getHits().length - 1).getSortValues();
            }

            // Create a task to be executed after KEEP_ALIVE_SCROLLING_TIME_MN that delete all reminders whom
            // expiration date has been reached
            // No need to add type restriction, reminder is useless since ES6
            // hack: -1min because a conflict problem due to delete_by_query with scheduler in the method
            // saveReminder(..)
            reminderCleanExecutor.schedule(() -> deleteByQuery(REMINDER_IDX,
                                                               ICriterion.le("expirationDate", OffsetDateTime.now())),
                                           KEEP_ALIVE_SCROLLING_TIME_MN - 1,
                                           TimeUnit.MINUTES);

            return sortValues;
        } catch (IOException e) {
            throw new RsRuntimeException(e);
        }
    }

    private SearchSourceBuilder createSourceBuilder4Agg(ICriterion criterion) {
        return createSourceBuilder4Agg(criterion, 0, 0);
    }

    /**
     * Build a SearchSourceBuilder following given ICriterion on searchKey
     */
    private SearchSourceBuilder createSourceBuilder4Agg(ICriterion criterion, int from, int size) {
        // Use filter instead of "direct" query (in theory, quickest because no score is computed)
        QueryBuilder critBuilder = QueryBuilders.constantScoreQuery(criterion.accept(CRITERION_VISITOR));
        // Only return hits information
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder().query(critBuilder);
        searchSourceBuilder.from(from);
        searchSourceBuilder.size(size);
        searchSourceBuilder.trackTotalHits(true);
        return searchSourceBuilder;
    }

    @Override
    public <T extends IIndexable> Long count(SearchKey<?, T> searchKey, ICriterion criterion) {
        try {
            SearchSourceBuilder builder = createSourceBuilder4Agg(addTypes(criterion, searchKey.getSearchTypes()));
            CountRequest request = new CountRequest(searchKey.getSearchIndex()).source(builder);
            // Launch the request
            CountResponse response = client.count(request, RequestOptions.DEFAULT);
            return response.getCount();
        } catch (IOException e) {
            throw new RsRuntimeException(e);
        }
    }

    @Override
    public <T extends IIndexable> double sum(SearchKey<?, T> searchKey, ICriterion criterion, String attName) {
        try {
            SearchSourceBuilder builder = createSourceBuilder4Agg(addTypes(criterion, searchKey.getSearchTypes()));
            builder.aggregation(AggregationBuilders.sum(attName).field(attName));
            SearchRequest request = new SearchRequest(searchKey.getSearchIndex()).source(builder);
            // Launch the request
            SearchResponse response = getSearchResponse(request);
            return ((Sum) response.getAggregations().get(attName)).getValue();
        } catch (IOException e) {
            throw new RsRuntimeException(e);
        }
    }

    @Override
    public <T extends IIndexable> OffsetDateTime minDate(SearchKey<?, T> searchKey,
                                                         ICriterion criterion,
                                                         String attName) {
        try {
            SearchSourceBuilder builder = createSourceBuilder4Agg(addTypes(criterion, searchKey.getSearchTypes()));
            builder.aggregation(AggregationBuilders.min(attName).field(attName));
            SearchRequest request = new SearchRequest(searchKey.getSearchIndex()).source(builder);
            // Launch the request
            SearchResponse response = getSearchResponse(request);
            Min min = response.getAggregations().get(attName);
            if ((min == null) || !Double.isFinite(min.getValue())) {
                return null;
            }
            return OffsetDateTimeAdapter.parse(min.getValueAsString());
        } catch (IOException e) {
            throw new RsRuntimeException(e);
        }
    }

    @Override
    public <T extends IIndexable> OffsetDateTime maxDate(SearchKey<?, T> searchKey,
                                                         ICriterion criterion,
                                                         String attName) {
        try {
            SearchSourceBuilder builder = createSourceBuilder4Agg(addTypes(criterion, searchKey.getSearchTypes()));
            builder.aggregation(AggregationBuilders.max(attName).field(attName));
            SearchRequest request = new SearchRequest(searchKey.getSearchIndex()).source(builder);
            // Launch the request
            SearchResponse response = getSearchResponse(request);
            Max max = response.getAggregations().get(attName);
            if ((max == null) || !Double.isFinite(max.getValue())) {
                return null;
            }
            return OffsetDateTimeAdapter.parse(max.getValueAsString());
        } catch (IOException e) {
            throw new RsRuntimeException(e);
        }
    }

    @Override
    public <T extends IIndexable> Aggregations getAggregationsFor(SearchKey<?, T> searchKey,
                                                                  ICriterion criterion,
                                                                  Collection<AggregationBuilder> aggs,
                                                                  int limit) {
        try {
            SearchSourceBuilder builder = createSourceBuilder4Agg(addTypes(criterion, searchKey.getSearchTypes()),
                                                                  0,
                                                                  limit);
            aggs.forEach(builder::aggregation);
            SearchRequest request = new SearchRequest(searchKey.getSearchIndex()).source(builder);
            // Launch the request
            SearchResponse response = getSearchResponse(request);
            return response.getAggregations();
        } catch (IOException e) {
            throw new RsRuntimeException(e);
        }
    }

    @Override
    public <T extends IIndexable> Aggregations getAggregationsFor(AggregationSearchContext<T> searchRequest) {
        return getAggregationsFor(searchRequest.searchKey(),
                                  searchRequest.criterion(),
                                  searchRequest.aggregations(),
                                  searchRequest.limit());
    }

    @Override
    public <T extends IIndexable> Map<String, AggregationSearchContextResponse> getMultiAggregationsFor(Map<String, AggregationSearchContext<T>> searchRequests) {
        Map<String, Integer> responseIndexes = new HashMap<>();
        Map<String, AggregationSearchContextResponse> results = new HashMap<>();
        Integer currentIndex = 0;
        MultiSearchRequest multiSearchRequest = new MultiSearchRequest();

        // Initialize all requests
        for (Map.Entry<String, AggregationSearchContext<T>> entry : searchRequests.entrySet()) {
            responseIndexes.put(entry.getKey(), currentIndex);
            multiSearchRequest.add(createSearchRequestForAggregations(entry.getValue()));
            currentIndex++;
        }

        // Launch parallel requests
        try {
            final MultiSearchResponse response = client.msearch(multiSearchRequest, searchOptions);
            // Build response
            responseIndexes.forEach((key, index) -> {
                MultiSearchResponse.Item item = response.getResponses()[index];
                if (item.isFailure()) {
                    LOGGER.error(item.getFailureMessage());
                    results.put(key,
                                new AggregationSearchContextResponse(null, item.isFailure(), item.getFailureMessage()));
                } else {
                    results.put(key,
                                new AggregationSearchContextResponse(item.getResponse().getAggregations(),
                                                                     item.isFailure(),
                                                                     null));
                }
            });
            return results;
        } catch (IOException e) {
            throw new RsRuntimeException(e);
        }
    }

    private <T extends IIndexable> SearchRequest createSearchRequestForAggregations(AggregationSearchContext<T> searchRequest) {
        SearchSourceBuilder builder = createSourceBuilder4Agg(addTypes(searchRequest.criterion(),
                                                                       searchRequest.searchKey().getSearchTypes()),
                                                              0,
                                                              searchRequest.limit());
        searchRequest.aggregations().forEach(builder::aggregation);
        return new SearchRequest(searchRequest.searchKey().getSearchIndex()).source(builder);
    }

    @Override
    public <T extends IIndexable> Aggregations getAggregations(SearchKey<?, T> searchKey,
                                                               ICriterion criterion,
                                                               Collection<QueryableAttribute> attributes) {
        try {
            SearchSourceBuilder builder = createSourceBuilder4Agg(addTypes(criterion, searchKey.getSearchTypes()));
            for (QueryableAttribute qa : attributes) {
                if (qa.isTextAttribute() && (qa.getTermsLimit() > 0)) {
                    builder.aggregation(AggregationBuilders.terms(qa.getAttributeName())
                                                           .field(qa.getAttributeName() + KEYWORD_SUFFIX)
                                                           .size(qa.getTermsLimit()));
                } else if (qa.isBooleanAttribute()) {
                    builder.aggregation(AggregationBuilders.terms(qa.getAttributeName()).field(qa.getAttributeName()))
                           .size(2);
                } else if (qa.isGeoBoundsAttribute()) {
                    builder.aggregation(AggregationBuilders.geoBounds(qa.getAttributeName())
                                                           .field(qa.getAttributeName())
                                                           .wrapLongitude(true));
                } else if (!qa.isTextAttribute()) {
                    builder.aggregation(AggregationBuilders.stats(qa.getAttributeName()).field(qa.getAttributeName()));
                }
            }
            SearchRequest request = new SearchRequest(searchKey.getSearchIndex()).source(builder);
            // Launch the request
            SearchResponse response = getSearchResponse(request);
            // Update attributes with aggregation if any
            if (response.getAggregations() != null) {
                for (Aggregation agg : response.getAggregations()) {
                    attributes.stream()
                              .filter(a -> agg.getName().equals(a.getAttributeName()))
                              .findFirst()
                              .ifPresent(a -> a.setAggregation(agg));
                }
                return response.getAggregations();
            } else {
                return new Aggregations(Lists.newArrayList());
            }
        } catch (IOException e) {
            throw new RsRuntimeException(e);
        }
    }

    /**
     * Retrieve sorted set of given attribute unique string values following request
     *
     * @param searchKey search key
     * @param crit      criterion
     * @param attName   string attribute name (full path)
     * @param <T>       search type
     * @return a TreeSet&lt;String>
     */
    @Override
    public <T extends IIndexable> SortedSet<String> uniqueAlphaSorted(SearchKey<?, T> searchKey,
                                                                      ICriterion crit,
                                                                      String attName,
                                                                      int maxCount) {
        SortedSet<String> result = new TreeSet<>();
        unique(searchKey, addTypes(crit, searchKey.getSearchTypes()), attName, maxCount, result, new HashMap<>());
        return result;
    }

    /**
     * Retrieve set of given attribute unique typed values following request
     *
     * @param searchKey search key
     * @param crit      criterion
     * @param attName   attribute name (full path)
     * @param <T>       search type
     * @param <R>       result type
     * @return an HashSet
     */
    public <T, R> Set<R> unique(SearchKey<?, T> searchKey, ICriterion crit, String attName) {
        Set<R> result = new HashSet<>();
        unique(searchKey,
               addTypes(crit, searchKey.getSearchTypes()),
               attName,
               Integer.MAX_VALUE,
               result,
               new HashMap<>());
        return result;
    }

    /**
     * Retrieve set of given attribute unique typed values following request
     *
     * @param searchKey search key
     * @param crit      criterion
     * @param inAttName attribute name (full path)
     * @param maxCount  maximum value search
     * @param set       contains unique values wanted (modified)
     * @param facetsMap facets wanted
     * @param <T>       type of data on which unique values are searched
     * @param <R>       type of attribute value
     * @return facets asked
     */
    @SuppressWarnings("unchecked")
    private <T, R, S extends Set<R>> Set<IFacet<?>> unique(SearchKey<?, T> searchKey,
                                                           ICriterion crit,
                                                           String inAttName,
                                                           int maxCount,
                                                           S set,
                                                           Map<String, FacetType> facetsMap) {
        try {

            String attName = isTextMapping(searchKey.getSearchIndex(), inAttName) ?
                inAttName + KEYWORD_SUFFIX :
                inAttName;
            Consumer<SearchSourceBuilder> addUniqueTermAgg = (builder) -> builder.aggregation(AggregationBuilders.terms(
                attName).field(attName).size(maxCount));

            Tuple<SearchResponse, Set<IFacet<?>>> responseNFacets = searchWithFacets(searchKey,
                                                                                     crit,
                                                                                     PageRequest.of(0, 1),
                                                                                     addUniqueTermAgg,
                                                                                     null,
                                                                                     facetsMap);
            Terms terms = responseNFacets.v1().getAggregations().get(attName);
            for (Terms.Bucket bucket : terms.getBuckets()) {
                set.add((R) bucket.getKey());
            }
            return responseNFacets.v2();
        } catch (IOException e) {
            throw new RsRuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    @VisibleForTesting
    public <R> List<R> search(SearchKey<?, R> searchKey, ICriterion criterion, String sourceAttribute) {
        try {
            SortedSet<Object> objects = searchAllCache.getUnchecked(new CacheKey(searchKey,
                                                                                 addTypes(criterion,
                                                                                          searchKey.getSearchTypes()),
                                                                                 sourceAttribute)).v1();
            return objects.stream().map(o -> (R) o).collect(Collectors.toList());
        } catch (final JsonSyntaxException e) {
            throw new RsRuntimeException(e);
        }
    }

    @Override
    public <R, U extends IIndexable> FacetPage<U> search(SearchKey<?, R[]> sourceSearchKey,
                                                         ICriterion sourceSearchCriterion,
                                                         String sourceAttribute,
                                                         Predicate<R> sourceFilterPredicate,
                                                         Function<Set<R>, Page<U>> toAskEntityFct,
                                                         Map<String, FacetType> facetsMap,
                                                         Pageable pageRequest) {
        // --- INPUT SEARCH ---
        // Search input elements from the ES cache
        Tuple<SortedSet<Object>, Set<IFacet<?>>> tupleInputObjects = searchAllCache.getUnchecked(new CacheKey(
            sourceSearchKey,
            addTypes(sourceSearchCriterion, sourceSearchKey.getSearchTypes()),
            sourceAttribute,
            facetsMap));
        Set<R> inputObjects = tupleInputObjects.v1().stream().map(o -> (R) o).collect(Collectors.toSet());
        if (sourceFilterPredicate != null) {
            inputObjects = inputObjects.stream().filter(sourceFilterPredicate).collect(Collectors.toSet());
        }
        // --- JOINED OBJECT SEARCH ---
        // return objects from ES repository based on inputObjects, facets with no modification, page and size of input objects to make nextPageable work
        return new FacetPage<>(toAskEntityFct.apply(inputObjects).getContent(),
                               tupleInputObjects.v2(),
                               pageRequest,
                               inputObjects.size());
    }

    /**
     * Is given attribute (can be a composite attribute like toto.titi) of type text from ES mapping ?
     *
     * @param inIndex   concerned index
     * @param attribute attribute from type
     * @return true or false
     */
    private boolean isTextMapping(String inIndex, String attribute) throws IOException {
        String index = inIndex.toLowerCase();
        try {
            Request request = new Request("GET", index + "/_mapping/field/" + attribute);
            Response response = client.getLowLevelClient().performRequest(request);
            try (InputStream is = response.getEntity().getContent()) {
                Map<String, Object> map = XContentHelper.convertToMap(XContentType.JSON.xContent(), is, true);
                // If attribute exists, response should contain this chain of several maps :
                // <index>."mappings".<type>.<attribute>."mapping".<attribute_last_path>."type"
                if ((map != null) && !map.isEmpty()) {
                    // In case attribute is toto.titi.tutu, we will need "tutu" further
                    String lastPathAtt = attribute.contains(".") ?
                        attribute.substring(attribute.lastIndexOf('.') + 1) :
                        attribute;
                    // BEWARE : instead of map.get(index) on the innermost map value retrieval, we use directly
                    // map.values().iterator().next() to get value associated to singleton element whatever the key is
                    // Indeed, because of Elasticsearch version 6 single type update, some indices are retrieved through
                    // an alias. Asking an alias mapping returned a block with index name, not alias name
                    return toMap(toMap(toMap(toMap(toMap(toMap(map.values().iterator().next()).get("mappings"))).get(
                        attribute)).get(MAPPING)).get(lastPathAtt)).get("type").equals("text");

                }
            }
        } catch (ResponseException e) {
            // In case index does not exist and/or mapping not available
            if (e.getResponse().getStatusLine().getStatusCode() == HttpStatus.SC_NOT_FOUND) {
                return false;
            }
            LOGGER.error("Failed to retrieve ES mapping on attribute {}", attribute);
            throw e;
        }
        return false;
    }

    /**
     * Add sort to the request
     *
     * @param builder search request
     * @param sort    map(attribute name, true if ascending)
     */
    private void manageSortRequest(String index, SearchSourceBuilder builder, Sort sort) throws IOException {
        // Convert Sort into linked hash map
        LinkedHashMap<String, Boolean> ascSortMap = new SortToLinkedHashMap().convert(sort);

        // Because string attributes are not indexed with Elasticsearch, it is necessary to add ".keyword" at
        // end of attribute name into sort request. So we need to know string attributes
        Response response;
        try {
            response = client.getLowLevelClient()
                             .performRequest(new Request("GET",
                                                         index + "/_mapping/field/" + Joiner.on(",")
                                                                                            .join(ascSortMap.keySet())));
        } catch (ResponseException e) {
            LOGGER.error(e.getMessage(), e);
            if (e.getMessage().contains(INDEX_NOT_FOUND_EXCEPTION)) {
                throw new ESIndexNotFoundRuntimeException();
            }
            throw e;
        }
        try (InputStream is = response.getEntity().getContent()) {
            Map<String, Object> map = XContentHelper.convertToMap(XContentType.JSON.xContent(), is, true);
            if ((map != null) && !map.isEmpty()) {
                // NOTE: in our context, attributes have same metada for all types so once we found one, we stop.
                // To do that, we create a new LinkedHashMap to KEEPS keys order !!! (crucial)
                LinkedHashMap<String, Boolean> updatedAscSortMap = new LinkedHashMap<>(ascSortMap.size());
                for (Map.Entry<String, Boolean> sortEntry : ascSortMap.entrySet()) {
                    String attribute = sortEntry.getKey();
                    if (isTextMapping(map, attribute)) {
                        updatedAscSortMap.put(attribute + KEYWORD_SUFFIX, sortEntry.getValue());
                    } else {
                        updatedAscSortMap.put(attribute, sortEntry.getValue());
                    }
                }

                // Add sort to request
                updatedAscSortMap.forEach((key, value) -> builder.sort(SortBuilders.fieldSort(key)
                                                                                   .order(value ?
                                                                                              SortOrder.ASC :
                                                                                              SortOrder.DESC)
                                                                                   .unmappedType(DOUBLE)));
                // "double" because a type is necessary. This has only an impact when seaching on several indices if
                // property is mapped on one and no on the other(s). Will see this when it happens (if it happens a day)
                // entry -> builder.sort(entry.getKey(), entry.getValue() ? SortOrder.ASC : SortOrder.DESC));
            }
        }
    }

    /**
     * Add aggregations to the search request.
     *
     * @param facetsMap asked facets
     * @param builder   search request
     * @return true if a second pass is needed (managing range facets)
     */
    private boolean manageFirstPassRequestAggregations(Map<String, FacetType> facetsMap, SearchSourceBuilder builder) {
        boolean twoPassRequestNeeded = false;
        if (facetsMap != null) {
            // Numeric/date facets needs :
            // First to add a percentiles aggregation to retrieved 9 values corresponding to
            // 10%, 20 %, ..., 90 % of the values
            // Secondly to add a range aggregation with these 9 values in order to retrieve 10 buckets of equal
            // size of values
            for (Map.Entry<String, FacetType> entry : facetsMap.entrySet()) {
                FacetType facetType = entry.getValue();
                if ((facetType == FacetType.NUMERIC) || (facetType == FacetType.DATE)) {
                    // Add min aggregation and max aggregagtion when a range aggregagtion is asked for (NUMERIC and DATE
                    // facets leed to range aggregagtion at second pass) to avoid ranges with Infinties values
                    builder.aggregation(FacetType.MIN.accept(aggBuilderFacetTypeVisitor, entry.getKey()));
                    builder.aggregation(FacetType.MAX.accept(aggBuilderFacetTypeVisitor, entry.getKey()));

                    twoPassRequestNeeded = true;
                }
                builder.aggregation(facetType.accept(aggBuilderFacetTypeVisitor, entry.getKey()));
            }
        }
        return twoPassRequestNeeded;
    }

    /**
     * Add aggregations to the search request (second pass). For range aggregations, use percentiles results (from first
     * pass request results) to create range aggregagtions.
     *
     * @param facetsMap asked facets
     * @param builder   search request
     * @param aggsMap   first pass aggregagtions results
     */
    private void manageSecondPassRequestAggregations(Map<String, FacetType> facetsMap,
                                                     SearchSourceBuilder builder,
                                                     Map<String, Aggregation> aggsMap) {
        for (Map.Entry<String, FacetType> entry : facetsMap.entrySet()) {
            FacetType facetType = entry.getValue();
            String attributeName = entry.getKey();
            String attName;
            // Replace percentiles aggregations by range aggregations
            if ((facetType == FacetType.NUMERIC) || (facetType == FacetType.DATE)) {
                attName = facetType == FacetType.NUMERIC ?
                    attributeName + NUMERIC_FACET_SUFFIX :
                    attributeName + DATE_FACET_SUFFIX;
                Percentiles percentiles = (Percentiles) aggsMap.get(attName);
                // No percentile values for this property => skip aggregation
                if (Iterables.all(percentiles, p -> Double.isNaN(p.getValue()))) {
                    continue;
                }
                AggregationBuilder aggBuilder = facetType == FacetType.NUMERIC ?
                    FacetType.RANGE_DOUBLE.accept(aggBuilderFacetTypeVisitor, attributeName, percentiles) :
                    FacetType.RANGE_DATE.accept(aggBuilderFacetTypeVisitor, attributeName, percentiles);
                // In case range contains only one value, better remove facet
                if (aggBuilder != null) {
                    // And add max and min aggregations
                    builder.aggregation(aggBuilder);
                    // And add max and min aggregations
                    builder.aggregation(FacetType.MIN.accept(aggBuilderFacetTypeVisitor, attributeName));
                    builder.aggregation(FacetType.MAX.accept(aggBuilderFacetTypeVisitor, attributeName));
                }
            } else { // Let others as it
                builder.aggregation(facetType.accept(aggBuilderFacetTypeVisitor, attributeName));
            }
        }
    }

    /**
     * Compute aggregations results to fill results facet map of an attribute
     *
     * @param aggsMap       aggregation resuls map
     * @param facets        map of results facets
     * @param facetType     type of facet for given attribute
     * @param attributeName given attribute
     */

    private void fillFacets(Map<String, Aggregation> aggsMap,
                            Set<IFacet<?>> facets,
                            FacetType facetType,
                            String attributeName,
                            long totalHits) {
        switch (facetType) {
            case STRING:
                fillStringFacets(aggsMap, facets, attributeName);
                break;
            case BOOLEAN:
                fillBooleanFacets(aggsMap, facets, attributeName, totalHits);
                break;
            case NUMERIC:
                fillNumericFacets(aggsMap, facets, attributeName);
                break;
            case DATE:
                fillDateFacets(aggsMap, facets, attributeName);
                break;
            default:
                break;
        }
    }

    private void fillDateFacets(Map<String, Aggregation> aggsMap, Set<IFacet<?>> facets, String attributeName) {
        org.elasticsearch.search.aggregations.bucket.range.Range dateRange = (org.elasticsearch.search.aggregations.bucket.range.Range) aggsMap.get(
            attributeName + AggregationBuilderFacetTypeVisitor.RANGE_FACET_SUFFIX);
        if (dateRange != null) {
            Map<Range<OffsetDateTime>, Long> valueMap = new LinkedHashMap<>();
            for (Bucket bucket : dateRange.getBuckets()) {
                // Retrieve min and max aggregagtions to replace -Infinity and +Infinity
                Min min = (Min) aggsMap.get(attributeName + AggregationBuilderFacetTypeVisitor.MIN_FACET_SUFFIX);
                Max max = (Max) aggsMap.get(attributeName + AggregationBuilderFacetTypeVisitor.MAX_FACET_SUFFIX);
                // Parsing ranges
                Range<OffsetDateTime> valueRange;
                // Case with no value : every bucket has a NaN value (as from, to or both)
                if (Objects.equals(bucket.getTo(), Double.NaN) || Objects.equals(bucket.getFrom(), Double.NaN)) {
                    // If first bucket contains NaN value, it means there are no value at all
                    return;
                }
                // (-∞ -> ?
                if (bucket.getFromAsString() == null) {
                    // (-∞ -> +∞) (completely dumb but...who knows ?)
                    if (bucket.getToAsString() == null) {
                        // range is then [min, max]
                        valueRange = Range.closed(OffsetDateTimeAdapter.parse(min.getValueAsString()),
                                                  OffsetDateTimeAdapter.parse(max.getValueAsString()));
                    } else { // (-∞ -> value[
                        // range is then [min, value[
                        valueRange = Range.closedOpen(OffsetDateTimeAdapter.parse(min.getValueAsString()),
                                                      OffsetDateTimeAdapter.parse(bucket.getToAsString()));
                    }
                } else if (bucket.getToAsString() == null) { // [value -> +∞)
                    // range is then [value, max ]
                    valueRange = Range.closed(OffsetDateTimeAdapter.parse(bucket.getFromAsString()),
                                              OffsetDateTimeAdapter.parse(max.getValueAsString()));
                } else { // [value -> value[
                    valueRange = Range.closedOpen(OffsetDateTimeAdapter.parse(bucket.getFromAsString()),
                                                  OffsetDateTimeAdapter.parse(bucket.getToAsString()));
                }
                valueMap.put(valueRange, bucket.getDocCount());
            }
            facets.add(new DateFacet(attributeName, valueMap));
        }
    }

    private void fillNumericFacets(Map<String, Aggregation> aggsMap, Set<IFacet<?>> facets, String attributeName) {
        org.elasticsearch.search.aggregations.bucket.range.Range numRange = (org.elasticsearch.search.aggregations.bucket.range.Range) aggsMap.get(
            attributeName + AggregationBuilderFacetTypeVisitor.RANGE_FACET_SUFFIX);
        if (numRange != null) {
            // Retrieve min and max aggregagtions to replace -Infinity and +Infinity
            Min min = (Min) aggsMap.get(attributeName + AggregationBuilderFacetTypeVisitor.MIN_FACET_SUFFIX);
            Max max = (Max) aggsMap.get(attributeName + AggregationBuilderFacetTypeVisitor.MAX_FACET_SUFFIX);
            // Parsing ranges
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
                        // Better not return a facet
                        return;
                    } // (-∞ -> value [
                    // range is then [min -> value [, because min value is scaled it is necessary to choose a little
                    // less
                    valueRange = Range.closedOpen(EsHelper.scaledDown(min.getValue()), (Double) bucket.getTo());
                } else if (Objects.equals(bucket.getTo(), Double.POSITIVE_INFINITY)) { // [value -> +∞)
                    // range is then [value, max], because max value is scaled it is necessary to choose a little more
                    valueRange = Range.closed((Double) bucket.getFrom(), EsHelper.scaledUp(max.getValue()));
                } else { // [value -> value [
                    valueRange = Range.closedOpen((Double) bucket.getFrom(), (Double) bucket.getTo());
                }
                valueMap.put(valueRange, bucket.getDocCount());
            }
            facets.add(new NumericFacet(attributeName, valueMap));
        }
    }

    private void fillStringFacets(Map<String, Aggregation> aggsMap, Set<IFacet<?>> facets, String attributeName) {
        Terms terms = (Terms) aggsMap.get(attributeName + AggregationBuilderFacetTypeVisitor.STRING_FACET_SUFFIX);
        if (terms.getBuckets().isEmpty()) {
            return;
        }
        Map<String, Long> valueMap = new LinkedHashMap<>(terms.getBuckets().size());
        terms.getBuckets().forEach(b -> valueMap.put(b.getKeyAsString(), b.getDocCount()));
        facets.add(new StringFacet(attributeName, valueMap, terms.getSumOfOtherDocCounts()));
    }

    private void fillBooleanFacets(Map<String, Aggregation> aggsMap,
                                   Set<IFacet<?>> facets,
                                   String attributeName,
                                   long totalHits) {
        Terms terms = (Terms) aggsMap.get(attributeName + AggregationBuilderFacetTypeVisitor.STRING_FACET_SUFFIX);
        if (terms.getBuckets().isEmpty()) {
            return;
        }
        Map<Boolean, Long> valueMap = new LinkedHashMap<>(terms.getBuckets().size());
        AtomicLong docWithTrueOrFalseCount = new AtomicLong(0);
        terms.getBuckets().forEach(b -> {
            valueMap.put(Boolean.valueOf(b.getKeyAsString()), b.getDocCount());
            docWithTrueOrFalseCount.addAndGet(b.getDocCount());
        });

        // A boolean bucket boes not set null values into "sum_other_doc_count" so "others" is computed from
        // total - true values - false values
        facets.add(new BooleanFacet(attributeName, valueMap, totalHits - docWithTrueOrFalseCount.get()));
    }

    @Override
    public <T extends IIndexable> Page<T> multiFieldsSearch(SearchKey<T, T> searchKey,
                                                            Pageable pageRequest,
                                                            Object inValue,
                                                            String... fields) {
        try {
            final List<T> results = new ArrayList<>();
            // OffsetDateTime must be formatted to be correctly used following Gson mapping
            Object value = inValue instanceof OffsetDateTime ?
                OffsetDateTimeAdapter.format((OffsetDateTime) inValue) :
                inValue;
            // Create filter query with all asked types and multi match query
            BoolQueryBuilder filterBuilder = QueryBuilders.boolQuery();
            for (String type : searchKey.getSearchTypes()) {
                filterBuilder = filterBuilder.must(QueryBuilders.matchPhraseQuery("type", type));
            }
            filterBuilder = filterBuilder.must(QueryBuilders.multiMatchQuery(value, fields));

            QueryBuilder queryBuilder = QueryBuilders.constantScoreQuery(filterBuilder);
            SearchSourceBuilder builder = new SearchSourceBuilder().query(queryBuilder)
                                                                   .from((int) pageRequest.getOffset())
                                                                   .size(pageRequest.getPageSize());
            SearchRequest request = new SearchRequest(searchKey.getSearchIndex()).source(builder);

            SearchResponse response = getSearchResponse(request);
            SearchHits hits = response.getHits();
            for (SearchHit hit : hits) {
                results.add(deserializeHitsStrategy.deserializeJson(hit.getSourceAsString(),
                                                                    searchKey.fromType(hit.getType())));
            }
            return new PageImpl<>(results, pageRequest, response.getHits().getTotalHits().value);
        } catch (final JsonSyntaxException | IOException e) {
            throw new RsRuntimeException(e);
        }
    }

    @Override
    public <T extends IIndexable & IDocFiles> void computeInternalDataFilesSummary(SearchKey<T, T> searchKey,
                                                                                   ICriterion crit,
                                                                                   String discriminantProperty,
                                                                                   Optional<String> discriminentPropertyInclude,
                                                                                   DocFilesSummary summary,
                                                                                   String... fileTypes) {
        try {
            if ((fileTypes == null) || (fileTypes.length == 0)) {
                throw new IllegalArgumentException("At least one file type must be provided");
            }
            SearchSourceBuilder builder = createSourceBuilder4Agg(addTypes(crit, searchKey.getSearchTypes()));
            // Add files count and files sum size aggregations
            addFilesCountAndSumAggs(searchKey, discriminantProperty, discriminentPropertyInclude, builder, fileTypes);

            // We only need aggregation so set hits size to 0
            builder.size(0);
            SearchRequest request = new SearchRequest(searchKey.getSearchIndex()).source(builder);
            // Launch the request
            SearchResponse response = getSearchResponse(request);

            // First "global" aggregations results
            summary.addDocumentsCount(response.getHits().getTotalHits().value);
            Aggregations aggs = response.getAggregations();
            for (String fileType : fileTypes) {

                // ref
                // ref:count
                String refCountName = TOTAL_PREFIX + fileType + REF_FILES_COUNT_SUFFIX;
                ValueCount refValueCount = ((Filter) aggs.get(refCountName)).getAggregations().get(refCountName);
                // ref:size
                String refSumName = TOTAL_PREFIX + fileType + REF_FILES_SIZE_SUFFIX;
                Sum refSum = ((Filter) aggs.get(refSumName)).getAggregations().get(refSumName);
                // ref:expose details in summary
                summary.getFileTypesSummaryMap().compute(fileType + REF_SUFFIX, (k, fs) -> {
                    if (fs == null) {
                        return new FilesSummary(refValueCount.getValue(), (long) refSum.getValue());
                    }
                    return new FilesSummary(fs.getFilesCount() + refValueCount.getValue(),
                                            (long) (fs.getFilesSize() + refSum.getValue()));
                });

                // !ref
                // !ref:count
                String notRefCountName = TOTAL_PREFIX + fileType + NOT_REF_FILES_COUNT_SUFFIX;
                ValueCount notRefValueCount = ((Filter) aggs.get(notRefCountName)).getAggregations()
                                                                                  .get(notRefCountName);
                // !ref:size
                String notRefSumName = TOTAL_PREFIX + fileType + NOT_REF_FILES_SIZE_SUFFIX;
                Sum notRefSum = ((Filter) aggs.get(notRefSumName)).getAggregations().get(notRefSumName);
                // !ref:expose details in summary
                summary.getFileTypesSummaryMap().compute(fileType + NOT_REF_SUFFIX, (k, fs) -> {
                    if (fs == null) {
                        return new FilesSummary(notRefValueCount.getValue(), (long) notRefSum.getValue());
                    }
                    return new FilesSummary(fs.getFilesCount() + notRefValueCount.getValue(),
                                            (long) (fs.getFilesSize() + notRefSum.getValue()));
                });

                // total
                // total:count
                long totalFileCount = refValueCount.getValue() + notRefValueCount.getValue();
                // total:size
                long totalFileSize = (long) (refSum.getValue() + notRefSum.getValue());
                // total:expose details in summary
                summary.getFileTypesSummaryMap().compute(fileType, (k, fs) -> {
                    if (fs == null) {
                        return new FilesSummary(totalFileCount, totalFileSize);
                    }
                    return new FilesSummary(fs.getFilesCount() + totalFileCount, fs.getFilesSize() + totalFileSize);
                });

                // 2020-10-22: keep this for backward compatibility
                // In previous version DocFilesSummary did not have fileTypesSummary
                // Check commit diff for more context.
                summary.addFilesCount(totalFileCount);
                summary.addFilesSize(totalFileSize);
            }
            // Then discriminants buckets aggregations results
            Terms buckets = aggs.get(discriminantProperty);
            for (Terms.Bucket bucket : buckets.getBuckets()) {
                // Usually discriminant = tag name
                String discriminant = bucket.getKeyAsString();
                if (!summary.getSubSummariesMap().containsKey(discriminant)) {
                    summary.getSubSummariesMap()
                           .put(discriminant,
                                new DocFilesSubSummary(Arrays.stream(fileTypes)
                                                             .flatMap(ft -> Stream.of(ft,
                                                                                      ft + REF_SUFFIX,
                                                                                      ft + NOT_REF_SUFFIX))
                                                             .toArray(String[]::new)));
                }
                DocFilesSubSummary discSummary = summary.getSubSummariesMap().get(discriminant);
                discSummary.addDocumentsCount(bucket.getDocCount());
                Aggregations discAggs = bucket.getAggregations();
                for (String fileType : fileTypes) {
                    ValueCount refValueCount = ((Filter) discAggs.get(fileType
                                                                      + REF_FILES_COUNT_SUFFIX)).getAggregations()
                                                                                                .get(fileType
                                                                                                     + REF_FILES_COUNT_SUFFIX);
                    Sum refSum = ((Filter) discAggs.get(fileType + REF_FILES_SIZE_SUFFIX)).getAggregations()
                                                                                          .get(fileType
                                                                                               + REF_FILES_SIZE_SUFFIX);

                    FilesSummary refFilesSummary = discSummary.getFileTypesSummaryMap().get(fileType + REF_SUFFIX);
                    refFilesSummary.addFilesCount(refValueCount.getValue());
                    refFilesSummary.addFilesSize((long) refSum.getValue());

                    ValueCount notRefValueCount = ((Filter) discAggs.get(fileType
                                                                         + NOT_REF_FILES_COUNT_SUFFIX)).getAggregations()
                                                                                                       .get(fileType
                                                                                                            + NOT_REF_FILES_COUNT_SUFFIX);
                    Sum notRefSum = ((Filter) discAggs.get(fileType + NOT_REF_FILES_SIZE_SUFFIX)).getAggregations()
                                                                                                 .get(fileType
                                                                                                      + NOT_REF_FILES_SIZE_SUFFIX);

                    FilesSummary notRefFilesSummary = discSummary.getFileTypesSummaryMap()
                                                                 .get(fileType + NOT_REF_SUFFIX);
                    notRefFilesSummary.addFilesCount(notRefValueCount.getValue());
                    notRefFilesSummary.addFilesSize((long) notRefSum.getValue());

                    FilesSummary filesSummary = discSummary.getFileTypesSummaryMap().get(fileType);
                    long filesCount = refValueCount.getValue() + notRefValueCount.getValue();
                    long filesSize = (long) (refSum.getValue() + notRefSum.getValue());
                    filesSummary.addFilesCount(filesCount);
                    filesSummary.addFilesSize(filesSize);

                    discSummary.addFilesCount(filesCount);
                    discSummary.addFilesSize(filesSize);
                }

            }
        } catch (IOException e) {
            throw new RsRuntimeException(e);
        }
    }

    private <T extends IIndexable & IDocFiles> void addFilesCountAndSumAggs(SearchKey<T, T> searchKey,
                                                                            String discriminantProperty,
                                                                            Optional<String> discriminantPropertyInclude,
                                                                            SearchSourceBuilder builder,
                                                                            String[] fileTypes) throws IOException {
        // Add aggregations to manage compute summary
        // First "global" aggregations on each asked file types
        for (String fileType : fileTypes) {
            String fileSizeField = FEATURE_FILES_PREFIX + fileType + FILESIZE_SUFFIX;
            String fileReferenceField = FEATURE_FILES_PREFIX + fileType + REFERENCE_SUFFIX;

            // file count
            String refCount = TOTAL_PREFIX + fileType + REF_FILES_COUNT_SUFFIX;
            String notRefCount = TOTAL_PREFIX + fileType + NOT_REF_FILES_COUNT_SUFFIX;

            TermQueryBuilder refFilter = QueryBuilders.termQuery(fileReferenceField, true);
            TermQueryBuilder notRefFilter = QueryBuilders.termQuery(fileReferenceField, false);

            FilterAggregationBuilder refCountAgg = AggregationBuilders.filter(refCount, refFilter)
                                                                      .subAggregation(AggregationBuilders.count(refCount)
                                                                                                         .field(
                                                                                                             fileSizeField));

            FilterAggregationBuilder notRefCountAgg = AggregationBuilders.filter(notRefCount, notRefFilter)
                                                                         .subAggregation(AggregationBuilders.count(
                                                                             notRefCount).field(fileSizeField));

            // file size sum
            String refSum = TOTAL_PREFIX + fileType + REF_FILES_SIZE_SUFFIX;
            String notRefSum = TOTAL_PREFIX + fileType + NOT_REF_FILES_SIZE_SUFFIX;

            FilterAggregationBuilder refSumAgg = AggregationBuilders.filter(refSum, refFilter)
                                                                    .subAggregation(AggregationBuilders.sum(refSum)
                                                                                                       .field(
                                                                                                           fileSizeField));

            FilterAggregationBuilder notRefSumAgg = AggregationBuilders.filter(notRefSum, notRefFilter)
                                                                       .subAggregation(AggregationBuilders.sum(notRefSum)
                                                                                                          .field(
                                                                                                              fileSizeField));

            // add aggs to builder
            builder.aggregation(refCountAgg);
            builder.aggregation(notRefCountAgg);
            builder.aggregation(refSumAgg);
            builder.aggregation(notRefSumAgg);
        }

        // Discriminant distribution aggregator
        TermsAggregationBuilder termsAggBuilder = AggregationBuilders.terms(discriminantProperty)
                                                                     .size(Integer.MAX_VALUE);

        if (isTextMapping(searchKey.getSearchIndex(), discriminantProperty)) {
            termsAggBuilder.field(discriminantProperty + KEYWORD_SUFFIX);
            if (discriminantPropertyInclude.isPresent()) {
                termsAggBuilder.includeExclude(new IncludeExclude(discriminantPropertyInclude.get(), null));
            }
        } else {
            termsAggBuilder.field(discriminantProperty);
        }
        // and "total" aggregations on each asked file types
        for (String fileType : fileTypes) {
            String fileSizeField = FEATURE_FILES_PREFIX + fileType + FILESIZE_SUFFIX;
            String fileReferenceField = FEATURE_FILES_PREFIX + fileType + REFERENCE_SUFFIX;

            // file count
            String refCount = fileType + REF_FILES_COUNT_SUFFIX;
            String notRefCount = fileType + NOT_REF_FILES_COUNT_SUFFIX;

            TermQueryBuilder refFilter = QueryBuilders.termQuery(fileReferenceField, true);
            TermQueryBuilder notRefFilter = QueryBuilders.termQuery(fileReferenceField, false);

            FilterAggregationBuilder refCountAgg = AggregationBuilders.filter(refCount, refFilter)
                                                                      .subAggregation(AggregationBuilders.count(refCount)
                                                                                                         .field(
                                                                                                             fileSizeField));

            FilterAggregationBuilder notRefCountAgg = AggregationBuilders.filter(notRefCount, notRefFilter)
                                                                         .subAggregation(AggregationBuilders.count(
                                                                             notRefCount).field(fileSizeField));

            // file size sum
            String refSum = fileType + REF_FILES_SIZE_SUFFIX;
            String notRefSum = fileType + NOT_REF_FILES_SIZE_SUFFIX;

            FilterAggregationBuilder refSumAgg = AggregationBuilders.filter(refSum, refFilter)
                                                                    .subAggregation(AggregationBuilders.sum(refSum)
                                                                                                       .field(
                                                                                                           fileSizeField));

            FilterAggregationBuilder notRefSumAgg = AggregationBuilders.filter(notRefSum, notRefFilter)
                                                                       .subAggregation(AggregationBuilders.sum(notRefSum)
                                                                                                          .field(
                                                                                                              fileSizeField));

            // add aggs to builder
            termsAggBuilder.subAggregation(refCountAgg);
            termsAggBuilder.subAggregation(notRefCountAgg);
            termsAggBuilder.subAggregation(refSumAgg);
            termsAggBuilder.subAggregation(notRefSumAgg);
        }
        builder.aggregation(termsAggBuilder);
    }

    @Override
    public <T extends IIndexable & IDocFiles> void computeExternalDataFilesSummary(SearchKey<T, T> searchKey,
                                                                                   ICriterion crit,
                                                                                   String discriminantProperty,
                                                                                   Optional<String> discriminentPropertyInclude,
                                                                                   DocFilesSummary summary,
                                                                                   String... fileTypes) {
        try {
            if ((fileTypes == null) || (fileTypes.length == 0)) {
                throw new IllegalArgumentException("At least one file type must be provided");
            }

            SearchSourceBuilder builder = createSourceBuilder4Agg(addTypes(crit, searchKey.getSearchTypes()));
            // Add files cardinality aggregation
            addFilesCardinalityAgg(searchKey, discriminantProperty, discriminentPropertyInclude, builder, fileTypes);

            SearchRequest request = new SearchRequest(searchKey.getSearchIndex()).source(builder);

            // Launch the request
            SearchResponse response = getSearchResponse(request);
            // First "global" aggregations results
            summary.addDocumentsCount(response.getHits().getTotalHits().value);
            Aggregations aggs = response.getAggregations();
            for (String fileType : fileTypes) {
                // ref
                Cardinality refCardinality = ((Filter) aggs.get(TOTAL_PREFIX
                                                                + fileType
                                                                + REF_FILES_COUNT_SUFFIX)).getAggregations()
                                                                                          .get(TOTAL_PREFIX
                                                                                               + fileType
                                                                                               + REF_FILES_COUNT_SUFFIX);
                summary.getFileTypesSummaryMap().compute(fileType + REF_SUFFIX, (k, fs) -> {
                    if (fs == null) {
                        return new FilesSummary(refCardinality.getValue(), 0);
                    }
                    return new FilesSummary(fs.getFilesCount() + refCardinality.getValue(), 0);
                });

                // !ref
                Cardinality notRefCardinality = ((Filter) aggs.get(TOTAL_PREFIX
                                                                   + fileType
                                                                   + NOT_REF_FILES_COUNT_SUFFIX)).getAggregations()
                                                                                                 .get(TOTAL_PREFIX
                                                                                                      + fileType
                                                                                                      + NOT_REF_FILES_COUNT_SUFFIX);
                summary.getFileTypesSummaryMap().compute(fileType + NOT_REF_SUFFIX, (k, fs) -> {
                    if (fs == null) {
                        return new FilesSummary(notRefCardinality.getValue(), 0);
                    }
                    return new FilesSummary(fs.getFilesCount() + notRefCardinality.getValue(), 0);
                });

                // total
                long totalFileCount = refCardinality.getValue() + notRefCardinality.getValue();
                // total:expose details in summary
                summary.getFileTypesSummaryMap().compute(fileType, (k, fs) -> {
                    if (fs == null) {
                        return new FilesSummary(totalFileCount, 0);
                    }
                    return new FilesSummary(fs.getFilesCount() + totalFileCount, 0);
                });

                // 2020-10-22: keep this for backward compatibility
                // In previous version DocFilesSummary did not have fileTypesSummary
                // Check commit diff for more context.
                summary.addFilesCount(totalFileCount);
            }
            // Then discriminants buckets aggregations results
            Terms buckets = aggs.get(discriminantProperty);
            for (Terms.Bucket bucket : buckets.getBuckets()) {
                String discriminant = bucket.getKeyAsString();
                if (!summary.getSubSummariesMap().containsKey(discriminant)) {
                    summary.getSubSummariesMap()
                           .put(discriminant,
                                new DocFilesSubSummary(Arrays.stream(fileTypes)
                                                             .flatMap(ft -> Stream.of(ft,
                                                                                      ft + REF_SUFFIX,
                                                                                      ft + NOT_REF_SUFFIX))
                                                             .toArray(String[]::new)));
                }
                DocFilesSubSummary discSummary = summary.getSubSummariesMap().get(discriminant);
                discSummary.addDocumentsCount(bucket.getDocCount());
                Aggregations discAggs = bucket.getAggregations();
                for (String fileType : fileTypes) {
                    Cardinality refCardinality = ((Filter) discAggs.get(fileType
                                                                        + REF_FILES_COUNT_SUFFIX)).getAggregations()
                                                                                                  .get(fileType
                                                                                                       + REF_FILES_COUNT_SUFFIX);
                    Cardinality notRefCardinality = ((Filter) discAggs.get(fileType
                                                                           + NOT_REF_FILES_COUNT_SUFFIX)).getAggregations()
                                                                                                         .get(fileType
                                                                                                              + NOT_REF_FILES_COUNT_SUFFIX);

                    FilesSummary refFilesSummary = discSummary.getFileTypesSummaryMap().get(fileType + REF_SUFFIX);
                    refFilesSummary.addFilesCount(refCardinality.getValue());

                    FilesSummary notRefFilesSummary = discSummary.getFileTypesSummaryMap()
                                                                 .get(fileType + NOT_REF_SUFFIX);
                    notRefFilesSummary.addFilesCount(notRefCardinality.getValue());

                    FilesSummary filesSummary = discSummary.getFileTypesSummaryMap().get(fileType);
                    long cardinality = refCardinality.getValue() + notRefCardinality.getValue();
                    filesSummary.addFilesCount(cardinality);
                    discSummary.addFilesCount(cardinality);
                }
            }
        } catch (IOException e) {
            throw new RsRuntimeException(e);
        }
    }

    /**
     * Difference between addFilesCardinalityAgg and addFilesCountAndSumAggs is on the type of aggregagtion (and the
     * file size sum of course).
     * In first case, propertie values are counted (for internal data files, it is sufficient because data files should
     * be differents), in second, distinct property values are counted (and for external files, which is the case here,
     * nothing prevents from use same uri on several data objects)
     */
    private <T extends IIndexable & IDocFiles> void addFilesCardinalityAgg(SearchKey<T, T> searchKey,
                                                                           String discriminantProperty,
                                                                           Optional<String> discriminentPropertyInclude,
                                                                           SearchSourceBuilder builder,
                                                                           String[] fileTypes) throws IOException {
        // Add aggregations to manage compute summary
        // First "global" aggregations on each asked file types
        for (String fileType : fileTypes) {
            String fileUriField = FEATURE_FILES_PREFIX + fileType + ".uri" + KEYWORD_SUFFIX;
            String fileReferenceField = FEATURE_FILES_PREFIX + fileType + REFERENCE_SUFFIX;

            // file cardinality
            String refCardinality = TOTAL_PREFIX + fileType + REF_FILES_COUNT_SUFFIX;
            String notRefCardinality = TOTAL_PREFIX + fileType + NOT_REF_FILES_COUNT_SUFFIX;

            TermQueryBuilder refFilter = QueryBuilders.termQuery(fileReferenceField, true);
            TermQueryBuilder notRefFilter = QueryBuilders.termQuery(fileReferenceField, false);

            FilterAggregationBuilder refCardinalityAgg = AggregationBuilders.filter(refCardinality, refFilter)
                                                                            .subAggregation(AggregationBuilders.cardinality(
                                                                                refCardinality).field(fileUriField));

            FilterAggregationBuilder notRefCardinalityAgg = AggregationBuilders.filter(notRefCardinality, notRefFilter)
                                                                               .subAggregation(AggregationBuilders.cardinality(
                                                                                                                      notRefCardinality)
                                                                                                                  .field(
                                                                                                                      fileUriField));

            builder.aggregation(refCardinalityAgg);
            builder.aggregation(notRefCardinalityAgg);
        }

        // Discriminant distribution aggregator
        TermsAggregationBuilder termsAggBuilder = AggregationBuilders.terms(discriminantProperty)
                                                                     .size(Integer.MAX_VALUE);
        if (isTextMapping(searchKey.getSearchIndex(), discriminantProperty)) {
            termsAggBuilder.field(discriminantProperty + KEYWORD_SUFFIX);
            if (discriminentPropertyInclude.isPresent()) {
                termsAggBuilder.includeExclude(new IncludeExclude(discriminentPropertyInclude.get(), null));
            }
        } else {
            termsAggBuilder.field(discriminantProperty);
        }

        // and "total" aggregations on each asked file types
        for (String fileType : fileTypes) {
            String fileUriField = FEATURE_FILES_PREFIX + fileType + ".uri" + KEYWORD_SUFFIX;
            String fileReferenceField = FEATURE_FILES_PREFIX + fileType + REFERENCE_SUFFIX;

            // file cardinality
            String refCardinality = fileType + REF_FILES_COUNT_SUFFIX;
            String notRefCardinality = fileType + NOT_REF_FILES_COUNT_SUFFIX;

            TermQueryBuilder refFilter = QueryBuilders.termQuery(fileReferenceField, true);
            TermQueryBuilder notRefFilter = QueryBuilders.termQuery(fileReferenceField, false);

            FilterAggregationBuilder refCardinalityAgg = AggregationBuilders.filter(refCardinality, refFilter)
                                                                            .subAggregation(AggregationBuilders.cardinality(
                                                                                refCardinality).field(fileUriField));

            FilterAggregationBuilder notRefCardinalityAgg = AggregationBuilders.filter(notRefCardinality, notRefFilter)
                                                                               .subAggregation(AggregationBuilders.cardinality(
                                                                                                                      notRefCardinality)
                                                                                                                  .field(
                                                                                                                      fileUriField));

            termsAggBuilder.subAggregation(refCardinalityAgg);
            termsAggBuilder.subAggregation(notRefCardinalityAgg);
        }
        builder.aggregation(termsAggBuilder);
    }

    /**
     * Return given index if present or retrieve index from RunTimeTenantResolver
     *
     * @return String index
     * @throws RsRuntimeException if no index is found
     */
    private String getIndex(Optional<String> index) {
        if (index.isPresent()) {
            return index.get();
        } else if (tenantResolver != null && tenantResolver.getTenant() != null) {
            return tenantResolver.getTenant();
        } else {
            throw new RsRuntimeException("Index not defined for elasticsearch request");
        }
    }

    /**
     * Utility class to create a quadruple key used by loading cache mechanism (yes, it's a lazy how to)
     */
    private static class CacheKey
        extends Tuple<Tuple<SearchKey<?, ?>, Map<String, FacetType>>, Tuple<ICriterion, String>> {

        public CacheKey(SearchKey<?, ?> searchKey, ICriterion v2, String v3) {
            super(new Tuple<>(searchKey, new HashMap<>()), new Tuple<>(v2, v3));
        }

        public CacheKey(SearchKey<?, ?> searchKey, ICriterion v2, String v3, Map<String, FacetType> facetsMap) {
            super(new Tuple<>(searchKey, facetsMap), new Tuple<>(v2, v3));
        }

        public SearchKey<?, ?> getSearchKey() {
            return v1().v1();
        }

        public ICriterion getCriterion() {
            return v2().v1();
        }

        public String getSourceAttribute() {
            return v2().v2();
        }

        public Map<String, FacetType> getFacetsMap() {
            return v1().v2();
        }

    }

}
