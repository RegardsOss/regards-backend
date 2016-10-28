/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.hateoas;

import java.util.ArrayList;
import java.util.List;

import org.springframework.hateoas.Link;
import org.springframework.hateoas.Resource;
import org.springframework.http.ResponseEntity;

/**
 * General utility methods for working with {@link ResponseEntity}s and {@link Resource}s.
 *
 * @author xbrochar
 */
public final class HateoasUtils {

    /**
     * Utility classes must not have public or default constructor
     */
    private HateoasUtils() {

    }

    /**
     * Wraps an object in a {@link Resource}.
     *
     * @param pToWrap
     *            The resource to wrap
     * @param pLinks
     *            The resource's links
     * @param <T>
     *            The resource type
     * @return The wrap resource
     */
    public static <T> Resource<T> wrap(final T pToWrap, final Link... pLinks) {
        return new Resource<>(pToWrap, pLinks);
    }

    /**
     * Wraps an list of objects in a list {@link Resource}s.
     *
     * @param pToWrap
     *            The resource to wrap
     * @param <T>
     *            The resource type
     * @return The wrap resource
     */
    public static <T> List<Resource<T>> wrapList(final List<T> pToWrap) {
        final List<Resource<T>> asResources = new ArrayList<>();
        for (final T item : pToWrap) {
            asResources.add(new Resource<>(item));
        }
        return asResources;
    }

    /**
     * Unwraps a {@link Resource}.
     *
     * @param pWrapped
     *            The wrapped resource
     * @param <T>
     *            The wrapped resource type
     * @return The unwrapped resource
     */
    public static <T> T unwrap(final Resource<T> pWrapped) {
        return pWrapped.getContent();
    }

    /**
     * Unwraps a {@link List} of {@link Resource}s.
     *
     * @param pWrapped
     *            A list of resources
     * @param <T>
     *            The wrapped resource type
     * @return The unwrapped list of resources
     */
    public static <T> List<T> unwrapList(final List<Resource<T>> pWrapped) {
        final List<T> result = new ArrayList<>();
        for (final Resource<T> r : pWrapped) {
            result.add(r.getContent());
        }
        return result;
    }
}
