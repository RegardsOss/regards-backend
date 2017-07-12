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
package fr.cnes.regards.modules.core.utils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Class RegardsStreamUtils
 *
 * Utility methods for working with {@link Stream}s.
 *
 * @author xbrochar
 */
public interface RegardsStreamUtils {

    /**
     * Return a predicate/filter in order to remove doubles (same as distinct) but with custom key extraction.<br>
     * <b>Warning: Keeps the first seen, so the order of elements in the stream does matter!</b>
     * <p/>
     * For example use as: <code>persons.stream().filter(distinctByKey(p -> p.getName());</code>
     *
     * Removes doubles based on person.name attribute.
     *
     *
     * @param pKeyExtractor
     *            The
     * @param <T>
     *            The type
     * @return A predicate
     * @see http://stackoverflow.com/questions/23699371/java-8-distinct-by-property
     */
    public static <T> Predicate<T> distinctByKey(final Function<? super T, ?> pKeyExtractor) {
        final Map<Object, Boolean> seen = new ConcurrentHashMap<>();
        return t -> seen.putIfAbsent(pKeyExtractor.apply(t), Boolean.TRUE) == null;
    }
}
