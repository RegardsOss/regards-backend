/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.staf.exception;

/**
 * Exception for errors that occurs during tar files preparation.
 * @author Sébastien Binda
 *
 */
@SuppressWarnings("serial")
public class STAFTarException extends STAFException {

    public STAFTarException(Exception pE) {
        super(pE);
    }

    public STAFTarException(String pMessage) {
        super(pMessage);
    }

    public STAFTarException(String pMessage, Exception pE) {
        super(pMessage, pE);
    }

}
