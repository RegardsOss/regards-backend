/*
 * Copyright 2017 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * RFC 7946 -August 2016<br/>
 * GeoJson base feature collection representation
 *
 * This is the base class for implementing GeoJson feature collection. Extend it to create your own.
 *
 * @param <F> represents feature type
 *
 * @author Marc Sordi
 *
 */
public abstract class AbstractFeatureCollection<F extends AbstractFeature<?, ?>> extends AbstractGeoJsonObject {

    @Valid
    private final List<F> features = new ArrayList<>();

    public AbstractFeatureCollection() {
        super(GeoJsonType.FEATURE_COLLECTION);
    }

    public final List<F> getFeatures() {
        return features;
    }

    public void addAll(Collection<F> features) {
        this.features.addAll(features);
    }

    public void add(F feature) {
        this.features.add(feature);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = (prime * result) + features.hashCode();
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
        @SuppressWarnings("rawtypes")
        AbstractFeatureCollection other = (AbstractFeatureCollection) obj;
        if (!features.equals(other.features)) {
            return false;
        }
        return true;
    }
}
