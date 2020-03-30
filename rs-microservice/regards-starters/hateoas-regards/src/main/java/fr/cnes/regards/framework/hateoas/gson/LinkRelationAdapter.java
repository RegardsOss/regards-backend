/*
 * Copyright 2017-2020 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.framework.hateoas.gson;

import java.io.IOException;

import org.springframework.hateoas.LinkRelation;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import fr.cnes.regards.framework.gson.annotation.GsonTypeAdapter;

/**
 * @author Marc SORDI
 *
 */
@GsonTypeAdapter(adapted = LinkRelation.class)
public class LinkRelationAdapter extends TypeAdapter<LinkRelation> {

    @Override
    public void write(JsonWriter out, LinkRelation rel) throws IOException {
        if (rel == null) {
            out.nullValue();
        } else {
            out.value(rel.value());
        }

    }

    @Override
    public LinkRelation read(JsonReader in) throws IOException {
        JsonToken token = in.peek();
        if (JsonToken.NULL.equals(token)) {
            in.nextNull();
            return null;
        }
        return LinkRelation.of(in.nextString());
    }

}
