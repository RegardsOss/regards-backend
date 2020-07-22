package fr.cnes.regards.modules.processing.domain.exception;

public class ExecutionFailureException extends Exception {

    public ExecutionFailureException(String s) {
        super(s);
    }

    public ExecutionFailureException(String s, Throwable throwable) {
        super(s, throwable);
    }

}
