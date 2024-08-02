/*
 * Copyright 2017-2024 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.spatial4j.shape.Rectangle;

import java.util.Objects;

public class EnvelopeBuilder extends ShapeBuilder<Rectangle, org.elasticsearch.geometry.Rectangle, EnvelopeBuilder> {

    private final Coordinate topLeft;

    private final Coordinate bottomRight;

    public EnvelopeBuilder(Coordinate topLeft, Coordinate bottomRight) {
        Objects.requireNonNull(topLeft, "topLeft of envelope cannot be null");
        Objects.requireNonNull(bottomRight, "bottomRight of envelope cannot be null");
        if (Double.isNaN(topLeft.z) != Double.isNaN(bottomRight.z)) {
            throw new IllegalArgumentException("expected same number of dimensions for topLeft and bottomRight");
        } else {
            this.topLeft = topLeft;
            this.bottomRight = bottomRight;
        }
    }

    public Coordinate topLeft() {
        return this.topLeft;
    }

    public Coordinate bottomRight() {
        return this.bottomRight;
    }

    protected StringBuilder contentToWKT() {
        StringBuilder sb = new StringBuilder();
        sb.append("(");
        sb.append(this.topLeft.x);
        sb.append(",");
        sb.append(" ");
        sb.append(this.bottomRight.x);
        sb.append(",");
        sb.append(" ");
        sb.append(this.topLeft.y);
        sb.append(",");
        sb.append(" ");
        sb.append(this.bottomRight.y);
        sb.append(")");
        return sb;
    }

    public Rectangle buildS4J() {
        return SPATIAL_CONTEXT.makeRectangle(this.topLeft.x, this.bottomRight.x, this.bottomRight.y, this.topLeft.y);
    }

    public org.elasticsearch.geometry.Rectangle buildGeometry() {
        return new org.elasticsearch.geometry.Rectangle(this.topLeft.x,
                                                        this.bottomRight.x,
                                                        this.topLeft.y,
                                                        this.bottomRight.y);
    }

    public int numDimensions() {
        return Double.isNaN(this.topLeft.z) ? 2 : 3;
    }

    public int hashCode() {
        return Objects.hash(new Object[] { this.topLeft, this.bottomRight });
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (obj != null && this.getClass() == obj.getClass()) {
            EnvelopeBuilder other = (EnvelopeBuilder) obj;
            return Objects.equals(this.topLeft, other.topLeft) && Objects.equals(this.bottomRight, other.bottomRight);
        } else {
            return false;
        }
    }
}