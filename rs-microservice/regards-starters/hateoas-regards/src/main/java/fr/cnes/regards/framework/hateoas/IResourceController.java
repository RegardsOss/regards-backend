/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.hateoas;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.hateoas.Resource;
import org.springframework.util.Assert;

/**
 * Single resource interface
 *
 * @param <T>
 *            data transfer object (whatever object)
 * @author msordi
 *
 */
@FunctionalInterface
public interface IResourceController<T> {

    /**
     * Convert an element to a {@link Resource}
     *
     * @param pElement
     *            element to convert
     * @return a {@link Resource}
     */
    Resource<T> toResource(T pElement);

    /**
     * Convert a list of elements to a list of {@link Resource}
     *
     * @param pElements
     *            list of elements to convert
     * @return a list of {@link Resource}
     */
    default List<Resource<T>> toResources(final List<T> pElements) {
        Assert.notNull(pElements);
        return pElements.stream().map(this::toResource).collect(Collectors.toList());
    }
}
