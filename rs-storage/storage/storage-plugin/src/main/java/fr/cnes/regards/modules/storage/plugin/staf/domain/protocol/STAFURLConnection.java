/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.storage.plugin.staf.domain.protocol;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;

/**
 * STAF URL Connection to handle URL STAF protocole.
 *
 * @author Sébastien Binda
 *
 */
public class STAFURLConnection extends URLConnection {

    protected STAFURLConnection(URL pUrl) {
        super(pUrl);
    }

    @Override
    public void connect() throws IOException {
        throw new IOException("Unhandle staf protocol connection.");
    }

}
