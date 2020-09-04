package fr.cnes.regards.modules.processing.exceptions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

public abstract class ProcessingException extends Exception {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessingException.class);

    protected final UUID exceptionId;
    protected final ProcessingExceptionType type;
    protected final String causeMessage;

    public ProcessingException(ProcessingExceptionType type, String message) {
        super(message);
        this.exceptionId = UUID.randomUUID();
        this.type = type;
        this.causeMessage = "";
    }

    public ProcessingException(ProcessingExceptionType type, String message, Throwable throwable) {
        super(message, throwable);
        this.exceptionId = UUID.randomUUID();
        this.type = type;
        this.causeMessage = throwable.getClass().getSimpleName() + ": " + throwable.getMessage();
        LOGGER.error("Processing error {} cause by:", exceptionId, throwable);
    }

    public UUID getExceptionId() {
        return exceptionId;
    }

    public ProcessingExceptionType getType() {
        return type;
    }

    public String getCauseMessage() {
        return causeMessage;
    }
}
