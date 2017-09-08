/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.storage.plugin.staf.domain.protocol;

import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;

/**
 * STAF URL Strzm handler factory to handle STAF URL Protocol
 * @author Sébastien Binda
 *
 */
public class STAFURLStreamHandlerFactory implements URLStreamHandlerFactory {

    @Override
    public URLStreamHandler createURLStreamHandler(String pProtocol) {
        if (STAFUrlFactory.STAF_URL_PROTOCOLE.equals(pProtocol)) {
            return new STAFStreamHandler();
        }

        return null;
    }

}
