/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.storage.plugin.staf.domain.exception;

/**
 * Global exception for STAF library.
 * @author Sébastien Binda
 */
@SuppressWarnings("serial")
public class STAFException extends Exception {

    public STAFException(Exception e) {
        super(e);
    }

    public STAFException(String pMessage) {
        super(pMessage);
    }

    public STAFException(String pMessage, Exception pE) {
        super(pMessage, pE);
    }

}
