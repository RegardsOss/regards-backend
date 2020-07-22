package fr.cnes.regards.modules.processing.service.exception;

import fr.cnes.regards.modules.processing.domain.constraints.ExecutionConstraintViolation;
import io.vavr.collection.Seq;

public class ProcessConstraintViolationsException extends Exception {

    private final Seq<ExecutionConstraintViolation> violations;

    public ProcessConstraintViolationsException(Seq<ExecutionConstraintViolation> violations) {
        this.violations = violations;
    }

    public Seq<ExecutionConstraintViolation> getViolations() {
        return violations;
    }
}
