/*
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.search.client;

import fr.cnes.regards.framework.feign.annotation.RestClient;
import fr.cnes.regards.framework.urn.UniformResourceName;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import javax.validation.Valid;
import java.util.Collection;
import java.util.Set;

/**
 * API to request data access rights
 *
 * @author Marc Sordi
 */
@RestClient(name = "rs-catalog", contextId = "rs-catalog.access-rights.client")
public interface IAccessRights {

    String ROOT_TYPE_MAPPING = "/entities";

    /**
     * To check access rights
     */
    String ACCESS_RIGHTS_MAPPING = "/access";

    /**
     * To retrieve a single entity
     */
    String URN_MAPPING = "/{urn}";

    String HAS_ACCESS_MAPPING = ACCESS_RIGHTS_MAPPING;

    String ENTITY_HAS_ACCESS_MAPPING = URN_MAPPING + ACCESS_RIGHTS_MAPPING;

    @GetMapping(path = ROOT_TYPE_MAPPING + ENTITY_HAS_ACCESS_MAPPING,
                produces = MediaType.APPLICATION_JSON_VALUE,
                consumes = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<Boolean> hasAccess(@Valid @PathVariable("urn") UniformResourceName urn);

    @PostMapping(path = ROOT_TYPE_MAPPING + HAS_ACCESS_MAPPING,
                 produces = MediaType.APPLICATION_JSON_VALUE,
                 consumes = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<Set<UniformResourceName>> hasAccess(@RequestBody Collection<UniformResourceName> inUrns);
}
