/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.jpa.utils;

import java.util.ArrayList;
import java.util.List;

/**
 * Utilities for {@link Iterable}
 *
 * @author Marc Sordi
 *
 */
public final class IterableUtils {

    public static <T> List<T> toList(final Iterable<T> pIterable) {
        List<T> target = new ArrayList<>();
        if (pIterable != null) {
            pIterable.forEach (target::add);
        }
        return target;
    }
}
