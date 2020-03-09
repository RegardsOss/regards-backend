/*
 * Copyright 2017-2020 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import fr.cnes.regards.framework.geojson.AbstractGeoJsonObject;
import fr.cnes.regards.framework.geojson.GeoJsonType;

/**
 * Not in RFC 7946 -August 2016<br/>
 * GeoJson unlocated feature representation
 * @author Marc Sordi
 */
public class Unlocated extends AbstractGeoJsonObject implements IGeometry {

    public Unlocated() {
        super(GeoJsonType.UNLOCATED);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends IGeometry> T withCrs(String crs) {
        return (T) this;
    }

    @Override
    public String toString() {
        return "Unlocated";
    }

    @Override
    public <T> T accept(IGeometryVisitor<T> visitor) {
        return visitor.visitUnlocated(this);
    }
}
