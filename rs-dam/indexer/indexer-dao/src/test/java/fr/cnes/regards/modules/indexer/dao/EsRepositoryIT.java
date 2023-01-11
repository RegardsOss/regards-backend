package fr.cnes.regards.modules.indexer.dao;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.*;
import com.google.common.reflect.TypeParameter;
import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import de.svenjacobs.loremipsum.LoremIpsum;
import fr.cnes.regards.framework.geojson.AbstractFeature;
import fr.cnes.regards.framework.geojson.coordinates.Position;
import fr.cnes.regards.framework.geojson.geometry.IGeometry;
import fr.cnes.regards.framework.geojson.geometry.Point;
import fr.cnes.regards.framework.gson.adapters.OffsetDateTimeAdapter;
import fr.cnes.regards.framework.gson.adapters.PolymorphicTypeAdapterFactory;
import fr.cnes.regards.framework.oais.urn.OAISIdentifier;
import fr.cnes.regards.framework.urn.EntityType;
import fr.cnes.regards.framework.urn.UniformResourceName;
import fr.cnes.regards.modules.dam.domain.entities.DataObject;
import fr.cnes.regards.modules.indexer.dao.builder.AggregationBuilderFacetTypeVisitor;
import fr.cnes.regards.modules.indexer.dao.deser.GsonDeserializeIIndexableStrategy;
import fr.cnes.regards.modules.indexer.dao.mapping.utils.AttrDescToJsonMapping;
import fr.cnes.regards.modules.indexer.dao.spatial.GeoHelper;
import fr.cnes.regards.modules.indexer.domain.IIndexable;
import fr.cnes.regards.modules.indexer.domain.SearchKey;
import fr.cnes.regards.modules.indexer.domain.SimpleSearchKey;
import fr.cnes.regards.modules.indexer.domain.aggregation.QueryableAttribute;
import fr.cnes.regards.modules.indexer.domain.criterion.ICriterion;
import fr.cnes.regards.modules.indexer.domain.criterion.StringMatchType;
import fr.cnes.regards.modules.indexer.domain.facet.FacetType;
import fr.cnes.regards.modules.model.domain.Model;
import org.elasticsearch.client.transport.NoNodeAvailableException;
import org.elasticsearch.common.geo.GeoPoint;
import org.elasticsearch.index.IndexNotFoundException;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.metrics.ParsedGeoBounds;
import org.elasticsearch.search.aggregations.metrics.ParsedStats;
import org.junit.*;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.UnknownHostException;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * EsRepository test
 *
 * @author oroussel
 */
@RunWith(SpringRunner.class)
@TestPropertySource("classpath:test.properties")
@ActiveProfiles("test")
public class EsRepositoryIT {

    private static class ItemAdapterFactory extends PolymorphicTypeAdapterFactory<IIndexable> {

        protected ItemAdapterFactory() {
            super(IIndexable.class, "type");
            registerSubtype(Item.class, TYPE);
            registerSubtype(ItemGeo.class, EntityType.DATA, true);
        }

    }

    private static class UrnAdpater extends TypeAdapter<UniformResourceName> {

        @Override
        public UniformResourceName read(JsonReader in) throws IOException {
            if (in.peek() == JsonToken.NULL) {
                in.nextNull();
                return null;
            }
            return UniformResourceName.fromString(in.nextString());
        }

        @Override
        public void write(JsonWriter out, UniformResourceName value) throws IOException {
            if (value != null) {
                out.value(value.toString());
            } else {
                out.nullValue();
            }
        }
    }

    private static class IGeometryAdapter extends TypeAdapter<IGeometry> {

        @Override
        public void write(JsonWriter out, IGeometry value) throws IOException {
            out.beginObject();
            if (value.getClass() == Point.class) {
                Position coordinates = ((Point) value).getCoordinates();
                out.name("coordinates");
                out.beginArray();
                // Write longitude
                out.value(coordinates.getLongitude());
                // Write latitude
                out.value(coordinates.getLatitude());
                // Optionally write altitude
                if (coordinates.getAltitude().isPresent()) {
                    out.value(coordinates.getAltitude().get());
                }
                out.endArray();
                out.name("type");
                out.value("POINT");
            } else {

            }
            out.endObject();
        }

        @Override
        public IGeometry read(JsonReader in) throws IOException {
            Point parsed = new Point();

            in.beginObject();
            in.nextName();
            in.beginArray();
            // Read longitude
            double longitude = in.nextDouble();
            // Read latitude
            double latitude = in.nextDouble();
            // Optionally read altitude
            if (in.peek().equals(JsonToken.NUMBER)) {
                double altitude = in.nextDouble();
                parsed.setCoordinates(new Position(longitude, latitude, altitude));
            } else {
                parsed.setCoordinates(new Position(longitude, latitude));
            }
            in.endArray();

            in.nextName();
            in.nextString();
            in.endObject();

            return parsed;
        }
    }

