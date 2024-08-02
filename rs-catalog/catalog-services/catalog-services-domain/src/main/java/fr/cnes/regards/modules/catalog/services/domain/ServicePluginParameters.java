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
package fr.cnes.regards.modules.catalog.services.domain;

import fr.cnes.regards.framework.urn.EntityType;
import fr.cnes.regards.modules.search.dto.SearchRequest;

import java.util.Map;

/**
 * Catalog service plugin parameters used to execute the plugin.
 *
 * @author Sébastien Binda
 */
public class ServicePluginParameters {

    /**
     * Entities type to apply plugin service on (case of MANY catalog service plugin type)
     */
    private EntityType entityType;

    /**
     * Opensearch query to retrieve entities to apply plugin service on (case of MANY catalog service plugin type)
     */
    private SearchRequest searchRequest;

    /**
     * Plugin dynamic parameters
     */
    private Map<String, String> dynamicParameters;

    public ServicePluginParameters() {
        super();
    }

    public ServicePluginParameters(EntityType entityType,
                                   SearchRequest searchRequest,
                                   Map<String, String> dynamicParameters) {
        super();
        this.entityType = entityType;
        this.searchRequest = searchRequest;
        this.dynamicParameters = dynamicParameters;
    }

    public EntityType getEntityType() {
        return entityType;
    }

    public SearchRequest getSearchRequest() {
        return searchRequest;
    }

    public Map<String, String> getDynamicParameters() {
        return dynamicParameters;
    }

}
