/*
 * Copyright 2017-2018 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.framework.geojson.geometry;

import fr.cnes.regards.framework.geojson.GeoJsonType;
import fr.cnes.regards.framework.geojson.coordinates.PolygonPositions;

/**
 * RFC 7946 -August 2016<br/>
 * GeoJson Polygon representation<br/>
 * <br/>
 * LineStrings are closed LineString with <b>four or more positions</b>.<br/>
 * First and last positions are equivalents, and they MUST contain identical values.<br/>
 * The first LineString MUST be the exterior ring.<br/>
 * Any others LineString MUST be interior rings.<br/>
 * Exterior rings are counterclockwise, and holes are clockwise.
 *
 * @author Marc Sordi
 *
 */
public class Polygon extends AbstractGeometry<PolygonPositions> {

    public Polygon() {
        super(GeoJsonType.POLYGON);
        coordinates = new PolygonPositions();
    }

    @Override
    public <T> T accept(IGeometryVisitor<T> visitor) {
        return visitor.visitPolygon(this);
    }
}
