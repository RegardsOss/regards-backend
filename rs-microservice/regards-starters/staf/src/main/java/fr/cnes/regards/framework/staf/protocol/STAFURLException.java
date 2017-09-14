/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.staf.protocol;

import fr.cnes.regards.framework.staf.exception.STAFException;

@SuppressWarnings("serial")
public class STAFURLException extends STAFException {

    public STAFURLException(String pMessage, Exception pE) {
        super(pMessage, pE);
    }

    public STAFURLException(String pMessage) {
        super(pMessage);
    }

}
