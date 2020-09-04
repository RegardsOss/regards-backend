package fr.cnes.regards.modules.processing.domain.exception;

import fr.cnes.regards.modules.processing.domain.PExecution;
import fr.cnes.regards.modules.processing.exceptions.ProcessingException;
import fr.cnes.regards.modules.processing.exceptions.ProcessingExceptionType;

public class ProcessingExecutionException extends ProcessingException {

    protected final PExecution exec;

    public ProcessingExecutionException(
            ProcessingExceptionType type,
            PExecution exec,
            String message
    ) {
        super(type, message);
        this.exec = exec;
    }

    public ProcessingExecutionException(
            ProcessingExceptionType type,
            PExecution exec,
            String message,
            Throwable throwable
    ) {
        super(type, message, throwable);
        this.exec = exec;
    }

    public PExecution getExec() {
        return exec;
    }
}