    private static class MultimapAdapter
        implements JsonDeserializer<Multimap<String, ?>>, JsonSerializer<Multimap<String, ?>> {

        @Override
        public Multimap<String, ?> deserialize(JsonElement json, Type type, JsonDeserializationContext context)
            throws JsonParseException {
            final HashMultimap<String, Object> result = HashMultimap.create();
            final Map<String, Collection<?>> map = context.deserialize(json, multimapTypeToMapType(type));
            for (final Map.Entry<String, ?> e : map.entrySet()) {
                final Collection<?> value = (Collection<?>) e.getValue();
                result.putAll(e.getKey(), value);
            }
            return result;
        }

        @Override
        public JsonElement serialize(Multimap<String, ?> src, Type type, JsonSerializationContext context) {
            final Map<?, ?> map = src.asMap();
            return context.serialize(map);
        }

        private <KK, V> Type multimapTypeToMapType(Type type) {
            final Type[] typeArguments = ((ParameterizedType) type).getActualTypeArguments();
            assert typeArguments.length == 2;
            @SuppressWarnings({ "unchecked", "serial" })
            final com.google.common.reflect.TypeToken<Map<KK, Collection<V>>> mapTypeToken = new com.google.common.reflect.TypeToken<Map<KK, Collection<V>>>() {

            }.where(new TypeParameter<KK>() {

             }, (com.google.common.reflect.TypeToken<KK>) com.google.common.reflect.TypeToken.of(typeArguments[0]))
             .where(new TypeParameter<V>() {

             }, (com.google.common.reflect.TypeToken<V>) com.google.common.reflect.TypeToken.of(typeArguments[1]));
            return mapTypeToken.getType();
        }

    }

    public static class FeatureTypeAdapterFactory implements TypeAdapterFactory {

        @Override
        public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {

            // This factory is only useful for Feature
            Class<? super T> requestedType = type.getRawType();
            if (!AbstractFeature.class.isAssignableFrom(requestedType)) {
                return null;
            }

            final TypeAdapter<T> delegate = gson.getDelegateAdapter(this, type);

            return new TypeAdapter<T>() {

                @Override
                public void write(JsonWriter out, T value) throws IOException {
                    delegate.write(out, value);
                }

                @SuppressWarnings("unchecked")
                @Override
                public T read(JsonReader in) throws IOException {
                    @SuppressWarnings("rawtypes") AbstractFeature feature = (AbstractFeature) delegate.read(in);
                    // Set feature unlocated if geometry is null
                    if (feature.getGeometry() == null) {
                        feature.setGeometry(IGeometry.unlocated());
                    }
                    if (feature.getNormalizedGeometry() == null) {
                        feature.setNormalizedGeometry(IGeometry.unlocated());
                    }
                    return (T) feature;
                }
            };
        }

    }

    /**
     * Item class
     */
    // CHECKSTYLE:OFF
    private static class Item implements Serializable, IIndexable {

        private final String type = TYPE;

        private String id;

        private String name;

        private List<String> groups;

        private Item subItem;

        private int height;

        private double price;

        public Item() {
        }

        public Item(String id, String... groups) {
            this.id = id;
            this.groups = Lists.newArrayList(groups);
        }

        @SuppressWarnings("unused")
        public Item(String id, String name, int height, double price, String... groups) {
            this(id, groups);
            this.name = name;
            this.height = height;
            this.price = price;
        }

        public List<String> getGroups() {
            return groups;
        }

