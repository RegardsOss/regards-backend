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

import java.util.ArrayList;
import java.util.List;

import org.springframework.restdocs.payload.FieldDescriptor;

import com.google.common.base.Strings;

import fr.cnes.regards.framework.test.integration.ConstrainedFields;

/**
 * Builds the description of all fields found in {@link AbstractFeatureCollection}.
 * @author Christophe Mertz
 */
public class GeoJsonFieldDescriptors {

    private String prefix;

    public GeoJsonFieldDescriptors() {
        super();
        prefix = null;
    }

    public GeoJsonFieldDescriptors(String prefix) {
        super();
        this.prefix = prefix;
    }

    public List<FieldDescriptor> build() {
        List<FieldDescriptor> lfd = new ArrayList<>();

        ConstrainedFields fieldsCollection = new ConstrainedFields(AbstractFeatureCollection.class);

        lfd.add(fieldsCollection.withPath("features", "Array of feature"));

        ConstrainedFields fieldsFeature = new ConstrainedFields(AbstractFeature.class);

        lfd.add(fieldsFeature.withPath(addPrefix("id"), "SIP id"));
        lfd.add(fieldsFeature
                        .withPath(addPrefix("ipType"), "type", "GeoJson type representation - RFC 7946 -August 2016",
                                  null));
        lfd.add(fieldsFeature.withPath(addPrefix("geometry"),
                                       "GeoJson base feature representation - RFC 7946 -August 2016"));
        lfd.add(fieldsFeature.withPath(addPrefix("bbox"), "An optional bounding box"));
        lfd.add(fieldsFeature.withPath(addPrefix("properties"), "properties"));

        return lfd;
    }

    private String addPrefix(String path) {
        return Strings.isNullOrEmpty(prefix) ? path : prefix + path;
    }

}
