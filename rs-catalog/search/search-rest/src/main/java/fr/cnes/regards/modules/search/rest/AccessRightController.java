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
package fr.cnes.regards.modules.search.rest;

import com.google.common.collect.Iterables;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.framework.urn.DataType;
import fr.cnes.regards.framework.urn.EntityType;
import fr.cnes.regards.framework.urn.UniformResourceName;
import fr.cnes.regards.modules.dam.domain.entities.AbstractEntity;
import fr.cnes.regards.modules.dam.domain.entities.DataObject;
import fr.cnes.regards.modules.indexer.dao.FacetPage;
import fr.cnes.regards.modules.indexer.domain.criterion.ICriterion;
import fr.cnes.regards.modules.indexer.domain.criterion.StringMatchType;
import fr.cnes.regards.modules.indexer.service.Searches;
import fr.cnes.regards.modules.search.service.ICatalogSearchService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * This controller allows to check data access rights
 *
 * @author Marc Sordi
 */
@RestController
@RequestMapping(path = AccessRightController.TYPE_MAPPING)
public class AccessRightController {

    @SuppressWarnings("unused")
    private static final Logger LOGGER = LoggerFactory.getLogger(AccessRightController.class);

    public static final String TYPE_MAPPING = "/entities";

    /**
     * To check access rights
     */
    private static final String ACCESS_RIGHTS_MAPPING = "/access";

    /**
     * To retrieve a single entity
     */
    private static final String URN_MAPPING = "/{urn}";

    public static final String HAS_ACCESS_MAPPING = ACCESS_RIGHTS_MAPPING;

    public static final String ENTITY_HAS_ACCESS_MAPPING = URN_MAPPING + ACCESS_RIGHTS_MAPPING;

    /**
     * Business search service
     */
    @Autowired
    protected ICatalogSearchService searchService;

    @RequestMapping(method = RequestMethod.GET, value = ENTITY_HAS_ACCESS_MAPPING)
    @ResourceAccess(description = "Allows to know if the user can download an entity", role = DefaultRole.PUBLIC)
    public ResponseEntity<Boolean> hasAccess(@Valid @PathVariable UniformResourceName urn) throws ModuleException {
        AbstractEntity<?> entity = searchService.get(urn);
        if (entity instanceof DataObject) {
            return ResponseEntity.ok(((DataObject) entity).getFiles().containsKey(DataType.RAWDATA));
        }
        return ResponseEntity.ok(true);
    }

    @RequestMapping(method = RequestMethod.POST, value = HAS_ACCESS_MAPPING)
    @ResourceAccess(description = "Allows to know if the user can download entities", role = DefaultRole.PUBLIC)
    public ResponseEntity<Set<UniformResourceName>> hasAccess(@RequestBody Collection<UniformResourceName> inUrns)
        throws ModuleException {
        if (inUrns.isEmpty()) {
            return ResponseEntity.ok(Collections.emptySet());
        }
        Set<UniformResourceName> urnsWithAccess = new HashSet<>();
        // ElasticSearch cannot manage more than 1024 criterions clauses at once. There is one clause per IP_ID plus
        // or clauses plus some depending on user access => create partitions of 1 000
        Iterable<List<UniformResourceName>> urnLists = Iterables.partition(inUrns, 1_000);
        for (List<UniformResourceName> urns : urnLists) {
            ICriterion criterion = ICriterion.or(urns.stream()
                                                     .map(urn -> ICriterion.eq("ipId",
                                                                               urn.toString(),
                                                                               StringMatchType.KEYWORD))
                                                     .toArray(n -> new ICriterion[n]));
            FacetPage<DataObject> page = searchService.search(criterion,
                                                              Searches.onSingleEntity(EntityType.DATA),
                                                              null,
                                                              PageRequest.of(0, urns.size()));
            urnsWithAccess.addAll(page.getContent()
                                      .parallelStream()
                                      .filter(dataObject -> dataObject.getFiles().containsKey(DataType.RAWDATA))
                                      .map(dataObject -> dataObject.getIpId())
                                      .collect(Collectors.toSet()));
        }
        return ResponseEntity.ok(urnsWithAccess);
    }
}
