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
package fr.cnes.regards.framework.geojson.coordinates;

import java.util.Optional;

/**
 * RFC 7946 -August 2016<br/>
 * GeoJson geometry position representation
 * @author Marc Sordi
 */
public class Position {

    private Double longitude;

    private Double latitude;

    private Double altitude = null;

    public Position() {
        // Useful for serialization
    }

    public Position(Double longitude, Double latitude) {
        this.longitude = longitude;
        this.latitude = latitude;
    }

    public Position(Double longitude, Double latitude, Double altitude) {
        this.longitude = longitude;
        this.latitude = latitude;
        this.altitude = altitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public Double getLatitude() {
        return latitude;
    }

    /**
     * Useful for normalization
     */
    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public Optional<Double> getAltitude() {
        return Optional.ofNullable(altitude);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = (prime * result) + ((altitude == null) ? 0 : altitude.hashCode());
        result = (prime * result) + ((latitude == null) ? 0 : latitude.hashCode());
        result = (prime * result) + ((longitude == null) ? 0 : longitude.hashCode());
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
        if (altitude == null) {
            if (other.altitude != null) {
                return false;
            }
        } else if (!altitude.equals(other.altitude)) {
            return false;
        }
        if (latitude == null) {
            if (other.latitude != null) {
                return false;
            }
        } else if (!latitude.equals(other.latitude)) {
            return false;
        }
        if (longitude == null) {
            return other.longitude == null;
        } else
            return longitude.equals(other.longitude);
    }

    @Override
    public String toString() {
        if (altitude != null) {
            return getLongitude() + ", " + getLatitude() + ", " + getAltitude();
        } else {
            return getLongitude() + ", " + getLatitude();
        }
    }

    /**
     * Return position as double[] where first index is longitude and seconde latitude
     */
    public double[] toArray() {
        return new double[] { longitude, latitude };
    }

    /**
     * Create a Position from array { longitude, latitude }
     * <B>NOTE: the goal of this method is to ease creation/transformation/computation of geometries so no check is
     * done concerning input values.</B>
     */
    public static Position fromArray(double[] lonLat) {
        return new Position(lonLat[0], lonLat[1]);
    }
}
