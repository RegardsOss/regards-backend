package fr.cnes.regards.modules.indexer.dao.mapping;

import com.google.gson.*;
import com.thedeanda.lorem.Lorem;
import com.thedeanda.lorem.LoremIpsum;
import fr.cnes.regards.modules.indexer.dao.mapping.model.*;
import fr.cnes.regards.modules.indexer.dao.mapping.utils.AttrDescToJsonMapping;
import fr.cnes.regards.modules.indexer.dao.mapping.utils.JsonConverter;
import fr.cnes.regards.modules.indexer.dao.mapping.utils.JsonMerger;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.collection.HashMap;
import io.vavr.collection.List;
import io.vavr.collection.Map;
import io.vavr.control.Option;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.http.HttpHost;
import org.apache.http.util.EntityUtils;
import org.elasticsearch.ElasticsearchStatusException;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.*;
import org.elasticsearch.common.xcontent.XContentType;
import org.jeasy.random.EasyRandom;
import org.jeasy.random.EasyRandomParameters;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.DockerComposeContainer;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.text.DateFormat;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static fr.cnes.regards.modules.indexer.dao.mapping.utils.AttrDescToJsonMapping.ELASTICSEARCH_MAPPING_PROP_NAME;
import static fr.cnes.regards.modules.model.domain.attributes.restriction.RestrictionType.ENUMERATION;
import static fr.cnes.regards.modules.model.domain.attributes.restriction.RestrictionType.NO_RESTRICTION;
import static fr.cnes.regards.modules.model.dto.properties.PropertyType.*;

public class MappingTest {

    static final Boolean SPEEDUP_NO_GEOSHAPE = true;

    private static final Logger LOGGER = LoggerFactory.getLogger(MappingTest.class);
    private static final Map<String,String> EMPTY = HashMap.empty();
    public static final DateTimeFormatter DATE_TIME_ISO_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

    @ClassRule
    public static DockerComposeContainer environment =
            new DockerComposeContainer(new File("src/test/resources/docker-compose.yml"))
                    //.withExposedService("kibana", 5601)
                    .withExposedService("elasticsearch", 9200);

    //    @Ignore
    @Test
    public void test() throws Exception {
        EasyRandom generator = this.easyRandom();
        Gson gson = this.createGson();
        RestHighLevelClient client = this.client();

        AttrDescToJsonMapping noAliasMapping = new AttrDescToJsonMapping(AttrDescToJsonMapping.RangeAliasStrategy.NO_ALIAS);
        AttrDescToJsonMapping gteLteMapping = new AttrDescToJsonMapping(AttrDescToJsonMapping.RangeAliasStrategy.GTELTE);

        IEsMappingCreationService mappingSrvNoAlias = new EsMappingService(null, this::itemAttrs, noAliasMapping, new JsonMerger());
        IEsMappingCreationService mappingSrvGteLte = new EsMappingService(null, this::itemAttrs, gteLteMapping, new JsonMerger());

        List<Query> rscQueries = List.of(new String[]{
            "sort_001",
            "search_after_001",
            "city_001",
        }).flatMap(Query::fromFile);

        HashMap.of(
            "no-mapping",       null,
            "mapping-no-alias", mappingSrvNoAlias.createMappingForIndex("mapping-no-alias"),
            "mapping-alias",    mappingSrvGteLte.createMappingForIndex("mapping-alias")
        )
        .map((indexName, mapping) -> Tuple.of(indexName, bulkThenQuery(generator, gson, client, indexName, mapping, rscQueries)))
        .forEach(this::logResult);

    }

    private void logResult(Tuple2<String, Option<TimingAnalysis>> object) {
        object._2.peek(ta -> {
            LOGGER.info("INDEX={} | indexing took {}ms", object._1, ta.getIndexingMs());
            ta.getQueryMeanMs().forEach((q, t) -> {
                LOGGER.info("INDEX={} | query {} took {}ms", object._1, q, t);
            });
        });
    }

