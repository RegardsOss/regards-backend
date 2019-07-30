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
package fr.cnes.regards.framework.security.annotation;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.springframework.core.annotation.AnnotationUtils;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import fr.cnes.regards.framework.security.role.DefaultRole;

/**
 * Class ResourceAccessAdapter
 *
 * GSON adapter for annotation {@link ResourceAccess}
 * @author Sébastien Binda
 */
public class ResourceAccessAdapter extends TypeAdapter<ResourceAccess> {

    /**
     * Label for name field
     */
    protected static final String ROLE_LABEL = "role";

    /**
     * Label for description field
     */
    protected static final String DESCRIPTION_LABEL = "description";

    @Override
    public void write(final JsonWriter pOut, final ResourceAccess pValue) throws IOException {

        pOut.beginObject();
        pOut.name(ROLE_LABEL).value(pValue.role().toString());
        pOut.name(DESCRIPTION_LABEL).value(pValue.description());
        pOut.endObject();
    }

    @Override
    public ResourceAccess read(final JsonReader pIn) throws IOException {
        final Map<String, Object> attributs = new HashMap<>();

        pIn.beginObject();
        while (pIn.hasNext()) {
            switch (pIn.nextName()) {
                case ROLE_LABEL:
                    attributs.put(ROLE_LABEL, DefaultRole.valueOf(pIn.nextString()));
                    break;
                case DESCRIPTION_LABEL:
                    attributs.put(DESCRIPTION_LABEL, pIn.nextString());
                    break;
                default:
                    break;
            }
        }
        pIn.endObject();

        return AnnotationUtils.synthesizeAnnotation(attributs, ResourceAccess.class, null);
    }

    /**
     * Instanciate a new ResourceAccess annotation
     * @param pDescription description
     * @param pDefaultRole default role
     * @return {@link ResourceAccess}
     */
    public static ResourceAccess createResourceAccess(final String pDescription, final DefaultRole pDefaultRole) {
        final Map<String, Object> attributs = new HashMap<>();
        if (pDescription != null) {
            attributs.put(DESCRIPTION_LABEL, pDescription);
        }
        if (pDefaultRole != null) {
            attributs.put(ROLE_LABEL, pDefaultRole);
        }
        return AnnotationUtils.synthesizeAnnotation(attributs, ResourceAccess.class, null);
    }

}
