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
package fr.cnes.regards.framework.utils.plugins;

import java.util.Optional;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import fr.cnes.regards.framework.gson.GsonCustomizer;

/**
 * Utility class to manage a single Gson instance for plugin configuration management.<br/>
 * Allows Gson to be customized statically mostly for test purpose.
 *
 * @author Marc Sordi
 *
 */
public final class PluginGsonUtils {

    private static Gson instance;

    private static GsonBuilder builder;

    private PluginGsonUtils() {
        // Nothing to do
    }

    public static Gson getInstance() {
        if (instance == null) {
            synchronized (PluginGsonUtils.class) {
                instance = getBuilder().create();
            }
        }
        return instance;
    }

    /**
     * Create a basic {@link GsonBuilder} with minimal customization
     * @return {@link GsonBuilder}
     */
    private static GsonBuilder getBuilder() {
        if (builder == null) {
            synchronized (PluginGsonUtils.class) {
                builder = GsonCustomizer.gsonBuilder(Optional.empty(), Optional.empty());
            }
        }
        return builder;
    }

    /**
     * Call this method to customize Gson builder
     * @return {@link GsonBuilder}
     */
    public static GsonBuilder customizeBuilder() {
        // Reset Gson instance
        synchronized (PluginGsonUtils.class) {
            instance = null;
        }
        return getBuilder();
    }
}
