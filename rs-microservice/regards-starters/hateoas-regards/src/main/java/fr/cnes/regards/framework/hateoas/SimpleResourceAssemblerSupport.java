package fr.cnes.regards.framework.hateoas;

import java.util.ArrayList;
import java.util.List;

import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport;

import com.google.common.base.Preconditions;

/**
 * Alternative solution to {@link RepresentationModelAssemblerSupport}.
 * It does not require the developper to define a [PersonResource] type, which is often unnessary.
 * @param <T> The base type wrapped in {@link EntityModel}
 * @author Xavier-Alexandre Brochard
 */
public abstract class SimpleResourceAssemblerSupport<T> implements RepresentationModelAssembler<T, EntityModel<T>> {

    /**
     * Converts all given entities into resources.
     * @param entities must not be {@literal null}.
     * @see #toModel(Object)
     */
    public List<EntityModel<T>> toResources(Iterable<? extends T> entities) {

        Preconditions.checkNotNull(entities);
        List<EntityModel<T>> result = new ArrayList<>();

        for (T entity : entities) {
            result.add(toModel(entity));
        }

        return result;
    }

}
