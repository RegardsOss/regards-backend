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
import fr.cnes.regards.framework.modules.plugins.domain.parameter.CollectionPluginParam;
import fr.cnes.regards.framework.modules.plugins.domain.parameter.IPluginParam;
import fr.cnes.regards.framework.modules.plugins.domain.parameter.JsonCollectionPluginParam;
import fr.cnes.regards.framework.modules.plugins.domain.parameter.JsonMapPluginParam;
import fr.cnes.regards.framework.modules.plugins.domain.parameter.JsonObjectPluginParam;
import fr.cnes.regards.framework.modules.plugins.domain.parameter.MapPluginParam;
import fr.cnes.regards.framework.modules.plugins.domain.parameter.ObjectPluginParam;
import fr.cnes.regards.framework.modules.plugins.domain.parameter.PluginParamType;

/**
 * Gson service for plugin parameter transformation
 *
 * @author Marc SORDI
 */
public class PluginParameterTransformer {

    private static final Logger LOGGER = LoggerFactory.getLogger(PluginParameterTransformer.class);

    private static final String TRANSFO_MESSAGE = "Transforming {} parameter to {} parameter";

    private static Gson gsonInstance;

    public static void setup(Gson gson) {
        gsonInstance = gson;
        if (gsonInstance == null) {
            LOGGER.info("Configuring development GSON instance");
            GsonBuilder builder = GsonCustomizer.gsonBuilder(Optional.empty(), Optional.empty());
            gsonInstance = builder.create();
        }
    }

    public static Object getParameterValue(IPluginParam param, PluginParamType targetType) {

        // Check type transformation constistency
        if (PluginParamType.COLLECTION.equals(targetType) && PluginParamType.JSON_COLLECTION.equals(param.getType())) {
            LOGGER.debug(TRANSFO_MESSAGE, targetType, param.getType());
            return transform((JsonCollectionPluginParam) param).getValue();
        } else if (PluginParamType.MAP.equals(targetType) && PluginParamType.JSON_MAP.equals(param.getType())) {
            LOGGER.debug(TRANSFO_MESSAGE, targetType, param.getType());
            return transform((JsonMapPluginParam) param).getValue();
        } else if (PluginParamType.POJO.equals(targetType) && PluginParamType.JSON_POJO.equals(param.getType())) {
            LOGGER.debug(TRANSFO_MESSAGE, targetType, param.getType());
            return transform((JsonObjectPluginParam) param).getValue();
        } else {
            String message = String.format("Cannot transform \"%s\" parameter with name \"%s\" to \"%s\" parameter",
                                           param.getType(), param.getName(), targetType);
            LOGGER.error(message);
            throw new PluginUtilsRuntimeException(message);
        }
    }

    /**
     * Transform {@link PluginParamType#JSON_POJO} plugin parameter to {@link PluginParamType#POJO}
     */
    public static ObjectPluginParam transform(JsonObjectPluginParam source) {
        ObjectPluginParam target = new ObjectPluginParam().with(source.getName());
        target.setDynamic(source.isDynamic());
        try {
            Object o = gsonInstance.fromJson(source.getValue(), Class.forName(source.getClazz()));
            target.setValue(o);
        } catch (JsonSyntaxException | ClassNotFoundException e) {
            propagateException(e, source);
        }
        return target;
    }

    /**
     * Transform {@link PluginParamType#JSON_COLLECTION} plugin parameter to {@link PluginParamType#COLLECTION}
     */
    public static CollectionPluginParam transform(JsonCollectionPluginParam source) {
        CollectionPluginParam target = new CollectionPluginParam().with(source.getName());
        target.setDynamic(source.isDynamic());
        try {
            if (source.getValue() != null && !source.getValue().isEmpty()) {
                Collection<Object> collection = new ArrayList<>();
                for (JsonElement el : source.getValue()) {
                    Object o = gsonInstance.fromJson(el, Class.forName(source.getClazz()));
                    collection.add(o);
                }
                target.setValue(collection);
            }
        } catch (JsonSyntaxException | ClassNotFoundException e) {
            propagateException(e, source);
        }
        return target;
    }

    /**
     * Transform {@link PluginParamType#JSON_MAP} plugin parameter to {@link PluginParamType#MAP}
     */
    public static MapPluginParam transform(JsonMapPluginParam source) {
        MapPluginParam target = new MapPluginParam().with(source.getName());
        target.setDynamic(source.isDynamic());
        try {
            if (source.getValue() != null && !source.getValue().isEmpty()) {
                Map<String, Object> map = new HashMap<>();
                for (Entry<String, JsonElement> entry : source.getValue().entrySet()) {
                    Object o = gsonInstance.fromJson(entry.getValue(), Class.forName(source.getClazz()));
                    map.put(entry.getKey(), o);
                }
                target.setValue(map);
            }
        } catch (JsonSyntaxException | ClassNotFoundException e) {
            propagateException(e, source);
        }
        return target;
    }

    private static void propagateException(Throwable t, IPluginParam source) {
        String message = String.format("Cannot transform \"%s\" parameter with name \"%s\"", source.getType(),
                                       source.getName());
        LOGGER.error(message, t);
        // Propagate exception
        throw new PluginUtilsRuntimeException(message, t);
    }
}
