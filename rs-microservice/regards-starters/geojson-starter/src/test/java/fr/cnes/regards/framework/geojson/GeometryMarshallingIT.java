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
package fr.cnes.regards.framework.geojson;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;
import com.google.common.collect.MapDifference;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import fr.cnes.regards.framework.geojson.geometry.*;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Stephane Cortine
 **/
@RunWith(SpringRunner.class)
@EnableAutoConfiguration
public class GeometryMarshallingIT {

    @Autowired
    private Gson gson;

    @Autowired
    private ObjectMapper objectMapper;

    @Before
    public void setUp() throws Exception {
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_ABSENT);
        objectMapper.registerModule(new SimpleModule() {

            @Override
            public void setupModule(SetupContext context) {
                super.setupModule(context);
                context.addBeanSerializerModifier(new BeanSerializerModifier() {

                    @Override
                    public JsonSerializer<?> modifySerializer(SerializationConfig config, BeanDescription desc,
                            JsonSerializer<?> serializer) {
                        if (IGeometry.class.isAssignableFrom(desc.getBeanClass())) {
                            return new GeometryCustomSerializer((JsonSerializer<Object>) serializer);
                        }
                        return serializer;
                    }
                });
            }
        });
    }

    @Test
    public void unlocated() throws JsonProcessingException {
        // Given
        String id = "myId";

        Feature feature = new Feature();
        feature.setId(id);

        // When, then
        checkMarshalling(feature);
    }

    @Test
    public void point() throws JsonProcessingException {
        checkMarshallingGeometry(GeometryFactory.createPoint());
    }

    @Test
    public void multipoint() throws JsonProcessingException {
        checkMarshallingGeometry(GeometryFactory.createMultiPoint());
    }

    @Test
    public void linestring() throws JsonProcessingException {
        checkMarshallingGeometry(GeometryFactory.createLineString());
    }

    @Test
    public void multilinestring() throws JsonProcessingException {
        checkMarshallingGeometry(GeometryFactory.createMultiLineString());
    }

    @Test
    public void polygon() throws JsonProcessingException {
        checkMarshallingGeometry(GeometryFactory.createPolygon());
    }

    @Test
    public void multiPolygon() throws JsonProcessingException {
        checkMarshallingGeometry(GeometryFactory.createMultiPolygon());
    }

    @Test
    public void geometryCollection() throws JsonProcessingException {
        checkMarshallingGeometry(GeometryFactory.createGeometryCollection());
    }

    private void checkMarshallingGeometry(IGeometry geometry) throws JsonProcessingException {
        // Given
        Feature feature = new Feature();
        feature.setGeometry(geometry);

        // use IGeometry Jackson Deserialization
        objectMapper.registerModule(new SimpleModule().addDeserializer(IGeometry.class, new GeometryDeserializer()));

        // When, then
        checkMarshalling(feature);
    }

    private void checkMarshalling(Feature feature) throws JsonProcessingException {
        // When
        // Serialize Gson -> deserialize Jackson
        String featureStringGson = gson.toJson(feature);

        Feature featureJackson = objectMapper.readValue(featureStringGson, Feature.class);

        // Serialize Jackson -> deserialize Gson
        String featureStringJackson = objectMapper.writeValueAsString(feature);

        Feature featureGson = gson.fromJson(featureStringJackson, Feature.class);

        Map<String, Object> mapGson = gson.fromJson(featureStringGson, new TypeToken<Map<String, Object>>() {

        }.getType());
        Map<String, Object> mapJackson = objectMapper.readValue(featureStringJackson,
                                                                new TypeReference<Map<String, Object>>() {

                                                                });

        MapDifference<String, Object> diffMap = Maps.difference(mapGson, mapJackson);
        Map<String, MapDifference.ValueDifference<Object>> entriesDiffering = diffMap.entriesDiffering();

        // Then
        Assert.assertEquals(feature, featureGson);
        Assert.assertEquals(feature, featureJackson);

        Assert.assertTrue("The provided JSONs are different (difference number=" + entriesDiffering.size() + ")",
                          diffMap.areEqual());
    }

    /**
     * Custom serialzer for Jackson
     */
    static class GeometryCustomSerializer extends JsonSerializer<IGeometry> {

        private final JsonSerializer<Object> defaultSerializer;

        public GeometryCustomSerializer(JsonSerializer<Object> serializer) {
            defaultSerializer = serializer;
        }

        @Override
        public void serialize(IGeometry geometry, JsonGenerator jsonGenerator, SerializerProvider serializerProvider)
                throws IOException {
            if (geometry.getType() == GeoJsonType.UNLOCATED) {
                jsonGenerator.writeObject(null);
            } else {
                defaultSerializer.serialize(geometry, jsonGenerator, serializerProvider);
            }
        }
    }

    /**
     * Custom deserialzer for Jackson
     */
    static class GeometryDeserializer extends StdDeserializer<IGeometry> {

        private final Map<String, Class> map = new HashMap<String, Class>() {{
            put(GeoJsonType.POINT, Point.class);
            put(GeoJsonType.MULTIPOINT, MultiPoint.class);
            put(GeoJsonType.LINESTRING, LineString.class);
            put(GeoJsonType.MULTILINESTRING, MultiLineString.class);
            put(GeoJsonType.POLYGON, Polygon.class);
            put(GeoJsonType.MULTIPOLYGON, MultiPolygon.class);
            put(GeoJsonType.GEOMETRY_COLLECTION, GeometryCollection.class);
        }};

        public GeometryDeserializer() {
            super(IGeometry.class);
        }

        public IGeometry deserialize(JsonParser jsonParser, DeserializationContext context) throws IOException {
            JsonNode node = jsonParser.readValueAsTree();

            return (IGeometry) jsonParser.readValueAs(map.get(node.get("type").textValue()));
        }
    }

}
