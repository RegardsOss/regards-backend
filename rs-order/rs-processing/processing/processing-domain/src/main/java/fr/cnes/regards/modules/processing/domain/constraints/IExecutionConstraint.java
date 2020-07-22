package fr.cnes.regards.modules.processing.domain.constraints;

import io.vavr.collection.List;

/**
 * Constraints represent rights and quotas.
 */
public interface IExecutionConstraint<T> {

    /**
     * Check the given value and return a list of violations. (Empty list = value is fine.)
     *
     * @param actualValue the value to check the constraint against
     * @return a list of eventual violations
     */
    List<ExecutionConstraintViolation> check(T actualValue);

}
