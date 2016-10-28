/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.hateoas;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.hateoas.Resource;

/**
 * Single resource interface for clients
 *
 * @param <T>
 *            data transfer object (whatever object)
 * @author xbrochar
 *
 */
public interface IResourceClient<T> {

    /**
     * Convert a resource to its content
     *
     * @param pResource
     *            element to convert
     * @return the resource's content
     */
    default T toContent(final Resource<T> pResource) {
        return pResource.getContent();
    }

    /**
     * Convert a list of {@link Resource}s to a list of its content
     *
     * @param pResources
     *            list of resources
     * @return the list of resources' content
     */
    default List<T> toContents(final List<Resource<T>> pResources) {
        return pResources.stream().map(this::toContent).collect(Collectors.toList());
    }
}
