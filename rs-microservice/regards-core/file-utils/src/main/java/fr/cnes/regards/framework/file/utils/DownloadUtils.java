package fr.cnes.regards.framework.file.utils;

import java.io.IOException;
import java.io.InputStream;
import java.net.*;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
public class DownloadUtils {

    /**
     * Get an InputStream on a source URL with no proxy used
     * @param source
     * @return
     * @throws IOException
     */
    public static InputStream download(URL source) throws IOException {
        return downloadThroughProxy(source, Proxy.NO_PROXY);
    }

    /**
     *
     * @param source
     * @param proxy
     * @return
     * @throws IOException
     */
    public static InputStream downloadThroughProxy(URL source, Proxy proxy) throws IOException {
        URLConnection connection = source.openConnection(proxy);
        connection.setDoInput(true); //that's the default but lets set it explicitly for understanding
        connection.connect();
        return connection.getInputStream();
    }

}