    private Option<TimingAnalysis> bulkThenQuery(
            EasyRandom generator,
            Gson gson,
            RestHighLevelClient client,
            String indexName,
            JsonObject mapping,
            List<Query> queries
    ) {
        final int bulkSize = 500;
        final int bulkIterations = 40;
        try {
            recreateIndex(client, indexName, mapping);

            long indexingMs = timedBulkIndex(generator, gson, client, indexName, bulkSize, bulkIterations);

            Map<String, Long> queryMeanMs =
                    queries.map(q -> Tuple.of(q, timedQuery(client, indexName, q)))
                    .toMap(ql -> ql._1.getName(), ql -> ql._2);

            return Option.of(new TimingAnalysis(indexingMs, queryMeanMs));
        }
        catch(Exception e) {
            LOGGER.error(e.getMessage(), e);
            return Option.none();
        }
    }


    // ======================
    // INDEX DOCUMENTS

    private long timedBulkIndex(EasyRandom generator, Gson gson, RestHighLevelClient client, String indexName,
            int bulkSize, int bulkIterations) throws IOException {
        StopWatch sw = new StopWatch();
        sw.start();
        for (int i = 0; i < bulkIterations; i++) {
            LOGGER.info("Indexing {} items in {}: iteration {}/{}", bulkSize, indexName, i, bulkIterations);
            BulkRequest req = Stream
                .iterate(0, n -> n + 1)
                .limit(bulkSize)
                .map(n -> generator.nextObject(Item.class))
                //.peek(item -> LOGGER.info(gson.toJson(item)))
                .reduce(new BulkRequest(),
                        (acc, item) -> acc.add(new IndexRequest(indexName, "_doc").source(gson.toJson(item), XContentType.JSON)),
                        (a, b) -> a
                );
            BulkResponse response = client.bulk(req, RequestOptions.DEFAULT);
            if (response.hasFailures()) {
                LOGGER.warn("    Failures: {}", response.buildFailureMessage());
            }
        }
        sw.stop();
        float indexingTime = ((float)sw.getTime(TimeUnit.MILLISECONDS))/1000.f;
        int itemCount = bulkSize * bulkIterations;
        LOGGER.info("=== Indexing {} items in {} took {}s => {}ms/item", itemCount, indexName, indexingTime, (1000f * indexingTime) / itemCount);
        return sw.getTime();
    }


    // ======================
    // RUN QUERIES

    private long timedQuery(RestHighLevelClient client, String indexName, Query query) {
        int took = 0;
        Request request = query.toRequestForIndex(indexName);
        for (int i = 0; i < 10; i++) {
            LOGGER.info(">>> Query request: {} {} ||| {}", request.getMethod(), request.getEndpoint(), query.getEntity().toString());
            try {
                Response response = client.getLowLevelClient().performRequest(request);
                int statusCode = response.getStatusLine().getStatusCode();
                String responseBody = EntityUtils.toString(response.getEntity());
                took += new JsonParser().parse(responseBody).getAsJsonObject().get("took").getAsInt();
                LOGGER.info("<<< Query response: {} ||| {}", statusCode, responseBody);
            } catch (IOException e) {
                LOGGER.error(e.getMessage(), e);
            }
        }
        int meanDuration = took / 10;
        LOGGER.info("=== Querying took a mean of {}ms", meanDuration);
        return meanDuration;

    }

    // ======================
    // INDEX CREATION

    private long recreateIndex(RestHighLevelClient client, String s, JsonObject mapping) throws IOException {
        try {
            client.indices().delete(new DeleteIndexRequest(s), RequestOptions.DEFAULT);
        }
        catch(ElasticsearchStatusException e) {
            // Ignore because the index probably didn't exist.
        }
        CreateIndexRequest req = new CreateIndexRequest(s);
        if (mapping != null) {
            LOGGER.info("    Mapping for {}: {}", s, mapping.toString());
            req = req.mapping("_doc", new JsonConverter().toXContentBuilder(mapping));
        }
        StopWatch sw = new StopWatch();
        sw.start();
        client.indices().create(req, RequestOptions.DEFAULT);
        sw.stop();
        return sw.getTime();
    }

    // ======================
    // ElasticSearch CLIENT

