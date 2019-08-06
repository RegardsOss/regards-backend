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
package fr.cnes.regards.framework.utils.plugins;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonSyntaxException;

import fr.cnes.regards.framework.gson.GsonCustomizer;
import fr.cnes.regards.framework.modules.plugins.domain.parameter.IPluginParam;
import fr.cnes.regards.framework.modules.plugins.domain.parameter.JsonCollectionPluginParam;
import fr.cnes.regards.framework.modules.plugins.domain.parameter.JsonMapPluginParam;
import fr.cnes.regards.framework.modules.plugins.domain.parameter.JsonObjectPluginParam;
import fr.cnes.regards.framework.modules.plugins.domain.parameter.PluginParamType;

/**
 * Gson service for plugin parameter transformation
 *
 * @author Marc SORDI
 */
public class PluginParameterTransformer {

    private static final Logger LOGGER = LoggerFactory.getLogger(PluginParameterTransformer.class);

    private static final String TRANSFO_MESSAGE = "Transforming value for parameter {} of type {}";

    private static final String SKIP_TRANSFO_MESSAGE = "Skip tranformation for parameter {}";

    private static Gson gsonInstance;

    public static void setup(Gson gson) {
        gsonInstance = gson;
        if (gsonInstance == null) {
            LOGGER.info("Configuring development GSON instance");
            GsonBuilder builder = GsonCustomizer.gsonBuilder(Optional.empty(), Optional.empty());
            gsonInstance = builder.create();
        }
    }

    /**
     * Get parameter value for complex parameter type.
     * Process value transformation on {@link JsonElement} to expected plugin pararameter JAVA type.
     *
     * @param param plugin parameter
     * @return value as a class instance
     */
    public static Object getParameterValue(IPluginParam param) {
        Object value = null;

        // Check type transformation constistency
        if (PluginParamType.COLLECTION.equals(param.getType())) {
            value = getCollectionValue(param);
        } else if (PluginParamType.MAP.equals(param.getType())) {
            value = getMapValue(param);
        } else if (PluginParamType.POJO.equals(param.getType())) {
            value = getPojoValue(param);
        } else {
            String message = String.format("Value transformation not available for parameter \"%s\" of type \"%s\"",
                                           param.getName(), param.getType());
            LOGGER.error(message);
            throw new PluginUtilsRuntimeException(message);
        }

        return value;
    }

    private static Object getCollectionValue(IPluginParam param) {
        if (JsonCollectionPluginParam.class.isAssignableFrom(param.getClass())) {
            LOGGER.debug(TRANSFO_MESSAGE, param.getName(), param.getType());
            return transformValue((JsonCollectionPluginParam) param);
        } else {
            LOGGER.debug(SKIP_TRANSFO_MESSAGE, param.getName());
            return param.getValue();
        }
    }

    private static Object getMapValue(IPluginParam param) {
        if (JsonMapPluginParam.class.isAssignableFrom(param.getClass())) {
            LOGGER.debug(TRANSFO_MESSAGE, param.getName(), param.getType());
            return transformValue((JsonMapPluginParam) param);
        } else {
            LOGGER.debug(SKIP_TRANSFO_MESSAGE, param.getName());
            return param.getValue();
        }
    }

    private static Object getPojoValue(IPluginParam param) {
        if (JsonObjectPluginParam.class.isAssignableFrom(param.getClass())) {
            LOGGER.debug(TRANSFO_MESSAGE, param.getName(), param.getType());
            return transformValue((JsonObjectPluginParam) param);
        } else {
            LOGGER.debug(SKIP_TRANSFO_MESSAGE, param.getName());
            return param.getValue();
        }
    }

    public static Object transformValue(JsonObjectPluginParam source) {
        try {
            return source.getValue() == null ? null
                    : gsonInstance.fromJson(source.getValue(), Class.forName(source.getClazz()));
        } catch (JsonSyntaxException | ClassNotFoundException e) {
            throw propagateException(e, source);
        }
    }

    public static Collection<Object> transformValue(JsonCollectionPluginParam source) {
        try {
            Collection<Object> collection = null;
            if (source.getValue() != null && !source.getValue().isEmpty()) {
                collection = new ArrayList<>();
                for (JsonElement el : source.getValue()) {
                    Object o = gsonInstance.fromJson(el, Class.forName(source.getClazz()));
                    collection.add(o);
                }
            }
            return collection;
        } catch (JsonSyntaxException | ClassNotFoundException e) {
            throw propagateException(e, source);
        }
    }

    public static Map<String, Object> transformValue(JsonMapPluginParam source) {
        try {
            Map<String, Object> map = null;
            if (source.getValue() != null && !source.getValue().isEmpty()) {
                map = new HashMap<>();
                for (Entry<String, JsonElement> entry : source.getValue().entrySet()) {
                    Object o = gsonInstance.fromJson(entry.getValue(), Class.forName(source.getClazz()));
                    map.put(entry.getKey(), o);
                }
            }
            return map;
        } catch (JsonSyntaxException | ClassNotFoundException e) {
            throw propagateException(e, source);
        }
    }

    private static PluginUtilsRuntimeException propagateException(Throwable t, IPluginParam source) {
        String message = String.format("Cannot transform \"%s\" parameter value with name \"%s\"", source.getType(),
                                       source.getName());
        LOGGER.error(message, t);
        return new PluginUtilsRuntimeException(message, t);
    }
}
