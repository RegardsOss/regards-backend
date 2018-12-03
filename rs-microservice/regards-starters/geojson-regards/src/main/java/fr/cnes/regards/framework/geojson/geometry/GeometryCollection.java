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

import java.util.ArrayList;
import java.util.List;

import fr.cnes.regards.framework.geojson.AbstractGeoJsonObject;
import fr.cnes.regards.framework.geojson.GeoJsonType;

/**
 * RFC 7946 -August 2016<br/>
 * GeoJson GeometryCollection representation
 * @author Marc Sordi
 */
public class GeometryCollection extends AbstractGeoJsonObject implements IGeometry {

    /**
     * AbstractGeometry&lt;?> instead of IGeometry to avoid a GeometryCollection of GeometryCollection
     */
    private List<AbstractGeometry<?>> geometries = new ArrayList<>();

    public GeometryCollection() {
        super(GeoJsonType.GEOMETRY_COLLECTION);
    }

    public List<AbstractGeometry<?>> getGeometries() {
        return geometries;
    }

    public void setGeometries(List<AbstractGeometry<?>> geometries) {
        this.geometries = geometries;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = (prime * result) + ((geometries == null) ? 0 : geometries.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!super.equals(obj)) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        GeometryCollection other = (GeometryCollection) obj;
        if (geometries == null) {
            return other.geometries == null;
        } else
            return geometries.equals(other.geometries);
    }

    @Override
    public <T> T accept(IGeometryVisitor<T> visitor) {
        return visitor.visitGeometryCollection(this);
    }
}
