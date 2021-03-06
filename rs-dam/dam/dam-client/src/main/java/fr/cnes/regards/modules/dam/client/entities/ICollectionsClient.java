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

import java.util.List;
import java.util.Set;

import org.springframework.hateoas.EntityModel;
import org.springframework.http.HttpEntity;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import fr.cnes.regards.framework.feign.annotation.RestClient;
import fr.cnes.regards.framework.oais.urn.OaisUniformResourceName;
import fr.cnes.regards.modules.dam.domain.entities.Collection;

/**
 * @author Sylvain Vissiere-Guerinet
 * @author Léo Mieulet
 */
@RestClient(name = "rs-dam", contextId = "rs-dam.collections.client")
@RequestMapping(value = "/collections", consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE)
public interface ICollectionsClient {

    /**
     * @return list of all {@link Collection}
     * Entry point to retrieve all collections
     */
    @RequestMapping(method = RequestMethod.GET)
    @ResponseBody
    HttpEntity<List<EntityModel<Collection>>> retrieveCollections();

    /**
     * @param collectionId Identifier of the {@link Collection} wanted
     * @return the specified {@link Collection}
     * Entry point to retrieve a {@link Collection} using its id
     */
    @RequestMapping(method = RequestMethod.GET, value = "/{collection_id}")
    @ResponseBody
    public HttpEntity<EntityModel<Collection>> retrieveCollection(@PathVariable("collection_id") Long collectionId);

    /**
     * @param collectionId id of the {@link Collection} to update
     * @param collection updated {@link Collection}
     * @return Updated {@link Collection}
     * Entry point to update a {@link Collection} using its id
     */
    @RequestMapping(method = RequestMethod.PUT, value = "/{collection_id}")
    @ResponseBody
    HttpEntity<EntityModel<Collection>> updateCollection(@PathVariable("collection_id") Long collectionId,
            @RequestBody Collection collection);

    /**
     * @param collectionId id of the {@link Collection} to delete
     * @return Void
     * Entry point to delete a collection using its id
     */
    @RequestMapping(method = RequestMethod.DELETE, value = "/{collection_id}")
    @ResponseBody
    HttpEntity<Void> deleteCollection(@PathVariable("collection_id") Long collectionId);

    /**
     * @param collection {@link Collection} to create
     * @return created {@link Collection}
     * Entry point to create a collection
     */
    @RequestMapping(method = RequestMethod.POST)
    @ResponseBody
    HttpEntity<EntityModel<Collection>> createCollection(@RequestBody Collection collection);

    /**
     * Entry point to handle dissociation of {@link Collection} specified by its id to other entities
     * @param collectionId {@link Collection} id
     * @param toBeDissociated entity to dissociate
     * @return {@link Collection} as a {@link EntityModel}
     */
    @RequestMapping(method = RequestMethod.PUT, value = "/{collection_id}/dissociate")
    @ResponseBody
    HttpEntity<EntityModel<Collection>> dissociateCollection(@PathVariable("collection_id") Long collectionId,
            @RequestBody Set<OaisUniformResourceName> toBeDissociated);

    /**
     * Entry point to handle association of {@link Collection} specified by its id to other entities
     * @param collectionId {@link Collection} id
     * @param toBeAssociatedWith entities to be associated
     * @return {@link Collection} as a {@link EntityModel}
     */
    @RequestMapping(method = RequestMethod.PUT, value = "/{collection_id}/associate")
    @ResponseBody
    HttpEntity<EntityModel<Collection>> associateCollections(@PathVariable("collection_id") Long collectionId,
            @RequestBody Set<OaisUniformResourceName> toBeAssociatedWith);
}
