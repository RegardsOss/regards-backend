/* Copyright 2017-2024 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.framework.gson.adapters;

import com.google.gson.JsonDeserializer;
import com.google.gson.JsonSerializer;

/**
 * Gson Type adapter providing both a JsonSerializer and a JsonDeserializer.<br/>
 * Classes implementing this interface should be annotated with {@link fr.cnes.regards.framework.gson.annotation.GsonTypeAdapterFactoryBean}
 * to be taken into account by {@link fr.cnes.regards.framework.gson.autoconfigure.GsonAutoConfiguration}
 * @param <T> adapted type
 * @author gandrieu
 * @author Olivier Rousselot
 */
public interface TypedGsonTypeAdapter<T> {

    JsonDeserializer<T> deserializer();

    JsonSerializer<T> serializer();

}
