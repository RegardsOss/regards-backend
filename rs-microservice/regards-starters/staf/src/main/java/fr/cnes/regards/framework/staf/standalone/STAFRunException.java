/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.staf.standalone;

/**
 * Runtime exception for STAF Standalone library
 * @author Sébastien Binda
 */
public class STAFRunException extends RuntimeException {

    public STAFRunException() {
        super();
    }

    public STAFRunException(String pMessage, Throwable pCause, boolean pEnableSuppression,
            boolean pWritableStackTrace) {
        super(pMessage, pCause, pEnableSuppression, pWritableStackTrace);
    }

    public STAFRunException(String pMessage, Throwable pCause) {
        super(pMessage, pCause);
    }

    public STAFRunException(String pMessage) {
        super(pMessage);
    }

    public STAFRunException(Throwable pCause) {
        super(pCause);
    }

}