    private RestHighLevelClient client() {
        String host = "localhost"; //environment.getServiceHost("elasticsearch_1", 9200);
        int port = 9200; //environment.getServicePort("elasticsearch_1", 9200);
        HttpHost http = new HttpHost(host, port, "http");
        RestClientBuilder restClientBuilder = RestClient.builder(http)
                .setRequestConfigCallback(requestConfigBuilder -> requestConfigBuilder.setSocketTimeout(1_200_000))
                .setMaxRetryTimeoutMillis(1_200_000);
        return new RestHighLevelClient(restClientBuilder);
    }

    // ======================
    // GSON

    private Gson createGson() {
        return new GsonBuilder().registerTypeAdapter(
                LocalDateTime.class,
                (JsonSerializer<LocalDateTime>)(date, typeOfSrc, context) -> new JsonPrimitive(date.format(DATE_TIME_ISO_FORMATTER)))
                .setDateFormat(DateFormat.FULL).create();
    }

    // ======================
    // Attribute Descriptions

    private Stream<AttributeDescription> itemAttrs() {
        return Stream.of(
                new AttributeDescription("dateRange", DATE_INTERVAL, NO_RESTRICTION, EMPTY.toJavaMap()),

                new AttributeDescription("location.city", STRING, ENUMERATION, EMPTY.toJavaMap()),
                new AttributeDescription("location.country", STRING, ENUMERATION, EMPTY.toJavaMap()),
                new AttributeDescription("location.center", STRING, NO_RESTRICTION, HashMap.of(
                        ELASTICSEARCH_MAPPING_PROP_NAME,
                        "{\"type\":\"geo_point\"}").toJavaMap()),
                new AttributeDescription("location.tile", STRING, ENUMERATION, HashMap.of(
                        ELASTICSEARCH_MAPPING_PROP_NAME,
                        "{\"type\":\"geo_shape\", \"precision\": \"1km\"}").toJavaMap()),

                new AttributeDescription("name", STRING, ENUMERATION, EMPTY.toJavaMap()),
                new AttributeDescription("content", STRING, NO_RESTRICTION, EMPTY.toJavaMap()),
                new AttributeDescription("size", INTEGER, NO_RESTRICTION, EMPTY.toJavaMap()),
                new AttributeDescription("probability", DOUBLE, NO_RESTRICTION, EMPTY.toJavaMap())
        );
    }

    // ======================
    // Random item generation

    interface InClass { Predicate<Field> withName(String name); }
    static <T> InClass inClass(Class<T> type) { return (name -> (field -> field.getName().equals(name) && field.getDeclaringClass().equals(type))); }

    private EasyRandom easyRandom() {
        Lorem lorem = LoremIpsum.getInstance();

        EasyRandomParameters parameters = new EasyRandomParameters();
        EasyRandom generator = new EasyRandom(parameters);
        parameters
                .collectionSizeRange(0,10)
                .randomize(Duration.class, () -> Duration.ofSeconds(generator.nextInt(3600 * 24 * 10)))
                .randomize(LocalDateTime.class, () -> LocalDateTime.now().minusSeconds(generator.nextInt(3600 * 24 * 10)))
                .randomize(DateRange.class, () -> DateRange.starting(generator.nextObject(LocalDateTime.class)).during(generator.nextObject(Duration.class)))
                .randomize(GeoPoint.class, () -> new GeoPoint(generator.nextInt(85), generator.nextInt(85)))
                .randomize(GeoTile.class, () -> {
                    GeoPoint from = generator.nextObject(GeoPoint.class);
                    return new GeoTile(from, from.translate(2,2));
                })
                .randomize(Location.class, () -> {
                    if (SPEEDUP_NO_GEOSHAPE) {
                        return null;
                    }
                    else {
                        GeoTile tile = generator.nextObject(GeoTile.class);
                        return new Location(lorem.getCity(), lorem.getCountry(),
                                            tile.center().translate(generator.nextFloat(), generator.nextFloat()), tile);
                    }
                })
                .randomize(inClass(Item.class).withName("content"), () -> lorem.getWords(2 + generator.nextInt(10)))
                .randomize(inClass(Location.class).withName("city"), lorem::getCity)
                .randomize(inClass(Location.class).withName("country"), lorem::getCountry)
        ;
        return generator;
    }

}
