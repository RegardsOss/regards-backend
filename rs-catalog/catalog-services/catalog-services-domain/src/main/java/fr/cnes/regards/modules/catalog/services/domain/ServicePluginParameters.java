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
package fr.cnes.regards.modules.catalog.services.domain;

import java.util.List;
import java.util.Map;

import fr.cnes.regards.framework.oais.urn.EntityType;

/**
 * Catalog service plugin parameters used to execute the plugin.
 *
 * @author Sébastien Binda
 *
 */
public class ServicePluginParameters {

    /**
     * Entity identifier to apply plugin service on (case of ONE catalog service plugin type)
     */
    private String entityId;

    /**
     * Entities ids to apply plugin service on (case of MANY catalog service plugin type)
     */
    private List<String> entitiesId;

    /**
     * Entities type to apply plugin service on (case of MANY catalog service plugin type)
     */
    private EntityType entityType;

    /**
     * Opensearch query to retrieve entities to apply plugin service on (case of MANY catalog service plugin type)
     */
    private String q;

    /**
     * Plugin dynamic parameters
     */
    private Map<String, String> dynamicParameters;

    public ServicePluginParameters() {
        super();
    }

    public ServicePluginParameters(String pEntityId, List<String> pEntitiesId, EntityType pEntityType,
            String pOpenSearchQuery, Map<String, String> pDynamicParameters) {
        super();
        entityId = pEntityId;
        entitiesId = pEntitiesId;
        entityType = pEntityType;
        q = pOpenSearchQuery;
        dynamicParameters = pDynamicParameters;
    }

    public String getEntityId() {
        return entityId;
    }

    public List<String> getEntitiesId() {
        return entitiesId;
    }

    public EntityType getEntityType() {
        return entityType;
    }

    public String getOpenSearchQuery() {
        return q;
    }

    public Map<String, String> getDynamicParameters() {
        return dynamicParameters;
    }

}
