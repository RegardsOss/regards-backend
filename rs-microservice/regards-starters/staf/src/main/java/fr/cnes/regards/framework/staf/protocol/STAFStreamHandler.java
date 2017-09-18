/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.staf.protocol;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

/**
 * STAF Stream handler to handle URL STAF protocole.
 *
 * @author Sébastien Binda
 *
 */
public class STAFStreamHandler extends URLStreamHandler {

    @Override
    protected URLConnection openConnection(URL pUrl) throws IOException {
        return new STAFURLConnection(pUrl);
    }

}
