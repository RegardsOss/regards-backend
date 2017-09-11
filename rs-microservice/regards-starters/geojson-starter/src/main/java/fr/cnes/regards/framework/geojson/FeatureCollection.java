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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * RFC 7946 -August 2016<br/>
 * GeoJson feature collection representation
 *
 * @param <ID> is an optional Feature identifier of type {@link String} or {@link Number}
 *
 * @author Marc Sordi
 *
 */
public class FeatureCollection<ID extends Serializable> extends AbstractGeoJsonObject {

    private final List<Feature<ID>> features = new ArrayList<>();

    public FeatureCollection() {
        super(GeoJsonType.FEATURE_COLLECTION);
    }

    public List<Feature<ID>> getFeatures() {
        return features;
    }

    public void addAll(Collection<Feature<ID>> features) {
        this.features.addAll(features);
    }

    public void add(Feature<ID> feature) {
        this.features.add(feature);
    }
}
