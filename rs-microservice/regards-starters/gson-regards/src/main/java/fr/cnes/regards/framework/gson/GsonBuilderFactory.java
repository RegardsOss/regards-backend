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
package fr.cnes.regards.framework.gson;

import java.util.Optional;

import org.springframework.context.ApplicationContext;

import com.google.gson.GsonBuilder;

/**
 * This class allows to build a new fresh {@link GsonBuilder} with specific properties independently of the main one.
 *
 * @author Marc Sordi
 *
 */
public class GsonBuilderFactory {

    private final GsonProperties properties;

    private final ApplicationContext applicationContext;

    public GsonBuilderFactory(GsonProperties properties, ApplicationContext applicationContext) {
        this.properties = properties;
        this.applicationContext = applicationContext;
    }

    /**
     * Create a **new** GSON builder instance with generic adapters and factories.<br/>
     * Useful to create specific (de)serializer for custom purpose avoiding conflict with main builder.<br/>
     * To create the related GSON instance, just call {@link GsonBuilder#create()}
     */
    public GsonBuilder newBuilder() {
        return GsonCustomizer.gsonBuilder(Optional.of(properties), Optional.of(applicationContext));
    }
}
