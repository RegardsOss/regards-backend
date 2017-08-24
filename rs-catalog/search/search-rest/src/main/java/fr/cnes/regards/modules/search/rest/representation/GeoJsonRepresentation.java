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
package fr.cnes.regards.modules.search.rest.representation;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.StringJoiner;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.http.MediaType;

import com.google.gson.Gson;

import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;
import fr.cnes.regards.modules.catalog.services.domain.ServiceScope;
import fr.cnes.regards.modules.catalog.services.domain.annotations.CatalogServicePlugin;
import fr.cnes.regards.modules.entities.domain.AbstractEntity;
import fr.cnes.regards.modules.models.domain.EntityType;
import fr.cnes.regards.modules.search.rest.assembler.resource.FacettedPagedResources;

/**
 * Representation plugin allowing to serialize data to GeoJson format
 *
 * @author Sylvain Vissiere-Guerinet
 */
@Plugin(author = "REGARDS Team", contact = "regards@c-s.fr",
        description = "Representation plugin handling GeoJSON serialization", id = "GeoJsonRepresentation",
        licence = "GPLv3", owner = "CSSI", url = "https://github.com/RegardsOss", version = "0.0.1")
@CatalogServicePlugin(applicationModes = { ServiceScope.ONE, ServiceScope.QUERY }, entityTypes = { EntityType.DATASET })
public class GeoJsonRepresentation implements IRepresentation {

    public static final MediaType MEDIA_TYPE = new MediaType("application", "geo+json", StandardCharsets.UTF_8);

    @Autowired
    private Gson gson;

    @Override
    public MediaType getHandledMediaType() {
        return MEDIA_TYPE;
    }

    @Override
    public byte[] transform(AbstractEntity pToBeTransformed, Charset pTargetCharset) {
        // geometric characterisation is already handled by AbstractEntity
        return gson.toJson(pToBeTransformed).getBytes(pTargetCharset);
    }

    @Override
    public byte[] transform(Collection<AbstractEntity> entities, Charset pCharset) {
        StringJoiner sj = new StringJoiner(",");
        for (AbstractEntity entity : entities) {
            sj.add(gson.toJson(entity));
        }
        String json = "{\"type\":\"FeatureCollection\",\"features\":[" + sj.toString() + "]}";
        return json.getBytes(pCharset);
    }

    @Override
    public byte[] transform(PagedResources<Resource<AbstractEntity>> pEntity, Charset pCharset) {
        // PagedResources are composed of 3 parts: metadata, links, content
        // lets handle metadata part
        String json = "{\"metadata\":";
        json += gson.toJson(pEntity.getMetadata());
        // lets handle links
        json += ",\"links\":";
        json += gson.toJson(pEntity.getLinks());
        // lets handle content
        json += ",\"content\":";
        Collection<Resource<AbstractEntity>> entities = pEntity.getContent();
        // lets serialize each entity to json
        StringJoiner sj = new StringJoiner(",");
        for (Resource<AbstractEntity> entity : entities) {
            sj.add(gson.toJson(entity));
        }
        // lets set the content now that each entity has been added
        json += "{\"type\":\"FeatureCollection\",\"features\":[" + sj.toString() + "]}";
        // lets close the json so it is a correct one
        json += "}";
        return json.getBytes(pCharset);
    }

    @Override
    public byte[] transform(Resource<AbstractEntity> pEntity, Charset pCharset) {
        // geometric characterisation is already handled by AbstractEntity
        return gson.toJson(pEntity).getBytes(pCharset);
    }

    @Override
    public byte[] transform(FacettedPagedResources<Resource<AbstractEntity>> pEntity, Charset pCharset) {
        // FacettedPagedResources are composed of 4 parts: metadata, links, facets, content
        // lets handle metadata part
        String json = "{\"metadata\":";
        json += gson.toJson(pEntity.getMetadata());
        // lets handle links
        json += ",\"links\":";
        json += gson.toJson(pEntity.getLinks());
        // lets handle facets
        json += ",\"facets\":";
        json += gson.toJson(pEntity.getFacets());
        // lets handle content
        json += ",\"content\":";
        Collection<Resource<AbstractEntity>> entities = pEntity.getContent();
        // lets serialize each entity to json
        StringJoiner sj = new StringJoiner(",");
        for (Resource<AbstractEntity> entity : entities) {
            sj.add(gson.toJson(entity));
        }
        // lets set the content now that each entity has been added
        json += "{\"type\":\"FeatureCollection\",\"features\":[" + sj.toString() + "]}";
        // lets close the json so it is a correct one
        json += "}";
        return json.getBytes(pCharset);
    }

}
