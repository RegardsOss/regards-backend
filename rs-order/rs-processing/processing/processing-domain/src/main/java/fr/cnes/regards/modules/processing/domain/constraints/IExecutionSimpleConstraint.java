package fr.cnes.regards.modules.processing.domain.constraints;

import io.vavr.collection.List;

/**
 * Constraint which can violated in only one way.
 *
 * @param <T> the generic type
 */
public interface IExecutionSimpleConstraint<T> extends IExecutionConstraint<T> {

    boolean simpleCheck(T actualValue);

    ExecutionConstraintViolation violationFor(T actualValue);

    default List<ExecutionConstraintViolation> check(T actualValue) {
        return simpleCheck(actualValue)
                ? List.of(violationFor(actualValue))
                : List.empty();
    }

}
