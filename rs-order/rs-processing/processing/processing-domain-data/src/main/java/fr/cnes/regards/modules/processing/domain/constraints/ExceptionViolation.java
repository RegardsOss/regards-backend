package fr.cnes.regards.modules.processing.domain.constraints;

import lombok.Value;

@Value
public class ExceptionViolation extends Exception implements Violation {

    Throwable cause;

    @Override public String getMessage() {
        return cause.getClass().getSimpleName() + " " + cause.getMessage();
    }
}
