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
package fr.cnes.regards.framework.geojson.coordinates;

import java.util.ArrayList;
import java.util.Optional;

/**
 * RFC 7946 -August 2016<br/>
 * GeoJson geometry position representation
 *
 * @author Marc Sordi
 */
public class Position extends ArrayList<Double> {

    public Position() {
        super();
    }

    public Position(Double longitude, Double latitude) {
        super(2);
        super.add(longitude);
        super.add(latitude);

    }

    public Position(Double longitude, Double latitude, Double altitude) {
        super(3);
        super.add(longitude);
        super.add(latitude);
        super.add(altitude);
    }

    /**
     * Create a Position from array(size 2 or 3 coordinates) : { longitude, latitude } or {  longitude, latitude, altitude }
     * <B>NOTE: the goal of this method is to ease creation/transformation/computation of geometries so no check is
     * done concerning input values.</B>
     */
    public static Position fromArray(double[] coordinates) {
        if (coordinates.length == 2) {
            return new Position(coordinates[0], coordinates[1]);
        } else if (coordinates.length == 3) {
            return new Position(coordinates[0], coordinates[1], coordinates[2]);
        } else {
            throw new IllegalArgumentException(
                "Position should at least have 2 coordinates: longitude and latitude, or 3 (longitude, latitude, altitude)");
        }
    }

    public Double getLongitude() {
        return get(0);
    }

    /**
     * Useful for normalization
     */
    public void setLongitude(Double longitude) {
        set(0, longitude);
    }

    public Double getLatitude() {
        return get(1);
    }

    public Optional<Double> getAltitude() {
        return size() == 2 ? Optional.empty() : Optional.of(get(2));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = (prime * result) + (getAltitude().isPresent() ? getAltitude().get().hashCode() : 0);
        result = (prime * result) + ((getLatitude() == null) ? 0 : getLatitude().hashCode());
        result = (prime * result) + ((getLongitude() == null) ? 0 : getLongitude().hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        Position other = (Position) obj;
        if (!getAltitude().isPresent()) {
            if (other.getAltitude().isPresent()) {
                return false;
            }
        } else if (!getAltitude().equals(other.getAltitude())) {
            return false;
        }
        if (getLatitude() == null) {
            if (other.getLatitude() != null) {
                return false;
            }
        } else if (!getLatitude().equals(other.getLatitude())) {
            return false;
        }
        if (getLongitude() == null) {
            return other.getLongitude() == null;
        } else {
            return getLongitude().equals(other.getLongitude());
        }
    }

    @Override
    public String toString() {
        if (getAltitude().isPresent()) {
            return getLongitude() + ", " + getLatitude() + ", " + getAltitude();
        } else {
            return getLongitude() + ", " + getLatitude();
        }
    }

    /**
     * Return position as Double[] where first index is longitude and second latitude and if exists third altitude
     */
    @Override
    public Double[] toArray() {
        return getAltitude().isPresent() ?
            new Double[] { getLongitude(), getLatitude(), getAltitude().get() } :
            new Double[] { getLongitude(), getLatitude() };
    }
}
