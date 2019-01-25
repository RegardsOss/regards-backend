/*
 * Copyright 2017-2018 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.framework.gson.adapters.actuator;

import java.lang.reflect.Type;

import org.springframework.boot.actuate.web.mappings.MappingsEndpoint.ApplicationMappings;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

/**
 *
 * Spring Boot actuator deserializer for beans endpoint
 *
 * @author Marc SORDI
 *
 */
public class ApplicationMappingsAdapter implements JsonSerializer<ApplicationMappings> {

    @Override
    public JsonElement serialize(ApplicationMappings mappings, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject o = new JsonObject();
        o.add("contexts", context.serialize(mappings.getContexts()));
        return o;
    }

}
