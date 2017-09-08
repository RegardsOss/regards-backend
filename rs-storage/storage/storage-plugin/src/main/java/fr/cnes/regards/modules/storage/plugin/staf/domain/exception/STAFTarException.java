/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.storage.plugin.staf.domain.exception;

import fr.cnes.regards.framework.staf.STAFException;

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

}
