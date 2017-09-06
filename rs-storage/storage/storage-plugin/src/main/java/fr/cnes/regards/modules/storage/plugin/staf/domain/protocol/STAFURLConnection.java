package fr.cnes.regards.modules.storage.plugin.staf.domain.protocol;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;

public class STAFURLConnection extends URLConnection {

    protected STAFURLConnection(URL pUrl) {
        super(pUrl);
    }

    @Override
    public void connect() throws IOException {
        // TODO
    }

}
