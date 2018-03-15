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
package fr.cnes.regards.framework.module.manager;

import fr.cnes.regards.framework.gson.adapters.PolymorphicTypeAdapterFactory;

/**
 *
 * Informs GSON how to (de)serialize {@link ModuleConfigurationItem}.
 *
 * @author Marc Sordi
 *
 */
@Deprecated
@SuppressWarnings("rawtypes")
public class ModuleConfigurationItemAdapterFactory extends PolymorphicTypeAdapterFactory<ModuleConfigurationItem> {

    public ModuleConfigurationItemAdapterFactory() {
        super(ModuleConfigurationItem.class, "key");
    }
}

// public Gson getConfigGson() {
// if (configGson == null) {
//
// // Initialize serialization factory
// ModuleConfigurationItemAdapterFactory confItemFactory = new ModuleConfigurationItemAdapterFactory();
// if ((managers != null) && !managers.isEmpty()) {
// for (IModuleConfigurationManager manager : managers) {
// manager.configureFactory(confItemFactory);
// }
// }
//
// // Initialize specific GSON instance
// GsonBuilder customBuilder = gsonBuilderFactory.newBuilder();
// customBuilder.addSerializationExclusionStrategy(new SerializationExclusionStrategy<>(ConfigIgnore.class));
// customBuilder.registerTypeAdapterFactory(confItemFactory);
// configGson = customBuilder.create();
// }
// return configGson;
// }
