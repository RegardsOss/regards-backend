/*
 * LICENSE_PLACEHOLDER
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
import fr.cnes.regards.modules.entities.domain.AbstractEntity;
import fr.cnes.regards.modules.search.domain.IRepresentation;

/**
 * @author Sylvain Vissiere-Guerinet
 */
@Plugin(author = "REGARDS Team", contact = "regards@c-s.fr",
        description = "Representation plugin handling GeoJSON serialization", id = "GeoJsonRepresentation",
        licence = "GPLv3", owner = "CSSI", url = "https://github.com/RegardsOss", version = "0.0.1")
public class GeoJsonRepresentation implements IRepresentation {

    @Autowired
    private Gson gson;

    @Override
    public MediaType getHandledMediaType() {
        return new MediaType("application", "geo+json", StandardCharsets.UTF_8);
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

}
