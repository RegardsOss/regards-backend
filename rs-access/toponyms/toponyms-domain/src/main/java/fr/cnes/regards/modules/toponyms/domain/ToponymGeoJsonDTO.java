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
package fr.cnes.regards.modules.toponyms.domain;

import fr.cnes.regards.framework.geojson.geometry.IGeometry;
import java.util.Map;
import java.util.Objects;
import org.geolatte.geom.Geometry;

/**
 *
 * DTO to transfer {@link Toponym} objects with {@link IGeometry} in place of {@link Geometry}
 *
 * @author Iliana
 *
 */
public class ToponymGeoJsonDTO {

    /**
     * Type
     */
    private String type;

    /**
     * Geojson geometry
     */
    private Map<String, Object> geometry;

    /**
     * Owner
     */
    private Map<String, Object> properties;


    public ToponymGeoJsonDTO(String type, Map<String, Object> geometry, Map<String, Object> properties) {
        this.type = type;
        this.geometry = geometry;
        this.properties = properties;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Map<String, Object>  getGeometry() {
        return geometry;
    }

    public void setGeometry(Map<String, Object> geometry) {
        this.geometry = geometry;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, Object> properties) {
        this.properties = properties;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        ToponymGeoJsonDTO that = (ToponymGeoJsonDTO) o;
        return Objects.equals(type, that.type) && Objects.equals(geometry, that.geometry) && Objects
                .equals(properties, that.properties);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, geometry, properties);
    }

    @Override
    public String toString() {
        return "ToponymGeoJsonDTO{" + "type='" + type + '\'' + ", geometry=" + geometry + ", properties=" + properties
                + '}';
    }
}
