package fr.cnes.regards.framework.utils.file;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.regex.Pattern;

import com.google.common.collect.Sets;
import com.google.common.io.ByteStreams;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
public final class DownloadUtils {

    private DownloadUtils() {
    }

    /**
     * Get an InputStream on a source URL with no proxy used
     */
    public static InputStream getInputStream(URL source) throws IOException {
        return getInputStreamThroughProxy(source, Proxy.NO_PROXY, Sets.newHashSet());
    }

    /**
     * works as {@link DownloadUtils#downloadThroughProxy} without proxy.
     */
    public static String download(URL source, Path destination, String checksumAlgorithm)
            throws IOException, NoSuchAlgorithmException {
        return downloadThroughProxy(source, destination, checksumAlgorithm, Proxy.NO_PROXY, Sets.newHashSet(), null);
    }

    /**
     * works as {@link DownloadUtils#downloadThroughProxy} without proxy.
     */
    public static String download(URL source, Path destination, String checksumAlgorithm,
            Collection<String> nonProxyHosts, Integer pConnectTimeout) throws IOException, NoSuchAlgorithmException {
        return downloadThroughProxy(source, destination, checksumAlgorithm, Proxy.NO_PROXY, nonProxyHosts,
                                    pConnectTimeout);
    }

    /**
     * Download from the source and write it onto the file system at the destination provided.
     * Use the provided checksumAlgorithm to calculate the checksum at the end for further verification
     * @return checksum, computed using the provided algorithm, of the file created at destination
     */
    public static String downloadThroughProxy(URL source, Path destination, String checksumAlgorithm, Proxy proxy,
            Collection<String> nonProxyHosts, Integer pConnectTimeout) throws NoSuchAlgorithmException, IOException {
        try (OutputStream os = Files.newOutputStream(destination, StandardOpenOption.CREATE);
                InputStream sourceStream = DownloadUtils.getInputStreamThroughProxy(source, proxy, nonProxyHosts,
                                                                                    pConnectTimeout);
                // lets compute the checksum during the copy!
                DigestInputStream dis = new DigestInputStream(sourceStream,
                        MessageDigest.getInstance(checksumAlgorithm))) {
            ByteStreams.copy(dis, os);
            return ChecksumUtils.getHexChecksum(dis.getMessageDigest().digest());
        }
    }

    /**
     * same than {@link DownloadUtils#downloadAndCheckChecksum(URL, Path, String, String, Proxy, Collection, Integer)} with {@link Proxy#NO_PROXY} as proxy
     */
    public static boolean downloadAndCheckChecksum(URL source, Path destination, String checksumAlgorithm,
            String expectedChecksum, Integer pConnectionTimeout) throws IOException, NoSuchAlgorithmException {
        return downloadAndCheckChecksum(source, destination, checksumAlgorithm, expectedChecksum, Proxy.NO_PROXY,
                                        Sets.newHashSet(), pConnectionTimeout);
    }

    /**
     * same than {@link DownloadUtils#downloadAndCheckChecksum(URL, Path, String, String, Proxy, Collection, Integer)} with {@link Proxy#NO_PROXY} as proxy and default timeout
     */
    public static boolean downloadAndCheckChecksum(URL source, Path destination, String checksumAlgorithm,
            String expectedChecksum) throws IOException, NoSuchAlgorithmException {
        return downloadAndCheckChecksum(source, destination, checksumAlgorithm, expectedChecksum, Proxy.NO_PROXY,
                                        Sets.newHashSet(), null);
    }

    /**
     * Download a source to the provided destination using the provided proxy.
     * Checks if the checksum computed thanks to checksumAlgorithm match to the expected checksum
     * @return checksum.equals(expectedChecksum)
     */
    public static boolean downloadAndCheckChecksum(URL source, Path destination, String checksumAlgorithm,
            String expectedChecksum, Proxy proxy, Collection<String> nonProxyHosts, Integer pConnectionTimeout)
            throws IOException, NoSuchAlgorithmException {
        String checksum = downloadThroughProxy(source, destination, checksumAlgorithm, proxy, nonProxyHosts,
                                               pConnectionTimeout);
        return checksum.toLowerCase().equals(expectedChecksum.toLowerCase());
    }

    public static InputStream getInputStreamThroughProxy(URL source, Proxy proxy, Collection<String> nonProxyHosts)
            throws IOException {
        URLConnection connection;
        if (needProxy(source, nonProxyHosts)) {
            connection = source.openConnection(proxy);
        } else {
            connection = source.openConnection();
        }
        connection.setDoInput(true); //that's the default but lets set it explicitly for understanding
        connection.connect();
        return connection.getInputStream();
    }

    /**
     * @param pConnectTimeout Sets a specified timeout value, in milliseconds, to be used when opening a communications link to the resource referenced by this URLConnection
     */
    public static InputStream getInputStreamThroughProxy(URL source, Proxy proxy, Collection<String> nonProxyHosts,
            Integer pConnectTimeout) throws IOException {
        URLConnection connection;
        if (needProxy(source, nonProxyHosts)) {
            connection = source.openConnection(proxy);
        } else {
            connection = source.openConnection();
        }
        connection.setDoInput(true); //that's the default but lets set it explicitly for understanding
        if (pConnectTimeout != null) {
            connection.setConnectTimeout(pConnectTimeout);
        }
        connection.connect();

        // Handle specific case of HTTP URLs.
        if (connection instanceof HttpURLConnection) {
            HttpURLConnection conn = (HttpURLConnection) connection;
            if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
                conn.disconnect();
                throw new FileNotFoundException(
                        String.format("Error during http/https access for URL %s, got response code : %d",
                                      source.toString(), conn.getResponseCode()));
            }
        }
        return connection.getInputStream();
    }

    public static Long getContentLength(URL source, Integer pConnectTimeout) throws IOException {
        URLConnection connection = source.openConnection();
        connection.setConnectTimeout(pConnectTimeout);
        return connection.getContentLengthLong();
    }

    public static boolean needProxy(URL url, Collection<String> nonProxyHosts) {
        if (nonProxyHosts != null && !nonProxyHosts.isEmpty()) {
            return !nonProxyHosts.stream().anyMatch(host -> Pattern.matches(host, url.getHost()));
        } else {
            return true;
        }
    }

}
