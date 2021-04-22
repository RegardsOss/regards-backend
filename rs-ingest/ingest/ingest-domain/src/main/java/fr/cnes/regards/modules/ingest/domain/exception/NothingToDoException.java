package fr.cnes.regards.modules.ingest.domain.exception;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
public class NothingToDoException extends Throwable {

    public NothingToDoException(String errorMessage) {
        super(errorMessage);
    }

    public NothingToDoException(String message, Throwable cause) {
        super(message, cause);
    }

    public NothingToDoException(Throwable cause) {
        super(cause);
    }
}
