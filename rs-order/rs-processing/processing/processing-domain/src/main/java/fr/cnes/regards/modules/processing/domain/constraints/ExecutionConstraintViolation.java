package fr.cnes.regards.modules.processing.domain.constraints;

public class ExecutionConstraintViolation {

    private final IExecutionConstraint<?> constraint;
    private final String message;

    public ExecutionConstraintViolation(IExecutionConstraint<?> constraint, String message) {
        this.constraint = constraint;
        this.message = message;
    }

    public IExecutionConstraint<?> getConstraint() {
        return constraint;
    }

    public String getMessage() {
        return message;
    }
}
