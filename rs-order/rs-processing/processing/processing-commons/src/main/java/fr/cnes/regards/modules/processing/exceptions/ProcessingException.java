package fr.cnes.regards.modules.processing.exceptions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;
import java.util.function.Predicate;

public abstract class ProcessingException extends RuntimeException {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessingException.class);

    protected final UUID exceptionId;
    protected final ProcessingExceptionType type;
    protected final String desc;

    public ProcessingException(ProcessingExceptionType type, String desc) {
        super();
        this.exceptionId = UUID.randomUUID();
        this.type = type;
        this.desc = desc;
    }

    public ProcessingException(ProcessingExceptionType type, String desc, Throwable throwable) {
        this(type, desc);
        LOGGER.error("Processing error {} cause by:", exceptionId, throwable);
    }

    public abstract String getMessage();

    public UUID getExceptionId() {
        return exceptionId;
    }

    public ProcessingExceptionType getType() {
        return type;
    }

    public static <T extends Throwable> Predicate<T> mustWrap() {
        return ProcessingException.class::isInstance;
    }
}
