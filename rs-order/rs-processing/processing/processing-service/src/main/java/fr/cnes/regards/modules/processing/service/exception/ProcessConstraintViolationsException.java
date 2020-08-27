package fr.cnes.regards.modules.processing.service.exception;

import fr.cnes.regards.modules.processing.domain.constraints.Violation;
import io.vavr.collection.Seq;

public class ProcessConstraintViolationsException extends Exception {

    private final Seq<Violation> violations;

    public ProcessConstraintViolationsException(Seq<Violation> violations) {
        this.violations = violations;
    }

    public Seq<Violation> getViolations() {
        return violations;
    }
}
