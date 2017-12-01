package fr.cnes.regards.modules.storage.domain;

/**
 * Storage runtime exception
 * @author Sylvain VISSIERE-GUERINET
 */
@SuppressWarnings("serial")
public class StorageException extends RuntimeException {

    /**
     * Constructor inherited from {@link RuntimeException}
     * @param message
     */
    public StorageException(String message) {
        super(message);
    }

    /**
     * Constructor inherited from {@link RuntimeException}
     */
    public StorageException() {
        super();
    }

    /**
     * Constructor inherited from {@link RuntimeException}
     * @param pMessage
     * @param pCause
     * @param pEnableSuppression
     * @param pWritableStackTrace
     */
    public StorageException(String pMessage, Throwable pCause, boolean pEnableSuppression,
            boolean pWritableStackTrace) {
        super(pMessage, pCause, pEnableSuppression, pWritableStackTrace);
    }

    /**
     * Constructor inherited from {@link RuntimeException}
     * @param pMessage
     * @param pCause
     */
    public StorageException(String pMessage, Throwable pCause) {
        super(pMessage, pCause);
    }

    /**
     * Constructor inherited from {@link RuntimeException}
     * @param pCause
     */
    public StorageException(Throwable pCause) {
        super(pCause);
    }

}
