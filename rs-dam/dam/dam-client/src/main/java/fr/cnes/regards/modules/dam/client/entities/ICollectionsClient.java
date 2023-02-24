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
package fr.cnes.regards.modules.dam.client.entities;

import fr.cnes.regards.framework.feign.annotation.RestClient;
import fr.cnes.regards.framework.oais.urn.OaisUniformResourceName;
import fr.cnes.regards.modules.dam.domain.entities.Collection;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.HttpEntity;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

/**
 * @author Sylvain Vissiere-Guerinet
 * @author Léo Mieulet
 */
@RestClient(name = "rs-dam", contextId = "rs-dam.collections.client")
public interface ICollectionsClient {

    String ROOT_PATH = "/collections";

    /**
     * @return list of all {@link Collection}
     * Entry point to retrieve all collections
     */
    @GetMapping(path = ROOT_PATH,
                consumes = MediaType.APPLICATION_JSON_VALUE,
                produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    HttpEntity<List<EntityModel<Collection>>> retrieveCollections();

    /**
     * @param collectionId Identifier of the {@link Collection} wanted
     * @return the specified {@link Collection}
     * Entry point to retrieve a {@link Collection} using its id
     */
    @GetMapping(path = ROOT_PATH + "/{collection_id}",
                consumes = MediaType.APPLICATION_JSON_VALUE,
                produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public HttpEntity<EntityModel<Collection>> retrieveCollection(@PathVariable("collection_id") Long collectionId);

    /**
     * @param collectionId id of the {@link Collection} to update
     * @param collection   updated {@link Collection}
     * @return Updated {@link Collection}
     * Entry point to update a {@link Collection} using its id
     */
    @PutMapping(path = ROOT_PATH + "/{collection_id}",
                consumes = MediaType.APPLICATION_JSON_VALUE,
                produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    HttpEntity<EntityModel<Collection>> updateCollection(@PathVariable("collection_id") Long collectionId,
                                                         @RequestBody Collection collection);

    /**
     * @param collectionId id of the {@link Collection} to delete
     * @return Void
     * Entry point to delete a collection using its id
     */
    @DeleteMapping(path = ROOT_PATH + "/{collection_id}",
                   consumes = MediaType.APPLICATION_JSON_VALUE,
                   produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    HttpEntity<Void> deleteCollection(@PathVariable("collection_id") Long collectionId);

    /**
     * @param collection {@link Collection} to create
     * @return created {@link Collection}
     * Entry point to create a collection
     */
    @PostMapping(path = ROOT_PATH,
                 consumes = MediaType.APPLICATION_JSON_VALUE,
                 produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    HttpEntity<EntityModel<Collection>> createCollection(@RequestBody Collection collection);

    /**
     * Entry point to handle dissociation of {@link Collection} specified by its id to other entities
     *
     * @param collectionId    {@link Collection} id
     * @param toBeDissociated entity to dissociate
     * @return {@link Collection} as a {@link EntityModel}
     */
    @PutMapping(path = ROOT_PATH + "/{collection_id}/dissociate",
                consumes = MediaType.APPLICATION_JSON_VALUE,
                produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    HttpEntity<EntityModel<Collection>> dissociateCollection(@PathVariable("collection_id") Long collectionId,
                                                             @RequestBody Set<OaisUniformResourceName> toBeDissociated);

    /**
     * Entry point to handle association of {@link Collection} specified by its id to other entities
     *
     * @param collectionId       {@link Collection} id
     * @param toBeAssociatedWith entities to be associated
     * @return {@link Collection} as a {@link EntityModel}
     */
    @PutMapping(path = ROOT_PATH + "/{collection_id}/associate",
                consumes = MediaType.APPLICATION_JSON_VALUE,
                produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    HttpEntity<EntityModel<Collection>> associateCollections(@PathVariable("collection_id") Long collectionId,
                                                             @RequestBody
                                                             Set<OaisUniformResourceName> toBeAssociatedWith);
}
