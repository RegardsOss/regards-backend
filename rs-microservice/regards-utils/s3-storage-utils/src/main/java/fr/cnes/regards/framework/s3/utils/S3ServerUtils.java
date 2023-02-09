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
package fr.cnes.regards.framework.s3.utils;

import fr.cnes.regards.framework.s3.domain.S3Server;
import fr.cnes.regards.framework.s3.domain.StorageConfig;
import fr.cnes.regards.framework.s3.exception.PatternSyntaxS3Exception;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.utils.StringUtils;

import javax.annotation.Nullable;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * @author Stephane Cortine
 */
public final class S3ServerUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(S3ServerUtils.class);

    /**
     * Return the S3 server {@link S3Server} the URL belong to or null if there is no corresponding server
     *
     * @param url       the url to check
     * @param s3Servers the list of S3 servers to check
     * @return the S3 server hosting the file at the url if it's present.
     */
    public static Optional<S3Server> isUrlFromS3Server(URL url, @Nullable List<S3Server> s3Servers) {
        return s3Servers == null ?
            Optional.empty() :
            s3Servers.stream()
                     .filter(s3Server -> isTheSameEndPoint(s3Server.getEndpoint(), url.getHost(), url.getPort()))
                     .findFirst();
    }

    private static boolean isTheSameEndPoint(String endPoint, String hostUrl, int portUrl) {
        try {
            URL url = new URL(endPoint);

            return url.getHost().equals(hostUrl) && url.getPort() == portUrl;
        } catch (MalformedURLException e) {
            LOGGER.error("This url {} is invalid", endPoint, e);
            return false;
        }
    }

    /**
     * Use the given server and source to build the S3 storage configuration and key to be used during S3 server operations
     * The bucket is in the given S3 server; otherwise in the URL in using pattern of given S3 server.
     * The key is in the URL in using pattern of given S3 server.
     *
     * @param source   the file url on the server
     * @param s3Server the S3 server configuration
     * @return a record with the s3 key for the file and the storage
     */
    public static KeyAndStorage getKeyAndStorage(URL source, S3Server s3Server)
        throws MalformedURLException, PatternSyntaxS3Exception {
        String pattern = s3Server.getPattern();
        if (StringUtils.isBlank(pattern)) {
            throw new PatternSyntaxS3Exception(
                "Input pattern of regex is empty from the configuration file from S3 server");
        }
        LOGGER.trace("Available S3 server {}", s3Server);
        Matcher matcher;
        try {
            matcher = Pattern.compile(pattern).matcher(source.toString());
            matcher.find();
        } catch (PatternSyntaxException e) {
            throw new PatternSyntaxS3Exception(String.format(
                "Input pattern syntax of regex is not correct from the configuration file from S3 server - pattern [%s]",
                pattern), e);
        }

        // Retrieve bucket of S3 server
        String bucket = s3Server.getBucket();
        LOGGER.debug("Bucket [{}] from the configuration file from S3 server ", bucket);
        if (StringUtils.isBlank(bucket)) {
            try {
                bucket = matcher.group(S3Server.REGEX_GROUP_BUCKET);
                LOGGER.debug("Bucket [{}] from the url [{}] for S3 server", bucket, source.toString());
            } catch (IllegalStateException | IndexOutOfBoundsException e) {//NOSONAR
                throw new MalformedURLException(String.format("Could not retrieve bucket from url %s using pattern [%s]",
                                                              source,
                                                              pattern));
            }

        }

        // Retrieve the path with filename
        String filePath;
        try {
            filePath = matcher.group(S3Server.REGEX_GROUP_PATHFILENAME);
            LOGGER.debug("File path [{}] from the url [{}] for S3 server.", filePath, source.toString());
        } catch (IllegalStateException | IndexOutOfBoundsException e) {//NOSONAR
            throw new MalformedURLException(String.format("Could not retrieve filename from url %s using pattern [%s]",
                                                          source,
                                                          pattern));
        }
        return new KeyAndStorage(filePath,
                                 StorageConfig.builder(s3Server.getEndpoint(),
                                                       s3Server.getRegion(),
                                                       s3Server.getKey(),
                                                       s3Server.getSecret()).bucket(bucket).build());

    }

    /**
     * Record with an S3 key and the S3 storage configuration
     *
     * @param key           an s3 key in S3 server(path with filename)
     * @param storageConfig an s3 storage configuration
     */
    public record KeyAndStorage(String key,
                                StorageConfig storageConfig) {
        //NOSONAR
    }

}
