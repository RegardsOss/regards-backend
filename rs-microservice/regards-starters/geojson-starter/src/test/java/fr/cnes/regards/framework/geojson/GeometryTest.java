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
package fr.cnes.regards.framework.geojson;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import fr.cnes.regards.framework.geojson.coordinates.Positions;
import fr.cnes.regards.framework.geojson.geometry.GeometryCollection;
import fr.cnes.regards.framework.geojson.geometry.IGeometry;
import fr.cnes.regards.framework.geojson.geometry.LineString;
import fr.cnes.regards.framework.geojson.geometry.MultiLineString;
import fr.cnes.regards.framework.geojson.geometry.MultiPoint;
import fr.cnes.regards.framework.geojson.geometry.MultiPolygon;
import fr.cnes.regards.framework.geojson.geometry.Point;
import fr.cnes.regards.framework.geojson.geometry.Polygon;
import fr.cnes.regards.framework.geojson.geometry.Unlocated;

/**
 * Test GeoJson serialization
 *
 * @author Marc Sordi
 *
 */
@RunWith(SpringRunner.class)
@EnableAutoConfiguration
public class GeometryTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(GeometryTest.class);

    private static final String GEOMETRY = "geometry";

    private static final String GEOMETRIES = "geometries";

    private static final String TYPE = "type";

    private static final String COORDINATES = "coordinates";

    @Autowired
    private Gson gson;

    @Test
    public void unlocated() {
        String id = "myId";

        // Write
        Feature<String> feature = new Feature<>();
        feature.setId(id);
        JsonElement jsonElement = gson.toJsonTree(feature);
        Assert.assertTrue(jsonElement.isJsonObject());
        JsonObject jsonObject = jsonElement.getAsJsonObject();
        Assert.assertTrue(jsonObject.get(GEOMETRY).isJsonNull());

        // Read
        Feature<?> readFeature = gson.fromJson(jsonElement, Feature.class);
        Assert.assertTrue(readFeature.getId().equals(id));
        Assert.assertTrue(readFeature.getGeometry() instanceof Unlocated);
    }

    @Test
    public void point() {
        Double longitude = -10.0;
        Double latitude = 20.0;

        // Write
        Feature<String> feature = new Feature<>();
        feature.setGeometry(IGeometry.point(IGeometry.position(longitude, latitude)));

        JsonElement jsonElement = gson.toJsonTree(feature);
        Assert.assertTrue(jsonElement.isJsonObject());
        JsonObject jsonObject = jsonElement.getAsJsonObject();

        // Get geometry
        JsonObject geometryObject = jsonObject.get(GEOMETRY).getAsJsonObject();
        Assert.assertTrue(geometryObject.has(TYPE));
        Assert.assertTrue(geometryObject.get(TYPE).getAsString().equals(GeoJsonType.POINT.getType()));
        Assert.assertTrue(geometryObject.has(COORDINATES));

        // Get coordinates
        JsonElement coordinates = geometryObject.get(COORDINATES);
        Assert.assertTrue(coordinates.isJsonArray());
        Assert.assertTrue(coordinates.getAsJsonArray().size() == 2);

        LOGGER.debug(jsonElement.toString());

        // Read
        Feature<?> readFeature = gson.fromJson(jsonElement, Feature.class);
        Assert.assertTrue(readFeature.getGeometry() instanceof Point);
    }

    @Test
    public void multipoint() {

        // Write
        Feature<String> feature = new Feature<>();

        MultiPoint geometry = IGeometry.multiPoint(IGeometry.position(-170.0, 25.25), IGeometry.position(70.0, 10.10));
        feature.setGeometry(geometry);

        JsonElement jsonElement = gson.toJsonTree(feature);
        Assert.assertTrue(jsonElement.isJsonObject());
        JsonObject jsonObject = jsonElement.getAsJsonObject();

        // Get geometry
        JsonObject geometryObject = jsonObject.get(GEOMETRY).getAsJsonObject();
        Assert.assertTrue(geometryObject.has(TYPE));
        Assert.assertTrue(geometryObject.get(TYPE).getAsString().equals(GeoJsonType.MULTIPOINT.getType()));
        Assert.assertTrue(geometryObject.has(COORDINATES));

        // Get coordinates
        JsonElement coordinates = geometryObject.get(COORDINATES);
        Assert.assertTrue(coordinates.isJsonArray());
        Assert.assertTrue(coordinates.getAsJsonArray().size() == 2);

        LOGGER.debug(jsonElement.toString());

        // Read
        Feature<?> readFeature = gson.fromJson(jsonElement, Feature.class);
        Assert.assertTrue(readFeature.getGeometry() instanceof MultiPoint);
    }

    @Test
    public void linestring() {

        // Write
        Feature<String> feature = new Feature<>();

        Positions lineStringCoordinates = IGeometry.toLineStringCoordinates(IGeometry.position(-170.0, 25.25),
                                                                            IGeometry.position(70.0, 10.10));
        LineString geometry = IGeometry.lineString(lineStringCoordinates);
        feature.setGeometry(geometry);

        JsonElement jsonElement = gson.toJsonTree(feature);
        Assert.assertTrue(jsonElement.isJsonObject());
        JsonObject jsonObject = jsonElement.getAsJsonObject();

        // Get geometry
        JsonObject geometryObject = jsonObject.get(GEOMETRY).getAsJsonObject();
        Assert.assertTrue(geometryObject.has(TYPE));
        Assert.assertTrue(geometryObject.get(TYPE).getAsString().equals(GeoJsonType.LINESTRING.getType()));
        Assert.assertTrue(geometryObject.has(COORDINATES));

        // Get coordinates
        JsonElement coordinates = geometryObject.get(COORDINATES);
        Assert.assertTrue(coordinates.isJsonArray());
        Assert.assertTrue(coordinates.getAsJsonArray().size() == 2);

        LOGGER.debug(jsonElement.toString());

        // Read
        Feature<?> readFeature = gson.fromJson(jsonElement, Feature.class);
        Assert.assertTrue(readFeature.getGeometry() instanceof LineString);
    }

    @Test
    public void multilinestring() {

        // Write
        Feature<String> feature = new Feature<>();

        Positions lineStringCoordinates1 = IGeometry.toLineStringCoordinates(IGeometry.position(-170.0, 25.25),
                                                                             IGeometry.position(70.0, 10.10));

        Positions lineStringCoordinates2 = IGeometry.toLineStringCoordinates(IGeometry.position(-90.0, 90.0),
                                                                             IGeometry.position(33.33, 77.77));

        MultiLineString geometry = IGeometry.multiLineString(lineStringCoordinates1, lineStringCoordinates2);

        feature.setGeometry(geometry);

        JsonElement jsonElement = gson.toJsonTree(feature);
        Assert.assertTrue(jsonElement.isJsonObject());
        JsonObject jsonObject = jsonElement.getAsJsonObject();

        // Get geometry
        JsonObject geometryObject = jsonObject.get(GEOMETRY).getAsJsonObject();
        Assert.assertTrue(geometryObject.has(TYPE));
        Assert.assertTrue(geometryObject.get(TYPE).getAsString().equals(GeoJsonType.MULTILINESTRING.getType()));
        Assert.assertTrue(geometryObject.has(COORDINATES));

        // Get coordinates
        JsonElement coordinates = geometryObject.get(COORDINATES);
        Assert.assertTrue(coordinates.isJsonArray());
        Assert.assertTrue(coordinates.getAsJsonArray().size() == 2);

        LOGGER.debug(jsonElement.toString());

        // Read
        Feature<?> readFeature = gson.fromJson(jsonElement, Feature.class);
        Assert.assertTrue(readFeature.getGeometry() instanceof MultiLineString);
    }

    @Test
    public void polygon() {

        // Write
        Feature<String> feature = new Feature<>();

        Positions exteriorRing = IGeometry
                .toLinearRingCoordinates(IGeometry.position(-170.0, 20.0), IGeometry.position(-170.0, 10.0),
                                         IGeometry.position(-140.0, 10.0), IGeometry.position(-140.0, 20.0),
                                         IGeometry.position(-170.0, 20.0));

        Positions hole = IGeometry
                .toLinearRingCoordinates(IGeometry.position(-160.0, 17.0), IGeometry.position(-150.0, 17.0),
                                         IGeometry.position(-150.0, 13.0), IGeometry.position(-160.0, 13.0),
                                         IGeometry.position(-160.0, 17.0));

        Polygon geometry = IGeometry.polygon(IGeometry.toPolygonCoordinates(exteriorRing, hole));

        feature.setGeometry(geometry);

        JsonElement jsonElement = gson.toJsonTree(feature);
        Assert.assertTrue(jsonElement.isJsonObject());
        JsonObject jsonObject = jsonElement.getAsJsonObject();

        // Get geometry
        JsonObject geometryObject = jsonObject.get(GEOMETRY).getAsJsonObject();
        Assert.assertTrue(geometryObject.has(TYPE));
        Assert.assertTrue(geometryObject.get(TYPE).getAsString().equals(GeoJsonType.POLYGON.getType()));
        Assert.assertTrue(geometryObject.has(COORDINATES));

        // Get coordinates
        JsonElement coordinates = geometryObject.get(COORDINATES);
        Assert.assertTrue(coordinates.isJsonArray());
        Assert.assertTrue(coordinates.getAsJsonArray().size() == 2);

        LOGGER.debug(jsonElement.toString());

        // Read
        Feature<?> readFeature = gson.fromJson(jsonElement, Feature.class);
        Assert.assertTrue(readFeature.getGeometry() instanceof Polygon);
    }

    @Test
    public void multiPolygon() {

        // Write
        Feature<String> feature = new Feature<>();

        Positions exteriorRing = IGeometry
                .toLinearRingCoordinates(IGeometry.position(-170.0, 20.0), IGeometry.position(-170.0, 10.0),
                                         IGeometry.position(-140.0, 10.0), IGeometry.position(-140.0, 20.0),
                                         IGeometry.position(-170.0, 20.0));

        Positions hole = IGeometry
                .toLinearRingCoordinates(IGeometry.position(-160.0, 17.0), IGeometry.position(-150.0, 17.0),
                                         IGeometry.position(-150.0, 13.0), IGeometry.position(-160.0, 13.0),
                                         IGeometry.position(-160.0, 17.0));

        MultiPolygon geometry = IGeometry.multiPolygon(IGeometry.toPolygonCoordinates(exteriorRing, hole),
                                                       IGeometry.toPolygonCoordinates(exteriorRing, hole));

        feature.setGeometry(geometry);

        JsonElement jsonElement = gson.toJsonTree(feature);
        Assert.assertTrue(jsonElement.isJsonObject());
        JsonObject jsonObject = jsonElement.getAsJsonObject();

        // Get geometry
        JsonObject geometryObject = jsonObject.get(GEOMETRY).getAsJsonObject();
        Assert.assertTrue(geometryObject.has(TYPE));
        Assert.assertTrue(geometryObject.get(TYPE).getAsString().equals(GeoJsonType.MULTIPOLYGON.getType()));
        Assert.assertTrue(geometryObject.has(COORDINATES));

        // Get coordinates
        JsonElement coordinates = geometryObject.get(COORDINATES);
        Assert.assertTrue(coordinates.isJsonArray());
        Assert.assertTrue(coordinates.getAsJsonArray().size() == 2);

        LOGGER.debug(jsonElement.toString());

        // Read
        Feature<?> readFeature = gson.fromJson(jsonElement, Feature.class);
        Assert.assertTrue(readFeature.getGeometry() instanceof MultiPolygon);
    }

    @Test
    public void geometryCollection() {
        // Write
        Feature<String> feature = new Feature<>();

        GeometryCollection geometry = IGeometry
                .geometryCollection(IGeometry.point(IGeometry.position(0.0, 10.0)), IGeometry.lineString(IGeometry
                        .toLineStringCoordinates(IGeometry.position(10.0, 13.0), IGeometry.position(25.0, 39.0))));

        feature.setGeometry(geometry);

        JsonElement jsonElement = gson.toJsonTree(feature);
        Assert.assertTrue(jsonElement.isJsonObject());
        JsonObject jsonObject = jsonElement.getAsJsonObject();

        // Get geometry
        JsonObject geometryObject = jsonObject.get(GEOMETRY).getAsJsonObject();
        Assert.assertTrue(geometryObject.has(TYPE));
        Assert.assertTrue(geometryObject.get(TYPE).getAsString().equals(GeoJsonType.GEOMETRY_COLLECTION.getType()));
        Assert.assertTrue(geometryObject.has(GEOMETRIES));

        // Get geometries
        JsonArray geometries = geometryObject.get(GEOMETRIES).getAsJsonArray();
        Assert.assertTrue(geometries.size() == 2);

        // Get first geometry
        geometryObject = geometries.get(0).getAsJsonObject();
        Assert.assertTrue(geometryObject.has(TYPE));
        Assert.assertTrue(geometryObject.get(TYPE).getAsString().equals(GeoJsonType.POINT.getType()));

        // Get second geometry
        geometryObject = geometries.get(1).getAsJsonObject();
        Assert.assertTrue(geometryObject.has(TYPE));
        Assert.assertTrue(geometryObject.get(TYPE).getAsString().equals(GeoJsonType.LINESTRING.getType()));

        LOGGER.debug(jsonElement.toString());

        // Read
        Feature<?> readFeature = gson.fromJson(jsonElement, Feature.class);
        Assert.assertTrue(readFeature.getGeometry() instanceof GeometryCollection);
    }
}
