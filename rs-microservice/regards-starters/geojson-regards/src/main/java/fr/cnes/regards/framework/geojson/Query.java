/*
 * Copyright 2017-2019 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import java.util.Map;

import com.google.common.collect.Maps;

/**
 * Query for geo+json opensearch meta response.
 * @author Sébastien Binda
 */
public class Query {

    private final Map<String, String> searchFilters = Maps.newHashMap();

    public Map<String, String> getSearchFilters() {
        return searchFilters;
    }

    public void addFilter(String name, String value) {
        searchFilters.put(name, value);
    }

}
