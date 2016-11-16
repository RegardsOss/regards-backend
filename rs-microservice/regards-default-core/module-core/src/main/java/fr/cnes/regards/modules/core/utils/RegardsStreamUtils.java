/*
 * LICENSE_PLACEHOLDER
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
