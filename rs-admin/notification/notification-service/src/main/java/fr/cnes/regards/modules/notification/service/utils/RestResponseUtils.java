/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.notification.service.utils;

import java.util.ArrayList;
import java.util.List;

import org.springframework.hateoas.Link;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * General utility methods for working with {@link ResponseEntity}s and {@link Resource}s.
 *
 * @author xbrochar
 */
public final class RestResponseUtils {

    /**
     * Utility classes must not have public or default constructor
     */
    private RestResponseUtils() {

    }

    /**
     * Wraps an object in a {@link Resource} and a {@link ResponseEntity} the same way as rest controller would do.
     *
     * @param pToWrap
     *            The resource to wrap
     * @param pStatus
     *            The resource status code
     * @param pLinks
     *            The resource's links
     * @param <T>
     *            The resource type
     * @return The wrap resource
     */
    public static <T> ResponseEntity<Resource<T>> wrap(final T pToWrap, final HttpStatus pStatus,
            final Link... pLinks) {
        return new ResponseEntity<>(new Resource<T>(pToWrap, pLinks), pStatus);
    }

    /**
     * Wraps an object in a {@link Resource} and a {@link ResponseEntity} the same way as rest controller would do.
     *
     * @param pToWrap
     *            The resource to wrap
     * @param pStatus
     *            The resource status code
     * @param <T>
     *            The resource type
     * @return The wrap resource
     */
    public static <T> ResponseEntity<List<Resource<T>>> wrapList(final List<T> pToWrap, final HttpStatus pStatus) {
        final List<Resource<T>> asResources = new ArrayList<>();
        for (final T item : pToWrap) {
            asResources.add(new Resource<>(item));
        }
        return new ResponseEntity<>(asResources, pStatus);
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
