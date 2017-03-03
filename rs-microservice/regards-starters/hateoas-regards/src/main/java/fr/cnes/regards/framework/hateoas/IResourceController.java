/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.hateoas;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.PagedResources;
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
     * @param pExtras
     *            Extra URL path parameters for links
     * @return a {@link Resource}
     */
    Resource<T> toResource(T pElement, Object... pExtras);

    /**
     * Convert a list of elements to a list of {@link Resource}
     *
     * @param pElements
     *            list of elements to convert
     * @param pExtras
     *            Extra URL path parameters for links
     * @return a list of {@link Resource}
     */
    default List<Resource<T>> toResources(final Collection<T> pElements, final Object... pExtras) {
        Assert.notNull(pElements);
        return pElements.stream().map(resource -> toResource(resource, pExtras)).collect(Collectors.toList());
    }

    /**
     * Convert a list of elements to a list of {@link Resource}
     *
     * @param pElements
     *            list of elements to convert
     * @param pExtras
     *            Extra URL path parameters for links
     * @return a list of {@link Resource}
     */
    default PagedResources<Resource<T>> toPagedResources(final Page<T> pElements,
            final PagedResourcesAssembler<T> pAssembler, final Object... pExtras) {
        Assert.notNull(pElements);
        final PagedResources<Resource<T>> pageResources = pAssembler.toResource(pElements);
        pageResources.forEach(resource -> resource.add(toResource(resource.getContent(), pExtras).getLinks()));
        return pageResources;
    }
}
