/*
 * Copyright 2017-2024 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.notifier.service.plugin;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;
import fr.cnes.regards.framework.modules.plugins.annotations.PluginParameter;
import fr.cnes.regards.modules.notifier.domain.plugin.IRuleMatcher;

import java.util.Map.Entry;

/**
 * Default plugin rule matcher
 *
 * @author Kevin Marchois
 */
@Plugin(author = "REGARDS Team",
        description = "Default rule matcher",
        id = DefaultRuleMatcher.PLUGIN_ID,
        version = "1.0.0",
        contact = "regards@c-s.fr",
        license = "GPLv3",
        owner = "CNES",
        url = "https://regardsoss.github.io/")
public class DefaultRuleMatcher implements IRuleMatcher {

    public static final String PLUGIN_ID = "DefaultRuleMatcher";

    public static final String ATTRIBUTE_TO_SEEK_FIELD_NAME = "attributeToSeek";

    public static final String ATTRIBUTE_VALUE_TO_SEEK_FIELD_NAME = "attributeValueToSeek";

    /**
     * Attribute name to seek in properties
     */
    @PluginParameter(name = ATTRIBUTE_TO_SEEK_FIELD_NAME, label = "Attribute to seek")
    private String attributeToSeek;

    /**
     * Attribute value to seek in properties
     */
    @PluginParameter(name = ATTRIBUTE_VALUE_TO_SEEK_FIELD_NAME, label = "Attribute value to seek")
    private String attributeValueToSeek;

    @Override
    public boolean match(JsonObject metadata, JsonObject payload) {
        return handleProperties(payload);
    }

    /**
     * Browse a list of properties to find the one with the name of the class attribute 'attributeToSeek'
     * and the value 'attributeValueToSeek'
     */
    private boolean handleProperties(JsonObject jsonObject) {
        if (jsonObject == null) {
            return false;
        }

        return jsonObject.entrySet().stream().anyMatch(entry -> containsAttributeToSeek(entry));
    }

    /**
     * Check if an entry match with the attributeToSeek and attributeValueToSeek
     *
     * @param entry to check
     * @return true if match, false otherwise
     */
    private boolean containsAttributeToSeek(Entry<String, JsonElement> entry) {
        if (entry.getKey().equals(attributeToSeek) && entry.getValue().getAsString().equals(attributeValueToSeek)) {
            return true;
        }
        if (entry.getValue().isJsonObject()) {
            return entry.getValue()
                        .getAsJsonObject()
                        .entrySet()
                        .stream()
                        .anyMatch(subEntry -> containsAttributeToSeek(subEntry));
        }
        return false;
    }

}
