/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.notification.service.utils;

import java.util.ArrayList;
import java.util.List;

import org.springframework.hateoas.Resource;
import org.springframework.http.HttpEntity;

/**
 * General utility methods for working with {@link HttpEntity}s and {@link Resource}s.
 *
 * @author CS SI
 */
public final class RestUnwrapper {

    /**
     * Utility classes must not have public or default constructor
     */
    private RestUnwrapper() {

    }

    /**
     * Unwraps a controller's reponse, traversing the {@link HttpEntity} and the {@link Resource}.
     *
     * @param pWrapped
     *            The resource wrapped in the same way as rest controllers do
     * @param <T>
     *            The wrapped resource type
     * @return The unwrapped resource
     */
    public static <T> T unwrap(final HttpEntity<Resource<T>> pWrapped) {
        return pWrapped.getBody().getContent();
    }

    /**
     * Unwraps a controller's reponse, traversing the {@link HttpEntity}, exploding the {@link List}, traversing the
     * {@link Resource} and rebuilding the final {@link List}.
     *
     * @param pWrapped
     *            A list of resources wrapped in the same way as rest controllers do
     * @param <T>
     *            The wrapped resource type
     * @return The unwrapped list of resources
     */
    public static <T> List<T> unwrapList(final HttpEntity<List<Resource<T>>> pWrapped) {
        final List<T> result = new ArrayList<>();
        for (final Resource<T> r : pWrapped.getBody()) {
            result.add(r.getContent());
        }
        return result;
    }
}
