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
package fr.cnes.regards.modules.catalog.services.domain.plugins;

import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ResponseEntity;

import fr.cnes.regards.modules.models.domain.EntityType;

/**
 * Interface to define a Catalog service plugin. This plugins applies on multiple entities.
 * The multiple entities can be :
 *  - Directly provided with a list of identifiers.
 *  - Provided throught an opensearch request (used to manage a big volume of entities)
 *
 * @author Sébastien Binda
 *
 */
public interface IEntitiesServicePlugin extends IService {

    /**
     * Apply the current service to the provided list of entities.
     * @param pEntitiesId Identifier of each entity
     */
    ResponseEntity<InputStreamResource> applyOnEntities(List<String> pEntitiesId, HttpServletResponse response);

    /**
     * Apply the current service with a given openSearch request and an entityType to apply on.
     * @param pOpenSearchQuery OpenSearch query
     * @param pEntityType Entity type
     */
    ResponseEntity<InputStreamResource> applyOnQuery(String pOpenSearchQuery, EntityType pEntityType,
            HttpServletResponse response);

}
