package fr.cnes.regards.modules.storage.plugin.staf.domain.protocol;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

public class STAFStreamHandler extends URLStreamHandler {

    @Override
    protected URLConnection openConnection(URL pUrl) throws IOException {
        return new STAFURLConnection(pUrl);
    }

}
