package fr.cnes.regards.modules.storage.domain;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
@SuppressWarnings("serial")
public class StorageException extends RuntimeException {

    public StorageException(String message) {
        super(message);
    }

    public StorageException() {
        super();
    }

    public StorageException(String pMessage, Throwable pCause, boolean pEnableSuppression,
            boolean pWritableStackTrace) {
        super(pMessage, pCause, pEnableSuppression, pWritableStackTrace);
    }

    public StorageException(String pMessage, Throwable pCause) {
        super(pMessage, pCause);
    }

    public StorageException(Throwable pCause) {
        super(pCause);
    }

}
