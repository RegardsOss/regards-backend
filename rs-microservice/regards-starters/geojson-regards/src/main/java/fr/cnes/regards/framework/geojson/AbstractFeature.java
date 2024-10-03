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
package fr.cnes.regards.framework.geojson;

import fr.cnes.regards.framework.geojson.geometry.*;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;

/**
 * RFC 7946 -August 2016<br/>
 * GeoJson base feature representation<br/>
 * <p>
 * This is the base class for implementing GeoJson feature. Extend it to create your own.
 *
 * @param <P>  wrapper for properties
 * @param <ID> is an optional Feature identifier of type {@link String} or {@link Number}
 * @author Marc Sordi
 */
public abstract class AbstractFeature<P, ID> extends AbstractGeoJsonObject {

    /**
     * ID MUST be a {@link String} or a {@link Number}
     */
    @Schema(description = "Provider identifier for the submitted product.", example = "RawProduct_001")
    protected ID id;

    /**
     * One of the available GeoJson geometry plus the custom unlocated one :
     * <ul>
     * <li>{@link Point}</li>
     * <li>{@link MultiPoint}</li>
     * <li>{@link LineString}</li>
     * <li>{@link MultiLineString}</li>
     * <li>{@link Polygon}</li>
     * <li>{@link MultiPolygon}</li>
     * <li>{@link GeometryCollection}</li>
     * <li>{@link Unlocated}</li>
     * </ul>
     */
    @Valid
    @Schema(description = "Information package geometry in GeoJSON RFC 7946 Format.",
            example = "{\"type\": \"Point\", \"coordinates\": [125.6, 10.1]}")
    protected IGeometry geometry = IGeometry.unlocated();

    /**
     * Same as geometry but normalized (see crawler) to be used on a cylindric project.
     */
    @Schema(hidden = true)
    private IGeometry normalizedGeometry = IGeometry.unlocated();

    @Valid
    @Schema(description = "Information package properties")
    protected P properties;

    public AbstractFeature() {
        super(GeoJsonType.FEATURE);
    }

    public ID getId() {
        return id;
    }

    public void setId(ID id) {
        this.id = id;
    }

    public IGeometry getGeometry() {
        return geometry;
    }

    public void setGeometry(IGeometry geometry) {
        this.geometry = geometry;
    }

    public IGeometry getNormalizedGeometry() {
        return normalizedGeometry;
    }

    public void setNormalizedGeometry(IGeometry normalizedGeometry) {
        this.normalizedGeometry = normalizedGeometry;
    }

    public P getProperties() {
        return properties;
    }

    public void setProperties(P properties) {
        this.properties = properties;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = (prime * result) + (id == null ? 0 : id.hashCode());
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
        @SuppressWarnings("rawtypes") AbstractFeature other = (AbstractFeature) obj;
        if (id == null) {
            return other.id == null;
        } else if (!id.equals(other.id)) {
            return false;
        }
        return true;
    }
}
