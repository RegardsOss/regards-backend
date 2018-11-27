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
package fr.cnes.regards.framework.geojson;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * This class represents a default GeoJson feature with properties as {@link Map} and an optional {@link String}
 * identifier.<br/>
 * You can define your own extending {@link AbstractFeature}.
 * @author Marc Sordi
 */
public class Feature extends AbstractFeature<Map<String, Object>, String> {

    /**
     * Title field name in geojson feature
     */
    public static final String TITLE_FIELD = "title";

    public static final String UPDATED_FIELD = "updated";

    public static final String LINKS_FIELD = "links";

    public Feature() {
        properties = new HashMap<>();
    }

    public void addProperty(String key, Object value) {
        properties.put(key, value);
    }

    public void setTitle(String title) {
        properties.put(TITLE_FIELD, title);
    }

    public void setLinks(Collection<GeoJsonLink> links) {
        properties.put(LINKS_FIELD, links);
    }

    public void setUpdated(OffsetDateTime updated) {
        properties.put(UPDATED_FIELD, updated);
    }
}
