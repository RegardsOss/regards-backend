/* Copyright 2017-2020 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.processing.utils.gson;

import com.google.gson.JsonDeserializer;
import com.google.gson.JsonSerializer;

/**
 * This interface is meant to be used as a target for ServiceLoader, so that
 * other components can independently declare new type adapters to be loaded
 * by ProcessingGsonUtils.
 *
 * @param <T> the generic type
 * @author Guillaume Andrieu
 */
public interface TypedGsonTypeAdapter<T> {

    Class<T> type();

    JsonDeserializer<T> deserializer();

    JsonSerializer<T> serializer();

}
