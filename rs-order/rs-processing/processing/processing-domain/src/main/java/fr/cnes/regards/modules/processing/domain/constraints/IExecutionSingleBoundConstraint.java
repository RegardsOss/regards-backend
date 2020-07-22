package fr.cnes.regards.modules.processing.domain.constraints;

import io.vavr.NotImplementedError;

public interface IExecutionSingleBoundConstraint<T extends Comparable<T>>
        extends IExecutionSimpleConstraint<T> {

    enum BoundType { MIN, MAX }

    BoundType type();
    T bound();

    default boolean simpleCheck(T actualValue) {
        switch (this.type()) {
            case MIN: return actualValue.compareTo(bound()) >= 0;
            case MAX: return actualValue.compareTo(bound()) <= 0;
            default: throw new NotImplementedError();
        }
    }

}
