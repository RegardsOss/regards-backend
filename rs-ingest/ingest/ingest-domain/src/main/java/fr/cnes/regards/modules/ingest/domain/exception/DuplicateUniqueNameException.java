package fr.cnes.regards.modules.ingest.domain.exception;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
public class DuplicateUniqueNameException extends Throwable {

    public DuplicateUniqueNameException(String errorMessage) {
        super(errorMessage);
    }

    public DuplicateUniqueNameException(String message, Throwable cause) {
        super(message, cause);
    }

    public DuplicateUniqueNameException(Throwable cause) {
        super(cause);
    }
}
