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
package fr.cnes.regards.framework.utils;

import fr.cnes.regards.framework.geojson.coordinates.PolygonPositions;
import fr.cnes.regards.framework.geojson.coordinates.Position;
import fr.cnes.regards.framework.geojson.coordinates.Positions;
import fr.cnes.regards.framework.geojson.geometry.IGeometry;
import nl.pdok.gml3.GMLParser;
import nl.pdok.gml3.exceptions.GML3ParseException;
import nl.pdok.gml3.impl.gml3_2_1.GML321GeotoolsParserImpl;
import org.apache.commons.lang3.StringUtils;
import org.locationtech.jts.geom.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/**
 * Util class in order to parse GML tag to IGeometry REGARDS (Point, LineString, Polygon).
 * <p>
 * Converts GML to JTS : for now GML 3.1.1.2 and 3.2.1 are supported (https://github.com/PDOK/gml3-jts)
 *
 * @author Stephane Cortine
 **/
public class GeometryConverterUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(GeometryConverterUtils.class);

    private final Map<String, Function<Geometry, IGeometry>> converters = new HashMap<>();

    public int pointSampling;

    public GeometryConverterUtils(int pointSampling) {
        this.pointSampling = pointSampling;

        converters.put(Geometry.TYPENAME_POINT, this::createPoint);
        converters.put(Geometry.TYPENAME_LINESTRING, this::createLineString);
        converters.put(Geometry.TYPENAME_POLYGON, this::createPolygon);
    }

    private IGeometry convert(Geometry geometry) {
        Objects.requireNonNull(geometry);

        Function<Geometry, IGeometry> createFunction = converters.get(geometry.getGeometryType());
        if (createFunction == null) {
            LOGGER.debug("Unrecognized geometry, available geometries: " + converters.keySet());
            return null;
        }
        return createFunction.apply(geometry);
    }

    private IGeometry createPolygon(Geometry geometry) {
        Polygon jtsPolygon = (Polygon) geometry;
        return createPolygon(jtsPolygon);
    }

    private IGeometry createLineString(Geometry geometry) {
        LineString jtsLineString = (LineString) geometry;
        return createLineString(jtsLineString);
    }

    private IGeometry createPoint(Geometry geometry) {
        Point jtsPoint = (Point) geometry;
        return createPoint(jtsPoint);
    }

    public IGeometry convert(Node node) {
        // Create the GML3.2.1 parser
        GMLParser parser = new GML321GeotoolsParserImpl();
        // XML code from node
        String partXmlCode = nodeXmlToString(node);
        if (StringUtils.isBlank(partXmlCode)) {
            return null;
        }
        LOGGER.debug("Parser xml code fom node: " + partXmlCode);
        try {
            return convert(parser.toJTSGeometry(partXmlCode));
        } catch (GML3ParseException e) {
            LOGGER.error(e.getMessage());
            return null;
        }
    }

    private String nodeXmlToString(Node node) {
        StringWriter stringWriter = new StringWriter();
        Transformer xform = null;
        try {
            xform = TransformerFactory.newInstance().newTransformer();

            xform.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            xform.transform(new DOMSource(node), new StreamResult(stringWriter));

            return (stringWriter.toString());
        } catch (TransformerException e) {
            LOGGER.error(e.getMessage());
            return "";
        }
    }

    private IGeometry createPolygon(Polygon jtsPolygon) {
        Objects.requireNonNull(jtsPolygon);
        LOGGER.debug("Transform jts Polygon -> regards Polygon");

        PolygonPositions polygonPositions = new PolygonPositions();
        Positions exteriorRing = Positions.fromArray(transformLineStringToArray(jtsPolygon.getExteriorRing()));
        polygonPositions.add(exteriorRing);
        // Holes
        for (int index = 0; index < jtsPolygon.getNumInteriorRing(); index++) {
            Positions holes = Positions.fromArray(transformLineStringToArray(jtsPolygon.getInteriorRingN(index)));

            polygonPositions.add(holes);
        }

        fr.cnes.regards.framework.geojson.geometry.Polygon regardsPolygon = IGeometry.polygon(polygonPositions);
        LOGGER.debug("Regards Polygon: " + regardsPolygon);
        return regardsPolygon;
    }

    private IGeometry createLineString(LineString jtsLineString) {
        Objects.requireNonNull(jtsLineString);
        LOGGER.debug("Transform jts LineString -> regards LineString");
        fr.cnes.regards.framework.geojson.geometry.LineString regardsLineString = IGeometry.lineString(Positions.fromArray(
            transformLineStringToArray(jtsLineString)));

        LOGGER.debug("Regards LineString: " + regardsLineString);
        return regardsLineString;
    }

    private IGeometry createPoint(Point jtsPoint) {
        Objects.requireNonNull(jtsPoint);
        LOGGER.debug("Transform jts Point -> regards Point");
        fr.cnes.regards.framework.geojson.geometry.Point regardsPoint = IGeometry.point(Position.fromArray(
            transformPointToArray(jtsPoint)));

        LOGGER.debug("Regards Point: " + regardsPoint);
        return regardsPoint;
    }

    private double[] transformPointToArray(Point point) {
        return transformCoordinateToArray(point.getCoordinates()[0]);
    }

    private double[][] transformLineStringToArray(LineString lineString) {
        double[][] positions;

        int nbCoordinates = lineString.getCoordinates().length;
        // Need point sampling ?
        if (pointSampling == 0 || nbCoordinates <= pointSampling) {
            positions = new double[lineString.getCoordinates().length][2];
            for (int index = 0; index < nbCoordinates; index++) {
                positions[index] = transformCoordinateToArray(lineString.getCoordinates()[index]);
            }
        } else {
            positions = new double[pointSampling][2];
            int step = 0, cntStep = 0;
            for (int index = 0; index < nbCoordinates; index += step) {
                positions[cntStep] = transformCoordinateToArray(lineString.getCoordinates()[index]);

                step = ((nbCoordinates - index) / (pointSampling - 1 - cntStep));
                cntStep++;
            }
            positions[pointSampling - 1] = transformCoordinateToArray(lineString.getCoordinates()[nbCoordinates - 1]);
        }
        return positions;
    }

    private double[] transformCoordinateToArray(Coordinate coordinate) {
        // Create with [longitude, latitude]
        double[] positions = new double[2];
        //Longitude
        double longitude = coordinate.getY();
        positions[0] = longitude;
        //Latitude
        double latitude = coordinate.getX();
        positions[1] = latitude;
        //Altitude
        double altitude = coordinate.getZ();
        if (!Double.isNaN(altitude)) {
            // Create with [longitude, latitude, altitude]
            positions = new double[] { longitude, latitude, altitude };
        }
        return positions;
    }
}
