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
package fr.cnes.regards.modules.search.client;

import java.util.Collection;
import java.util.Set;

import javax.validation.Valid;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import fr.cnes.regards.framework.feign.annotation.RestClient;
import fr.cnes.regards.framework.oais.urn.UniformResourceName;

/**
 * API to request data access rights
 *
 * @author Marc Sordi
 *
 */
@RestClient(name = "rs-catalog")
@RequestMapping(value = IAccessRights.TYPE_MAPPING, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
public interface IAccessRights {

    static final String TYPE_MAPPING = "/entities";

    /**
     * To check access rights
     */
    static final String ACCESS_RIGHTS_MAPPING = "/access";

    /**
     * To retrieve a single entity
     */
    static final String URN_MAPPING = "/{urn}";

    static final String HAS_ACCESS_MAPPING = ACCESS_RIGHTS_MAPPING;

    static final String ENTITY_HAS_ACCESS_MAPPING = URN_MAPPING + ACCESS_RIGHTS_MAPPING;

    @RequestMapping(method = RequestMethod.GET, value = ENTITY_HAS_ACCESS_MAPPING)
    ResponseEntity<Boolean> hasAccess(@Valid @PathVariable UniformResourceName urn);

    @RequestMapping(method = RequestMethod.POST, value = HAS_ACCESS_MAPPING)
    ResponseEntity<Set<UniformResourceName>> hasAccess(@RequestBody Collection<UniformResourceName> inUrns);
}
