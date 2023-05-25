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
package fr.cnes.regards.framework.swagger.autoconfigure.override;

import com.fasterxml.jackson.databind.JavaType;
import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.core.converter.ModelConverter;
import io.swagger.v3.core.converter.ModelConverterContext;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.models.media.Schema;

import java.util.HashMap;
import java.util.Iterator;

/**
 * Hack for classes extending HashMap : for some unknown reason, isEmpty method is included in swagger spec generation.
 * And so, during open api generation, the SessionStepProperties become an Object, and is not considered as Map.
 * Hiding with @Hidden this method solve the problem, but is ugly
 *
 * @author Thomas GUILLOU
 * @see "https://github.com/swagger-api/swagger-core/issues/3535"
 **/
public class MapAwareConverter implements ModelConverter {

    @Override
    public Schema<?> resolve(AnnotatedType type, ModelConverterContext context, Iterator<ModelConverter> chain) {
        if (chain.hasNext()) {
            Schema<?> schema = chain.next().resolve(type, context, chain);
            JavaType javaType = Json.mapper().constructType(type.getType());
            if (javaType != null) {
                Class<?> cls = javaType.getRawClass();
                if (HashMap.class.isAssignableFrom(cls)) {
                    if (schema != null && schema.getProperties() != null) {
                        schema.getProperties().remove("empty");
                        if (schema.getProperties().isEmpty()) {
                            schema.setProperties(null);
                        }
                    }
                }
            }
            return schema;
        } else {
            return null;
        }
    }
}
