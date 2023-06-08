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
package fr.cnes.regards.modules.opensearch.service.parser;

import fr.cnes.regards.modules.indexer.domain.criterion.ICriterion;
import fr.cnes.regards.modules.indexer.domain.criterion.exception.InvalidGeometryException;
import fr.cnes.regards.modules.opensearch.service.converter.PolygonToArray;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.convert.converter.Converter;

/**
 * Creates {@link ICriterion} from multiple geometry types.
 *
 * @author Sébastien Binda
 */
public final class GeometryCriterionBuilder {

    /**
     * Class logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(GeometryCriterionBuilder.class);

    private GeometryCriterionBuilder() {
    }

    /**
     * Read WKT format in given String to creates a Geometry criterion.
     * Only Polygons are handled here.
     *
     * @param wktGeometry geometry in WKT format
     */
    public static ICriterion build(String wktGeometry) throws InvalidGeometryException {
        try {
            WKTReader wkt = new WKTReader();
            Geometry geometry = wkt.read(wktGeometry);

            if (geometry instanceof Polygon polygon) {
                if (!geometry.isValid()) {
                    throw new InvalidGeometryException("The passed WKT string contains an invalid POLYGON");
                }
                Converter<Polygon, double[][][]> converter = new PolygonToArray();
                double[][][] coordinates = converter.convert(polygon);
                return ICriterion.intersectsPolygon(coordinates);
            } else {
                // Only Polygons are handled for now
                throw new InvalidGeometryException("The passed WKT string does not reference a polygon");
            }
        } catch (ParseException | IllegalArgumentException e) {
            LOGGER.error("Geometry parsing error", e);
            throw new InvalidGeometryException(e.getMessage(), e);
        }
    }

    /**
     * Creates a circle geometry criterion from longitude, latitude and radius.
     *
     * @return {@link ICriterion}
     */
    public static ICriterion build(String lonParam, String latParam, String radiusParam)
            throws InvalidGeometryException {
        // Check required query parameter
        if (latParam == null && lonParam == null && radiusParam == null) {
            return null;
        }

        if (latParam == null) {
            throw new InvalidGeometryException("Missing center latitude parameter to create circle geometry");
        }

        if (lonParam == null) {
            throw new InvalidGeometryException("Missing center longitude parameter to create circle geometry");
        }

        if (radiusParam == null) {
            throw new InvalidGeometryException("Missing radius parameter to create circle geometry");
        }

        double[] center = {Double.parseDouble(lonParam), Double.parseDouble(latParam)};
        return ICriterion.intersectsCircle(center, radiusParam);
    }

    public static ICriterion buildBbox(String bbox) throws InvalidGeometryException {
        return ICriterion.intersectsBbox(bbox);
    }

}
