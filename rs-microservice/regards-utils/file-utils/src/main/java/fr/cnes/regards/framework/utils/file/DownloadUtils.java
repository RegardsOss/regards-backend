/*
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
 *
 * This file is part of REGARDS.
 *
 * REGARDS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * REGARDS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with REGARDS. If not, see <http://www.gnu.org/licenses/>.
 */

package fr.cnes.regards.framework.utils.file;

import com.google.common.collect.Sets;
import com.google.common.io.ByteStreams;
import fr.cnes.regards.framework.s3.client.S3HighLevelReactiveClient;
import fr.cnes.regards.framework.s3.domain.*;
import fr.cnes.regards.framework.s3.exception.S3ClientException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import reactor.core.Exceptions;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
public final class DownloadUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(DownloadUtils.class);

    public static final int PIPE_SIZE = 1024 * 10;

    private DownloadUtils() {
    }

    /**
     * Check if the file in the given URL exists.
     *
     * @param source          The URL to check
     * @param proxy           the proxy to use if needed
     * @param nonProxyHosts   the list of hosts for which the proxy is not needed
     * @param pConnectTimeout the time the process will wait while trying to connect, can be null
     * @param knownS3Servers  the list of known S3 hosts, the process will use the specific s3 download algorithm if the downloaded file belong to one of these hosts
     * @return true if the file exists
     * @throws IOException                   when there is an error during the connection
     * @throws UnsupportedOperationException when the source protocol is not http or file
     */
    public static boolean exists(URL source,
                                 Proxy proxy,
                                 List<S3Server> knownS3Servers,
                                 Collection<String> nonProxyHosts,
                                 Integer pConnectTimeout) throws IOException, UnsupportedOperationException {

        URLConnection connection = getConnectionThroughProxy(source, proxy, nonProxyHosts, pConnectTimeout);

        //Filesystem
        if (source.getProtocol().equals("file")) {
            return Files.exists(Paths.get(source.getFile()));
        }

        if (source.getProtocol().equals("http") || source.getProtocol().equals("https")) {
            Optional<S3Server> s3Server = isUrlFromS3Host(source, knownS3Servers);
            if (s3Server.isPresent()) {
                //S3
                KeyAndStorage keyAndStorage = getKeyAndStorage(source, s3Server.get());
                return existsS3(keyAndStorage.key, keyAndStorage.storageConfig);
            } else {
                //Regular download
                HttpURLConnection conn = (HttpURLConnection) connection;
                conn.setRequestMethod("HEAD");
                connection.connect();
                int responseCode = conn.getResponseCode();
                conn.disconnect();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    return true;
                } else if (responseCode == HttpURLConnection.HTTP_NOT_FOUND) {
                    return false;
                } else {
                    throw new ConnectException(String.format(
                        "Error during http/https access for URL %s, got response code : %d",
                        source,
                        conn.getResponseCode()));
                }
            }
        }

        throw new UnsupportedOperationException(String.format("Unsupported protocol %s for URL %s",
                                                              source.getProtocol(),
                                                              source));

    }

    /**
     * Check if file exist on s3 sotrage
     *
     * @param key           the file to check
     * @param storageConfig the StorageConfig of the sterver
     * @return true if the file exists
     */
    private static boolean existsS3(String key, StorageConfig storageConfig) {
        S3HighLevelReactiveClient client = getS3HighLevelReactiveClient();
        StorageCommandID cmdId = new StorageCommandID(key, UUID.randomUUID());
        StorageCommand.Check check = StorageCommand.check(storageConfig, cmdId, key);
        return client.check(check).block().matchCheckResult(present -> true, absent -> false, unreachableStorage -> {
            throw new S3ClientException(unreachableStorage.getThrowable());
        });
    }

    /**
     * Get an InputStream on a source URL with no proxy used
     *
     * @param source        the source URL
     * @param knownS3Server the list of known s3 servers for s3 download, can be null
     * @return an InputStream of the source
     */
    public static InputStream getInputStream(URL source, List<S3Server> knownS3Server) throws IOException {
        return getInputStreamThroughProxy(source, Proxy.NO_PROXY, Sets.newHashSet(), knownS3Server);
    }

    /**
     * works as {@link DownloadUtils#downloadThroughProxy} without proxy.
     */
    public static String download(URL source, Path destination, String checksumAlgorithm, List<S3Server> knownS3Servers)
        throws IOException, NoSuchAlgorithmException {
        return downloadThroughProxy(source,
                                    destination,
                                    checksumAlgorithm,
                                    Proxy.NO_PROXY,
                                    Sets.newHashSet(),
                                    null,
                                    knownS3Servers);
    }

    /**
     * works as {@link DownloadUtils#downloadThroughProxy} without proxy.
     */
    public static String download(URL source,
                                  Path destination,
                                  String checksumAlgorithm,
                                  Collection<String> nonProxyHosts,
                                  Integer pConnectTimeout,
                                  List<S3Server> knownS3Servers) throws IOException, NoSuchAlgorithmException {
        return downloadThroughProxy(source,
                                    destination,
                                    checksumAlgorithm,
                                    Proxy.NO_PROXY,
                                    nonProxyHosts,
                                    pConnectTimeout,
                                    knownS3Servers);
    }

    /**
     * Download from the source and write it onto the file system at the destination provided.
     * Use the provided checksumAlgorithm to calculate the checksum at the end for further verification
     *
     * @param source            the url of the file to download
     * @param destination       the path where to store the downloaded file
     * @param checksumAlgorithm the algorithm used to compute the checksum of the downloaded file
     * @param proxy             the proxy to use if needed
     * @param nonProxyHosts     the list of hosts for which the proxy is not needed
     * @param pConnectTimeout   the time the process will wait while trying to connect, can be null
     * @param knownS3Servers    the list of known S3 hosts, the process will use the specific s3 download algorithm if the downloaded file belong to one of these hosts
     * @return checksum, computed using the provided algorithm, of the file created at destination
     */
    public static String downloadThroughProxy(URL source,
                                              Path destination,
                                              String checksumAlgorithm,
                                              Proxy proxy,
                                              Collection<String> nonProxyHosts,
                                              Integer pConnectTimeout,
                                              List<S3Server> knownS3Servers)
        throws NoSuchAlgorithmException, IOException {
        try (OutputStream os = Files.newOutputStream(destination, StandardOpenOption.CREATE);
            InputStream sourceStream = DownloadUtils.getInputStreamThroughProxy(source,
                                                                                proxy,
                                                                                nonProxyHosts,
                                                                                pConnectTimeout,
                                                                                knownS3Servers);
            // lets compute the checksum during the copy!
            DigestInputStream dis = new DigestInputStream(sourceStream, MessageDigest.getInstance(checksumAlgorithm))) {
            ByteStreams.copy(dis, os);
            return ChecksumUtils.getHexChecksum(dis.getMessageDigest().digest());
        }
    }

    /**
     * same than {@link DownloadUtils#downloadAndCheckChecksum(URL, Path, String, String, Proxy, Collection, Integer, List<S3Server>)} with {@link Proxy#NO_PROXY} as proxy
     */
    public static boolean downloadAndCheckChecksum(URL source,
                                                   Path destination,
                                                   String checksumAlgorithm,
                                                   String expectedChecksum,
                                                   Integer pConnectionTimeout,
                                                   List<S3Server> knownS3Servers)
        throws IOException, NoSuchAlgorithmException {
        return downloadAndCheckChecksum(source,
                                        destination,
                                        checksumAlgorithm,
                                        expectedChecksum,
                                        Proxy.NO_PROXY,
                                        Sets.newHashSet(),
                                        pConnectionTimeout,
                                        knownS3Servers);
    }

    /**
     * same than {@link DownloadUtils#downloadAndCheckChecksum(URL, Path, String, String, Proxy, Collection, Integer, List<S3Server>)} with {@link Proxy#NO_PROXY} as proxy and default timeout
     */
    public static boolean downloadAndCheckChecksum(URL source,
                                                   Path destination,
                                                   String checksumAlgorithm,
                                                   String expectedChecksum,
                                                   List<S3Server> knownS3Servers)
        throws IOException, NoSuchAlgorithmException {
        return downloadAndCheckChecksum(source,
                                        destination,
                                        checksumAlgorithm,
                                        expectedChecksum,
                                        Proxy.NO_PROXY,
                                        Sets.newHashSet(),
                                        null,
                                        knownS3Servers);
    }

    /**
     * Download a source to the provided destination using the provided proxy.
     * Checks if the checksum computed thanks to checksumAlgorithm match to the expected checksum
     *
     * @return checksum.equals(expectedChecksum)
     */
    public static boolean downloadAndCheckChecksum(URL source,
                                                   Path destination,
                                                   String checksumAlgorithm,
                                                   String expectedChecksum,
                                                   Proxy proxy,
                                                   Collection<String> nonProxyHosts,
                                                   Integer pConnectionTimeout,
                                                   List<S3Server> knownS3Servers)
        throws IOException, NoSuchAlgorithmException {
        String checksum = downloadThroughProxy(source,
                                               destination,
                                               checksumAlgorithm,
                                               proxy,
                                               nonProxyHosts,
                                               pConnectionTimeout,
                                               knownS3Servers);
        return checksum.toLowerCase().equals(expectedChecksum.toLowerCase());
    }

    public static InputStream getInputStreamThroughProxy(URL source,
                                                         Proxy proxy,
                                                         Collection<String> nonProxyHosts,
                                                         List<S3Server> knownS3Servers) throws IOException {
        return getInputStreamThroughProxy(source, proxy, nonProxyHosts, null, knownS3Servers);
    }

    /**
     * @param pConnectTimeout Sets a specified timeout value, in milliseconds, to be used when opening a communications link to the resource referenced by this URLConnection
     */
    public static InputStream getInputStreamThroughProxy(URL source,
                                                         Proxy proxy,
                                                         Collection<String> nonProxyHosts,
                                                         Integer pConnectTimeout,
                                                         List<S3Server> knownS3Servers) throws IOException {
        Optional<S3Server> s3Server = isUrlFromS3Host(source, knownS3Servers);
        if (s3Server.isPresent()) {
            KeyAndStorage keyAndStorage = getKeyAndStorage(source, s3Server.get());
            StorageCommandID cmdId = new StorageCommandID(keyAndStorage.key, UUID.randomUUID());
            return getInputStreamFromS3Source(keyAndStorage.key, keyAndStorage.storageConfig, cmdId);
        } else {
            return getInputStreamThroughProxyFromRegularSource(source, proxy, nonProxyHosts, pConnectTimeout);
        }

    }

    /**
     * Get an InputStream of the file to download using an s3 source
     *
     * @param entryKey      the file to download on the s3 server
     * @param storageConfig the storageConfiguration
     * @return an InputStream of the file
     */
    public static InputStream getInputStreamFromS3Source(String entryKey,
                                                         StorageConfig storageConfig,
                                                         StorageCommandID cmdId) throws IOException {
        S3HighLevelReactiveClient client = getS3HighLevelReactiveClient();

        StorageCommand.Read readCmd = StorageCommand.read(storageConfig, cmdId, entryKey);
        try {
            return client.read(readCmd)
                         .flatMap(readResult -> readResult.matchReadResult(r -> toInputStream(r),
                                                                           unreachable -> Mono.error(new S3ClientException(
                                                                               "Unreachable server: "
                                                                               + unreachable.toString())),
                                                                           notFound -> Mono.error(new FileNotFoundException(
                                                                               "Entry not found"))))
                         .block();
        } catch (Exception e) {
            Throwable unwrappedException = Exceptions.unwrap(e);
            if (unwrappedException instanceof S3ClientException s3ClientException) {
                throw s3ClientException;
            }
            if (unwrappedException instanceof FileNotFoundException fileNotFoundException) {
                throw fileNotFoundException;
            }
            throw e;
        }

    }

    /**
     * Build the s3 client
     *
     * @return the s3 client
     */
    private static S3HighLevelReactiveClient getS3HighLevelReactiveClient() {
        Scheduler scheduler = Schedulers.newParallel("s3-reactive-client", 10);
        int maxBytesPerPart = 5 * 1024 * 1024;
        S3HighLevelReactiveClient client = new S3HighLevelReactiveClient(scheduler, maxBytesPerPart, 10);
        return client;
    }

    private static Mono<InputStream> toInputStream(StorageCommandResult.ReadingPipe pipe) {
        DataBufferFactory dbf = new DefaultDataBufferFactory();
        Flux<ByteBuffer> buffers = pipe.getEntry().flatMapMany(e -> e.getData());
        PipedOutputStream outputStream = new PipedOutputStream();
        PipedInputStream inputStream = new PipedInputStream(PIPE_SIZE);
        try {
            inputStream.connect(outputStream);
        } catch (IOException e) {
            throw new RuntimeException(
                "This should never happen, the input stream was created 1 line before we tried connecting it, it cannot be already connected",
                e);
        }
        DataBufferUtils.write(buffers.map(dbf::wrap), outputStream).onErrorResume(throwable -> {
            try {
                outputStream.close();
            } catch (IOException ioe) {
            }
            return Flux.error(throwable);
        }).doOnComplete(() -> {
            try {
                outputStream.close();
            } catch (IOException ioe) {
            }
        }).subscribe();
        return Mono.just(inputStream);
    }

    public static Long getContentLength(URL source, Integer pConnectTimeout) throws IOException {
        URLConnection connection = source.openConnection();
        connection.setConnectTimeout(pConnectTimeout);
        return connection.getContentLengthLong();
    }

    /**
     * Check if proxy is needed for given url
     *
     * @param url           the url to check
     * @param nonProxyHosts the list of hosts for which a proxy is not needed
     * @return true if you need a proxy to reach the url
     */
    public static boolean needProxy(URL url, Collection<String> nonProxyHosts) {
        if (nonProxyHosts != null && !nonProxyHosts.isEmpty()) {
            return !nonProxyHosts.stream().anyMatch(host -> Pattern.matches(host, url.getHost()));
        } else {
            return true;
        }
    }

    /**
     * Return the S3Server the URL belong to or null if there is no corresponding server
     *
     * @param url       the url to check
     * @param s3Servers the list of servers to check
     * @return the server hosting the file at the url if it's present.
     */
    private static Optional<S3Server> isUrlFromS3Host(URL url, List<S3Server> s3Servers) {
        return s3Servers == null ?
            Optional.empty() :
            s3Servers.stream().filter(s -> isTheSameHost(s.getEndpoint(), url.getHost(), url.getPort())).findAny();
    }

    private static boolean isTheSameHost(String endPoint, String hostUrl, int hostPort) {
        try {
            URL url = new URL(endPoint);
            String endPointUrl = url.getHost();
            int endPointHost = url.getPort();
            return endPointUrl.equals(hostUrl) && endPointHost == hostPort;
        } catch (MalformedURLException e) {
            LOGGER.error("The url {} is invalid", endPoint, e);
            return false;
        }
    }

    /**
     * Get an InputStream of the file to download
     *
     * @param source          the file to download
     * @param proxy           the proxy to use if needed
     * @param nonProxyHosts   the list of hosts for which the proxy is not needed
     * @param pConnectTimeout the time the process will wait while trying to connect, can be null
     * @return an InputStream of the file
     * @throws IOException when there is an error during connection opening
     */
    private static InputStream getInputStreamThroughProxyFromRegularSource(URL source,
                                                                           Proxy proxy,
                                                                           Collection<String> nonProxyHosts,
                                                                           Integer pConnectTimeout) throws IOException {
        URLConnection connection = getConnectionThroughProxy(source, proxy, nonProxyHosts, pConnectTimeout);
        connection.connect();

        // Handle specific case of HTTP URLs.
        if (connection instanceof HttpURLConnection) {
            HttpURLConnection conn = (HttpURLConnection) connection;
            if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
                conn.disconnect();
                throw new FileNotFoundException(String.format(
                    "Error during http/https access for URL %s, got response code : %d",
                    source,
                    conn.getResponseCode()));
            }
        }

        return connection.getInputStream();
    }

    /**
     * Open the connection through proxy
     *
     * @param source          the url of the file where we will attempt to connect
     * @param proxy           the proxy to use if needed
     * @param nonProxyHosts   the list of hosts for which the proxy is not needed
     * @param pConnectTimeout the time the process will wait while trying to connect, can be null
     * @return the open connection ready to be connected to
     * @throws IOException when there is an error during connection opening
     */
    private static URLConnection getConnectionThroughProxy(URL source,
                                                           Proxy proxy,
                                                           Collection<String> nonProxyHosts,
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
        return connection;
    }

    /**
     * Use the given server and source to build the s3Storage and key to be used during s3 operations
     *
     * @param source   the file url on the server
     * @param s3Server the server configuration
     * @return a record with the s3 key for the file and the storage
     */
    private static KeyAndStorage getKeyAndStorage(URL source, S3Server s3Server) throws MalformedURLException {
        String bucket;
        Pattern pattern = Pattern.compile(s3Server.getPattern());
        Matcher matcher = pattern.matcher(source.toString());
        matcher.find();
        String filePath;
        try {
            filePath = matcher.group(2);
        } catch (IllegalStateException e) {
            throw new MalformedURLException(String.format("Could not retrieve filename from url %s using pattern %s",
                                                          source,
                                                          pattern));
        }
        if (StringUtils.isNotBlank(s3Server.getBucket())) {
            bucket = s3Server.getBucket();
        } else {
            bucket = matcher.group(1);
        }
        StorageConfig storageConfig = StorageConfig.builder()
                                                   .endpoint(s3Server.getEndpoint())
                                                   .bucket(bucket)
                                                   .key(s3Server.getKey())
                                                   .secret(s3Server.getSecret())
                                                   .region(s3Server.getRegion())
                                                   .build();
        KeyAndStorage keyAndStorage = new KeyAndStorage(filePath, storageConfig);
        return keyAndStorage;
    }

    /**
     * Record with an s3 key and the s3 storage config
     *
     * @param key           an s3 key
     * @param storageConfig an s3 storage configuration
     */
    private record KeyAndStorage(String key, StorageConfig storageConfig) {

    }
}
