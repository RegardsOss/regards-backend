package fr.cnes.regards.framework.file.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import com.google.common.io.ByteStreams;

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

    /**
     * works as {@link DownloadUtils#downloadThroughProxy} without proxy.
     */
    public static String download(URL source, Path destination, String checksumAlgorithm)
            throws IOException, NoSuchAlgorithmException {
        return downloadThroughProxy(source, destination, checksumAlgorithm, Proxy.NO_PROXY);
    }

    /**
     *
     * Download from the source and write it onto the file system at the destination provided.
     * Use the provided checksumAlgorithm to calculate the checksum at the end for further verification
     *
     * @param source
     * @param destination
     * @param checksumAlgorithm
     * @param proxy
     * @return checksum, computed using the provided algorithm, of the file created at destination
     * @throws IOException
     * @throws NoSuchAlgorithmException
     */
    public static String downloadThroughProxy(URL source, Path destination, String checksumAlgorithm, Proxy proxy)
            throws IOException, NoSuchAlgorithmException {
        OutputStream os = Files.newOutputStream(destination, StandardOpenOption.CREATE);
        InputStream sourceStream = DownloadUtils.getInputStreamThroughProxy(source, proxy);
        ByteStreams.copy(sourceStream, os);
        os.close();
        sourceStream.close();
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
