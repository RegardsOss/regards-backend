/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.notification.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.hateoas.Resource;
import org.springframework.http.HttpEntity;

/**
 * General utility methods for working with {@link HttpEntity}s and {@link Resource}s.
 *
 * @author CS SI
 */
public final class HateoasUtils {

    /**
     * Utility classes must not have public or default constructor
     */
    private HateoasUtils() {

    }

    /**
     * Unwraps a controller's reponse, traversing the {@link HttpEntity}, exploding the {@link List}, traversing the
     * {@link Resource} and rebuilding the final {@link List}.
     *
     * @param pResponse
     *            A list of resources wrapped in the same way as rest controllers do
     * @param <T>
     *            The wrapped resource type
     * @return The unwrapped list of resources
     */
    public static <T> List<T> mapResponseToContent(final HttpEntity<List<Resource<T>>> pResponse) {
        return pResponse.getBody().stream().map(e -> e.getContent()).collect(Collectors.toList());
    }
}
