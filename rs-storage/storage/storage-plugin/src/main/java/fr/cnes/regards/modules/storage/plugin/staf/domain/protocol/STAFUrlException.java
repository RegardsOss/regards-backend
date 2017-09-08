/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.storage.plugin.staf.domain.protocol;

import fr.cnes.regards.framework.staf.STAFException;

@SuppressWarnings("serial")
public class STAFUrlException extends STAFException {

    public STAFUrlException(String pMessage, Exception pE) {
        super(pMessage, pE);
    }

    public STAFUrlException(String pMessage) {
        super(pMessage);
    }

}