        @SuppressWarnings("unused")
        public void setGroups(List<String> pGroups) {
            groups = pGroups;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        @Override
        public String getLabel() {
            return name;
        }

        public Item getSubItem() {
            return subItem;
        }

        @SuppressWarnings("unused")
        public void setSubItem(Item subItem) {
            this.subItem = subItem;
        }

        @SuppressWarnings("unused")
        public int getHeight() {
            return height;
        }

        public void setHeight(int height) {
            this.height = height;
        }

        @SuppressWarnings("unused")
        public double getPrice() {
            return price;
        }

        public void setPrice(double price) {
            this.price = price;
        }

        @JsonProperty("id")
        @Override
        public String getDocId() {
            return id;
        }

        @Override
        public String getType() {
            return type;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = (prime * result) + ((id == null) ? 0 : id.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            Item other = (Item) obj;
            if (id == null) {
                if (other.id != null) {
                    return false;
                }
            } else if (!id.equals(other.id)) {
                return false;
            }
            return true;
        }

    }

    private static class ItemGeo extends DataObject {

        private final String type = TYPEDATAOBJECT;

        public ItemGeo(Model model, String tenant, String providerId, String label) {
            super(model, tenant, providerId, label);
        }

    }

    private static final String TYPE = "item";

    private static final String TYPEGEO = "itemgeo";

    private static final String TYPEDATAOBJECT = EntityType.DATA.toString();

    /**
     * Class to test
     */
    private static IEsRepository repository;

    /**
     * JSON mapper
     */
    private static Gson gson;

    @Value("${regards.elasticsearch.host}")
    private String elasticHost;

    @Value("${regards.elasticsearch.http.port}")
    private int elasticPort;

    @Value("${regards.elasticsearch.http.protocol:http}")
    private String elasticProtocol;

    /**
     * Befor class setting up method
     *
     * @throws Exception exception
     */
    @Before
    public void setUp() throws Exception {
        boolean repositoryOK = true;
        // we get the properties into target/test-classes because this is where maven will put the filtered file(with real values and not placeholder)
        try {
            gson = new GsonBuilder().disableInnerClassSerialization()
                                    .registerTypeAdapterFactory(new ItemAdapterFactory())
                                    .registerTypeAdapterFactory(new FeatureTypeAdapterFactory())
                                    .registerTypeAdapter(UniformResourceName.class, new UrnAdpater())
                                    .registerTypeAdapter(IGeometry.class, new IGeometryAdapter())
                                    .registerTypeAdapter(OffsetDateTime.class, new OffsetDateTimeAdapter().nullSafe())
                                    .registerTypeHierarchyAdapter(Multimap.class, new MultimapAdapter())
                                    .create();

            repository = new EsRepository(gson,
                                          Collections.emptyList(),
                                          elasticHost,
                                          elasticPort,
                                          elasticProtocol,
                                          null,
                                          null,
                                          0,
                                          new GsonDeserializeIIndexableStrategy(gson),
                                          new AggregationBuilderFacetTypeVisitor(10, 1),
                                          new AttrDescToJsonMapping(AttrDescToJsonMapping.RangeAliasStrategy.GTELTE));
        } catch (NoNodeAvailableException e) {
            repositoryOK = false;
        }
        // Do not launch tests is Elasticsearch is not available
        Assume.assumeTrue(repositoryOK);

        final Consumer<String> cleanFct = (pIndex) -> {
            try {
                repository.deleteIndex(pIndex);
            } catch (final IndexNotFoundException infe) {
            }
        };
        // All created indices from tests
        cleanFct.accept("test");
        cleanFct.accept("titi");
        cleanFct.accept("tutu");
        cleanFct.accept("items");
        cleanFct.accept("mergeditems");
        cleanFct.accept("bulktest");
        cleanFct.accept("loading");
    }

    @After
    public void tearDown() throws Exception {
        if (repository != null) {
            repository.close();
        }
    }

    @Test
    public void testCreateDeleteIndex() throws UnknownHostException {
        Assert.assertTrue(repository.createIndex("test"));
        Assert.assertTrue(repository.deleteIndex("test"));
    }

    @Test
    public void testCreateIndexWithSpecialMappings() {
        Assert.assertTrue(repository.createIndex("test"));
        String[] types = { "pipo", "bimbo" };
    }

    @Test
    public void testIndexExists() {
        Assert.assertTrue(repository.createIndex("tutu"));
        Assert.assertTrue(repository.indexExists("tutu"));
        Assert.assertFalse(repository.indexExists("sqdkljhskjhdfkjhsdfkjhskdjfksjdhfkjhksdjhfksjdfgsjhfgsjdhgfjshdf"));
    }

    /**
     * Test save, get delete
     */
    @Test
    public void testSaveGetDelete() {
        repository.createIndex("items");
        // Creations for first two
        final Item item1 = new Item("1", "group1", "group2", "group3");
        Assert.assertTrue(repository.save("items", item1));
        Assert.assertTrue(repository.save("items", new Item("2", "group1", "group3")));
        // Update
        final Item item2 = new Item("2", "group4", "group5");
        Assert.assertFalse(repository.save("items", item2));

        // Get
        final Item item1FromIndex = repository.get(Optional.of("items"), "item", "1", Item.class);
        Assert.assertNotNull(item1FromIndex);
        Assert.assertEquals(item1, item1FromIndex);

        // Get an inexistant item
        Assert.assertNull(repository.get(Optional.of("items"), "item", "3", Item.class));

        // Save and get an empty item
        try {
            repository.save("items", new Item());
            Assert.fail("docId and type not provided, this should have thrown an IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }

        // Testing get method with an Item as parameter (instead of id and type)
        Item toFindItem = new Item("1");
        toFindItem = repository.get(Optional.of("items"), toFindItem);

    }

    /**
     *
     */
    @Test
    public void testBulkSave() {
        // Twice same first item (=> create then update) plus an empty item
        final Item item1 = new Item("1", "group1", "group2", "group3");
        final Item item2 = new Item("1", "group1", "group2", "group3");
        final List<Item> list = new ArrayList<>();
        list.add(item1);
        list.add(item2);
        list.add(new Item());
        try {
            repository.saveBulk("bulktest", list);
            Assert.fail("saveBulk should have thrown an IllegalArgumentException (last item does not provide id nor "
                        + "type ");
        } catch (IllegalArgumentException e) {
        }

        // remove failed item
        list.remove(2);
        BulkSaveResult bulkSaveResult = repository.saveBulk("bulktest", list);
        Assert.assertEquals(1, bulkSaveResult.getSavedDocsCount());

        // If someone could find a case when a document save failed...Don't hesitate to talk it to me
    }

    // @Test
    public void testLoad() {
        loadItemsBulk(100_000);
    }

    /**
     * Load generated data into Elsaticsearch
     *
     * @param pCount number of documents to insert
     */
    private void loadItemsBulk(int pCount) {
        repository.createIndex("loading");

        final LoremIpsum loremIpsum = new LoremIpsum();
        final String[] words = loremIpsum.getWords(100).split(" ");

        final List<Item> items = new ArrayList<>();
        for (int i = 0; i < pCount; i++) {
            final Item item = new Item(Integer.toString(i),
                                       Stream.generate(() -> words[(int) (Math.random() * words.length)])
                                             .limit((int) (Math.random() * 10))
                                             .collect(Collectors.toSet())
                                             .toArray(new String[0]));
            item.setName(words[(int) (Math.random() * words.length)]);
            item.setHeight((int) (Math.random() * 1000));
            item.setPrice(Math.random() * 10000.);
            items.add(item);
            if ((i % 10_000) == 0) {
                final long start = System.currentTimeMillis();
                repository.saveBulk("loading", items);
                System.out.println("Loading (10 000 items): " + (System.currentTimeMillis() - start) + " ms");
                items.clear();
            }
        }
        if (items.size() > 0) {
            final long start = System.currentTimeMillis();
            repository.saveBulk("loading", items);
            System.out.println("Loading (" + items.size() + " items): " + (System.currentTimeMillis() - start) + " ms");
        }
    }

    @Test
    public void testGetAggregateOnNumericValues() throws IOException {
        String itemsIndexName = "items";
        repository.createIndex(itemsIndexName);
        // Creations for first two
        final Item item1 = new Item("1", "toto", 10, 10000d, "group1", "group2", "group3");
        repository.save(itemsIndexName, item1);
        Item item2 = new Item("2", "titi", 10, 20000d, "group1", "group3");
        repository.save(itemsIndexName, item2);

        Map<String, QueryableAttribute> qas = Maps.newHashMap();
        qas.put("price", new QueryableAttribute("price", null, false, 10, false));

        repository.refresh(itemsIndexName);

        //        ICriterion all = ICriterion.all();
        ICriterion all = ICriterion.contains("groups", "group1", StringMatchType.KEYWORD);
        SearchKey<Item, Item> searchKey = new SearchKey<>(TYPE, Item.class);
        searchKey.setSearchIndex(itemsIndexName);
        Aggregations aggregations = repository.getAggregations(searchKey,
                                                               all,
                                                               (Collection<QueryableAttribute>) qas.values());
        Assert.assertEquals(20000d, ((ParsedStats) aggregations.asList().get(0)).getMax(), 0.0001d);
        Assert.assertEquals(10000d, ((ParsedStats) aggregations.asList().get(0)).getMin(), 0.0001d);
        repository.deleteIndex(itemsIndexName);

    }

    @Test
    public void testGetAggregatesOnBoundingBox() throws IOException {
        String itemsTenant = "items";
        repository.createIndex(itemsTenant);

        // Creations for first two
        DataObject dataObject1 = new ItemGeo(new Model(), itemsTenant, "provider", "label");
        GeoPoint do1SePoint = new GeoPoint(43.524768, 1.4879276);
        GeoPoint do1NwPoint = new GeoPoint(43.5889203, 1.3747632);
        dataObject1.setSePoint(do1SePoint);
        dataObject1.setNwPoint(do1NwPoint);
        dataObject1.setId(1L);
        dataObject1.setTags(Sets.newHashSet("group1", "group2", "group3"));
        dataObject1.setLabel("toto");
        dataObject1.setIpId(UniformResourceName.build(OAISIdentifier.AIP.name(),
                                                      EntityType.DATA,
                                                      itemsTenant,
                                                      UUID.fromString("74f2c965-0136-47f0-93e1-4fd098db701c"),
                                                      1,
                                                      null,
                                                      null));
        Point point = IGeometry.point(1.3747632, 43.524768);
        dataObject1.setWgs84(GeoHelper.normalize(point));
        dataObject1.setNormalizedGeometry(GeoHelper.normalize(point));
        dataObject1.getFeature().setGeometry(GeoHelper.normalize(point));
        dataObject1.getFeature().setNormalizedGeometry(GeoHelper.normalize(point));

        DataObject dataObject2 = new ItemGeo(new Model(), itemsTenant, "provider", "label");
        GeoPoint do2SePoint = new GeoPoint(43.4461681, -0.0369283);
        GeoPoint do2NwPoint = new GeoPoint(43.7695852, -0.5334374);
        dataObject2.setId(2L);
        dataObject1.setIpId(UniformResourceName.build(OAISIdentifier.AIP.name(),
                                                      EntityType.DATA,
                                                      itemsTenant,
                                                      UUID.fromString("74f2c965-0136-47f0-93e1-4fd098db1234"),
                                                      1,
                                                      null,
                                                      null));
        dataObject2.setSePoint(do2SePoint);
        dataObject2.setNwPoint(do2NwPoint);
        dataObject2.setTags(Sets.newHashSet("group1"));
        dataObject2.setLabel("titi");
        Point point2 = IGeometry.point(-0.0369283, 43.7695852);
        dataObject2.setWgs84(GeoHelper.normalize(point2));
        dataObject2.setNormalizedGeometry(GeoHelper.normalize(point2));
        dataObject2.getFeature().setGeometry(GeoHelper.normalize(point2));
        dataObject2.getFeature().setNormalizedGeometry(GeoHelper.normalize(point));

        repository.save(itemsTenant, dataObject1);
        repository.save(itemsTenant, dataObject2);

        Map<String, QueryableAttribute> qas = Maps.newHashMap();
        qas.put("sePoint", new QueryableAttribute("sePoint", null, false, 10, false, true));
        qas.put("nwPoint", new QueryableAttribute("nwPoint", null, false, 10, false, true));

        ICriterion all = ICriterion.contains("tags", "group1", StringMatchType.KEYWORD);
        repository.refresh(itemsTenant);
        Assert.assertTrue(repository.indexExists(itemsTenant));

        SearchKey<ItemGeo, ItemGeo> searchKey = new SearchKey<>(TYPEDATAOBJECT, ItemGeo.class);
        searchKey.setSearchIndex(itemsTenant);
        Aggregations aggregations = repository.getAggregations(searchKey,
                                                               all,
                                                               (Collection<QueryableAttribute>) qas.values());
        //We only check extrem NW and SE values
        Assert.assertEquals(43.7695, ((ParsedGeoBounds) aggregations.asList().get(0)).topLeft().lat(), 0.0001d);
        Assert.assertEquals(-0.5334, ((ParsedGeoBounds) aggregations.asList().get(0)).topLeft().lon(), 0.0001d);
        Assert.assertEquals(43.4461, ((ParsedGeoBounds) aggregations.asList().get(1)).bottomRight().lat(), 0.0001d);
        Assert.assertEquals(1.4879276, ((ParsedGeoBounds) aggregations.asList().get(1)).bottomRight().lon(), 0.0001d);
        repository.deleteIndex(itemsTenant);
    }

    @Test
    public void testEmpty() {
        String index = "toto";
        if (repository.indexExists(index)) {
            repository.deleteAll(index);
        } else {
            repository.createIndex(index);
        }

        Map<String, FacetType> facetMap = new HashMap<>();
        facetMap.put("titi", FacetType.DATE);
        facetMap.put("tutu", FacetType.NUMERIC);
        facetMap.put("tata", FacetType.STRING);
        SimpleSearchKey<IIndexable> searchKey = new SimpleSearchKey<>("toto", IIndexable.class);
        searchKey.setSearchIndex(index);
        FacetPage<IIndexable> page = (FacetPage<IIndexable>) repository.search(searchKey,
                                                                               10,
                                                                               ICriterion.all(),
                                                                               facetMap);
        Assert.assertNotNull(page.getFacets());
        Assert.assertTrue(page.getFacets().isEmpty());
    }
    // CHECKSTYLE:ON
}
