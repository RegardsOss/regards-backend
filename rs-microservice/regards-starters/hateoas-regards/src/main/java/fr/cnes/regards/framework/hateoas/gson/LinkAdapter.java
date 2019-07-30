/*
 * Copyright 2017-2019 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import org.springframework.hateoas.Link;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import fr.cnes.regards.framework.gson.annotation.GsonTypeAdapter;

/**
 * {@link Link} adapter
 * @author Marc Sordi
 */
@GsonTypeAdapter(adapted = Link.class)
public class LinkAdapter extends TypeAdapter<Link> {

    private static final String REL = "rel";

    private static final String HREF = "href";

    @Override
    public void write(JsonWriter out, Link link) throws IOException {
        if (link == null) {
            out.nullValue();
        } else {
            out.beginObject();
            out.name(REL).value(link.getRel());
            out.name(HREF).value(link.getHref());
            out.endObject();
        }
    }

    @Override
    public Link read(JsonReader in) throws IOException {
        String rel = null;
        String href = null;
        in.beginObject();
        while (in.hasNext()) {
            String name = in.nextName();
            if (REL.equals(name)) {
                rel = in.nextString();
            } else if (HREF.equals(name)) {
                href = in.nextString();
            } else {
                throw new UnsupportedOperationException("Unknown resource link property " + name);
            }
        }
        in.endObject();
        return new Link(href, rel);
    }

}
