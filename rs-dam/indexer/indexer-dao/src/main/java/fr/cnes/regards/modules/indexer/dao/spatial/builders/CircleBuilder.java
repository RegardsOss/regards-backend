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
package fr.cnes.regards.modules.indexer.dao.spatial.builders;

import org.elasticsearch.common.unit.DistanceUnit;
import org.elasticsearch.common.unit.DistanceUnit.Distance;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.spatial4j.shape.Circle;

import java.util.Objects;

public class CircleBuilder extends ShapeBuilder<Circle, org.elasticsearch.geometry.Circle, CircleBuilder> {

    private DistanceUnit unit;

    private double radius;

    private Coordinate center;

    public CircleBuilder() {
        this.unit = DistanceUnit.DEFAULT;
        this.center = ZERO_ZERO;
    }

    public CircleBuilder center(Coordinate center) {
        this.center = center;
        return this;
    }

    public CircleBuilder center(double lon, double lat) {
        return this.center(new Coordinate(lon, lat));
    }

    public Coordinate center() {
        return this.center;
    }

    public CircleBuilder radius(String radius) {
        return this.radius(Distance.parseDistance(radius));
    }

    public CircleBuilder radius(Distance radius) {
        return this.radius(radius.value, radius.unit);
    }

    public CircleBuilder radius(double radius, String unit) {
        return this.radius(radius, DistanceUnit.fromString(unit));
    }

    public CircleBuilder radius(double radius, DistanceUnit unit) {
        this.unit = unit;
        this.radius = radius;
        return this;
    }

    public double radius() {
        return this.radius;
    }

    public DistanceUnit unit() {
        return this.unit;
    }

    public Circle buildS4J() {
        return SPATIAL_CONTEXT.makeCircle(this.center.x,
                                          this.center.y,
                                          360.0D * this.radius / this.unit.getEarthCircumference());
    }

    public org.elasticsearch.geometry.Circle buildGeometry() {
        return new org.elasticsearch.geometry.Circle(this.center.x, this.center.y, this.unit.toMeters(this.radius));
    }

    public String toWKT() {
        throw new UnsupportedOperationException("The WKT spec does not support CIRCLE geometry");
    }

    public int numDimensions() {
        return Double.isNaN(this.center.z) ? 2 : 3;
    }

    public int hashCode() {
        return Objects.hash(new Object[] { this.center, this.radius, this.unit.ordinal() });
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (obj != null && this.getClass() == obj.getClass()) {
            CircleBuilder other = (CircleBuilder) obj;
            return Objects.equals(this.center, other.center)
                   && Objects.equals(this.radius, other.radius)
                   && Objects.equals(this.unit.ordinal(), other.unit.ordinal());
        } else {
            return false;
        }
    }
}