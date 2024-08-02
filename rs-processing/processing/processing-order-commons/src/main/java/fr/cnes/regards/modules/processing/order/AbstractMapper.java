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

package fr.cnes.regards.modules.processing.order;

import fr.cnes.regards.framework.urn.UniformResourceName;
import io.vavr.collection.Map;
import io.vavr.control.Option;
import io.vavr.control.Try;

/**
 * This class provides abstract mapping capabilities between {@code Map<String,String>} and the generic type parameter.
 *
 * @param <T> the generic type parameter converted to/from a map
 * @author gandrieu
 */
public abstract class AbstractMapper<T> {

    public abstract Map<String, String> toMap(T data);

    public abstract Option<T> fromMap(Map<String, String> map);

    protected final <T extends Enum<T>> Option<T> parse(Map<String, String> map, String name, Class<T> type) {
        return map.get(name).flatMap(str -> parse(type, str));
    }

    protected final <T extends Enum<T>> Option<T> parse(Class<T> type, String str) {
        return Try.of(() -> Enum.valueOf(type, str)).toOption();
    }

    protected final Option<Boolean> parseBoolean(Map<String, String> map, String propName) {
        return map.get(propName).flatMap(propValue -> Try.of(() -> Boolean.parseBoolean(propValue)).toOption());
    }

    protected final Option<UniformResourceName> parseUrn(Map<String, String> map, String propName) {
        return map.get(propName)
                  .flatMap(propValue -> Try.of(() -> UniformResourceName.fromString(propValue)).toOption());

    }

    protected final Option<String> parseString(Map<String, String> map, String propName) {
        return map.get(propName).flatMap(propValue -> Try.of(() -> propValue).toOption());
    }

}
