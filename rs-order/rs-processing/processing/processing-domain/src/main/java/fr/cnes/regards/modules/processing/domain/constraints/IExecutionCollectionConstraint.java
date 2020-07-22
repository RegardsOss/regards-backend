package fr.cnes.regards.modules.processing.domain.constraints;

import io.vavr.NotImplementedError;
import io.vavr.collection.Seq;

/**
 * Constraint applicable when we need to
 * @param <T> the generic type
 */
public interface IExecutionCollectionConstraint<T> extends IExecutionSimpleConstraint<T> {

    enum CollectionConstraintType { IN, NOT_IN }

    CollectionConstraintType type();
    Seq<T> values();

    default boolean simpleCheck(T actualValue) {
        Seq<T> values = values();

        if (values.isEmpty()) { return true; }

        boolean found = values.contains(actualValue);
        switch (this.type()) {
            case IN: return found;
            case NOT_IN: return !found;
            default: throw new NotImplementedError();
        }
    }

}
