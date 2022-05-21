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

import fr.cnes.regards.framework.geojson.geometry.IGeometry;
import fr.cnes.regards.framework.geojson.geometry.MultiPolygon;
import fr.cnes.regards.framework.geojson.geometry.Polygon;
import fr.cnes.regards.modules.indexer.domain.criterion.ICriterion;
import fr.cnes.regards.modules.indexer.domain.criterion.exception.InvalidGeometryException;

import java.util.ArrayList;
import java.util.List;

/**
 * Creates {@link ICriterion} from multiple toponym geometry types.
 *
 * @author Iliana Ghazali
 */
public class ToponymGeometryCriterionBuilder {

    private ToponymGeometryCriterionBuilder() {
    }

    /**
     * Creates a {@link ICriterion} from Polygon or MultiPolygon
     *
     * @param toponymGeometry geometry of a toponym in {@link IGeometry} format
     * @return {@link ICriterion}
     * @throws InvalidGeometryException if the toponymGeometry is different from a Polygon or a MultiPolygon
     */
    public static ICriterion build(IGeometry toponymGeometry) throws InvalidGeometryException {
        if (toponymGeometry instanceof Polygon) {
            Polygon polygon = (Polygon) toponymGeometry;
            return ICriterion.intersectsPolygon(polygon.toArray());
        } else if (toponymGeometry instanceof MultiPolygon) {
            MultiPolygon multiPolygon = (MultiPolygon) toponymGeometry;
            // get all polygons from the multipolygon and create and a list of ICriterion
            double[][][][] multiPolygonToArray = multiPolygon.toArray();
            List<ICriterion> listCriterion = new ArrayList<>();
            for (double[][][] polygonArray : multiPolygonToArray) {
                listCriterion.add(ICriterion.intersectsPolygon(polygonArray));
            }
            return ICriterion.or(listCriterion);
        } else {
            // Only Polygons and MultiPolygons are handled
            throw new InvalidGeometryException("Only polygons and multi-polygons are handled for toponyms");
        }

    }

}
