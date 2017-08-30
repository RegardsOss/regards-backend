package fr.cnes.regards.framework.file.utils;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

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
    public static InputStream getInputStream(URL source) throws IOException {
        return getInputStreamThroughProxy(source, Proxy.NO_PROXY);
    }

    public static String download(URL source, Path destination, Charset encoding, String checksumAlgorithm)
            throws IOException, NoSuchAlgorithmException {
        return downloadThroughProxy(source, destination, encoding, checksumAlgorithm, Proxy.NO_PROXY);
    }

    public static String downloadThroughProxy(URL source, Path destination, Charset encoding, String checksumAlgorithm, Proxy proxy)
            throws IOException, NoSuchAlgorithmException {
        BufferedWriter writer = Files
                .newBufferedWriter(destination, encoding, StandardOpenOption.CREATE);
        InputStream sourceStream = DownloadUtils.getInputStreamThroughProxy(source, proxy);
        int read;
        while ((read = sourceStream.read()) != -1) {
            writer.write(read);
        }
        writer.flush();
        writer.close();
        // Now that it is stored, lets checked that it is correctly stored!
        InputStream is = Files.newInputStream(destination);
        DigestInputStream dis = new DigestInputStream(is, MessageDigest.getInstance(checksumAlgorithm));
        while (dis.read() != -1) {
        }
        dis.close();
        return ChecksumUtils.getHexChecksum(dis.getMessageDigest().digest());
    }

    /**
     *
     * @param source
     * @param proxy
     * @return
     * @throws IOException
     */
    public static InputStream getInputStreamThroughProxy(URL source, Proxy proxy) throws IOException {
        URLConnection connection = source.openConnection(proxy);
        connection.setDoInput(true); //that's the default but lets set it explicitly for understanding
        connection.connect();
        return connection.getInputStream();
    }

}
