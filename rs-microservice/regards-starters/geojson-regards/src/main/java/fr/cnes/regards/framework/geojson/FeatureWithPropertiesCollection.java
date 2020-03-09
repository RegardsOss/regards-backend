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
package fr.cnes.regards.framework.geojson;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * This class represents a default GeoJson feature collection containing default {@link Feature}
 * identifier.<br/>
 * You can define your own extending {@link AbstractFeatureCollection}.
 * @author Sébastien Binda
 */
public class FeatureWithPropertiesCollection extends FeatureCollection {

    public static final String ID_FIELD = "id";

    public static final String TITLE_FIELD = "title";

    public static final String TOTAL_RESULTS_FIELD = "totalResults";

    public static final String START_IDX_FIELD = "startIndex";

    public static final String ITEMS_PER_PAGE_FIELD = "itemsPerPage";

    public static final String DESCRIPTION_FIELD = "description";

    public static final String QUERY_FIELD = "query";

    public static final String LINKS_FIELD = "links";

    protected Map<String, Object> properties;

    public FeatureWithPropertiesCollection() {
        properties = new HashMap<>();
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    public void addProperty(String key, Object value) {
        properties.put(key, value);
    }

    public void setId(String value) {
        properties.put(ID_FIELD, value);
    }

    public void setTitle(String value) {
        properties.put(TITLE_FIELD, value);
    }

    public void setPaginationInfos(long totalResults, long startIndex, int itemsPerPage) {
        properties.put(TOTAL_RESULTS_FIELD, totalResults);
        properties.put(START_IDX_FIELD, startIndex);
        properties.put(ITEMS_PER_PAGE_FIELD, itemsPerPage);
    }

    public void setDescription(String description) {
        properties.put(DESCRIPTION_FIELD, description);
    }

    public void setQuery(Query query) {
        properties.put(QUERY_FIELD, query);
    }

    public void setLinks(Collection<GeoJsonLink> links) {
        properties.put(LINKS_FIELD, links);
    }
}
