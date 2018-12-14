package fr.cnes.regards.framework.hateoas;

import java.util.ArrayList;
import java.util.List;

import org.springframework.hateoas.Resource;
import org.springframework.hateoas.ResourceAssembler;
import org.springframework.hateoas.mvc.ResourceAssemblerSupport;

import com.google.common.base.Preconditions;

/**
 * Alternative solution to {@link ResourceAssemblerSupport}.
 * It does not require the developper to define a [PersonResource] type, which is often unnessary.
 * @param <T> The base type wrapped in {@link Resource}
 * @author Xavier-Alexandre Brochard
 */
public abstract class SimpleResourceAssemblerSupport<T> implements ResourceAssembler<T, Resource<T>> {

    /**
     * Converts all given entities into resources.
     * @param entities must not be {@literal null}.
     * @see #toResource(Object)
     */
    public List<Resource<T>> toResources(Iterable<? extends T> entities) {

        Preconditions.checkNotNull(entities);
        List<Resource<T>> result = new ArrayList<>();

        for (T entity : entities) {
            result.add(toResource(entity));
        }

        return result;
    }

}
