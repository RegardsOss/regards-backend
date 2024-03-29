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
import fr.cnes.regards.framework.s3.domain.StorageConfigBuilder;
import fr.cnes.regards.framework.s3.dto.StorageConfigDto;
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

    public static final String S3_PATTERN_EXCEPTION_MSG_FORMAT = "S3 server pattern syntax is not valid - provided pattern [%s]";

    private static final Logger LOGGER = LoggerFactory.getLogger(S3ServerUtils.class);

    /**
     * Return the S3 server {@link S3Server} the URL belong to or null if there is no corresponding server
     *
     * @param url       the url to check
     * @param s3Servers the list of S3 servers to check
     * @return the S3 server hosting the file at the url if it's present.
     */
    public static Optional<S3Server> isUrlFromS3Server(URL url, @Nullable List<S3Server> s3Servers)
        throws PatternSyntaxS3Exception {
        return s3Servers == null ? Optional.empty() : getS3Server(url, s3Servers);
    }

    /**
     * Check if a {@link S3Server} configuration matches with the source url. By default, the comparison is based on
     * the host and port of the url. If multiple configurations are found, the bucket will be the discriminating
     * criterion.
     *
     * @param url       origin url that could be a s3 url.
     * @param s3Servers list of s3 servers currently configured.
     * @return a s3 server if a match is found.
     * @throws PatternSyntaxS3Exception if the url is invalid according to the configured S3 server pattern.
     */
    private static Optional<S3Server> getS3Server(URL url, List<S3Server> s3Servers) throws PatternSyntaxS3Exception {
        List<S3Server> matchingS3Servers = s3Servers.stream()
                                                    .filter(s3Server -> isTheSameEndPoint(s3Server.getEndpoint(),
                                                                                          url.getHost(),
                                                                                          url.getPort()))
                                                    .toList();
        Optional<S3Server> s3server;
        if (!matchingS3Servers.isEmpty()) {
            int serverSize = matchingS3Servers.size();
            if (serverSize == 1) {
                s3server = Optional.of(matchingS3Servers.get(0));
                LOGGER.info("Accessing url {} from configured S3 server {}", url, s3server.get().getEndpoint());
            } else {
                // get the expected server with the bucket name, which is unique among the servers
                try {
                    s3server = matchingS3Servers.stream()
                                                .filter(s3Server -> extractRegexpGroupFromUrl(url,
                                                                                              s3Server,
                                                                                              compileS3ServerPattern(
                                                                                                  s3Server.getPattern(),
                                                                                                  url.toString()),
                                                                                              S3Server.REGEX_GROUP_BUCKET).equals(
                                                    s3Server.getBucket()))
                                                .findFirst();
                } catch (PatternSyntaxException e) {
                    throw new PatternSyntaxS3Exception(String.format(S3_PATTERN_EXCEPTION_MSG_FORMAT, e.getPattern()),
                                                       e);
                }
            }
        } else {
            s3server = Optional.empty();
            LOGGER.info("Accessing url {} from HTTP server (not found in configured S3 server).", url);
        }
        return s3server;
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
    public static KeyAndStorage getKeyAndStorage(URL source, S3Server s3Server) throws PatternSyntaxS3Exception {
        LOGGER.trace("Available S3 server {}", s3Server);
        String pattern = s3Server.getPattern();
        Matcher matcher;
        try {
            matcher = compileS3ServerPattern(pattern, source.toString());
        } catch (PatternSyntaxException e) {
            throw new PatternSyntaxS3Exception(String.format(S3_PATTERN_EXCEPTION_MSG_FORMAT, pattern), e);
        }

        // Retrieve bucket of S3 server
        String bucket = s3Server.getBucket();
        LOGGER.debug("Bucket [{}] from the configuration file from S3 server ", bucket);
        if (StringUtils.isBlank(bucket)) {
            try {
                bucket = extractRegexpGroupFromUrl(source, s3Server, matcher, S3Server.REGEX_GROUP_BUCKET);
                s3Server.setBucket(bucket);
            } catch (PatternSyntaxException e) {
                throw new PatternSyntaxS3Exception(String.format(S3_PATTERN_EXCEPTION_MSG_FORMAT, pattern), e);
            }
        }

        // Retrieve the path with filename
        String filePath;
        try {
            filePath = extractRegexpGroupFromUrl(source, s3Server, matcher, S3Server.REGEX_GROUP_PATHFILENAME);
        } catch (PatternSyntaxException e) {
            throw new PatternSyntaxS3Exception(String.format(S3_PATTERN_EXCEPTION_MSG_FORMAT, pattern), e);
        }

        return new KeyAndStorage(filePath,
                                 new StorageConfigBuilder(s3Server.getEndpoint(),
                                                          s3Server.getRegion(),
                                                          s3Server.getKey(),
                                                          s3Server.getSecret()).bucket(bucket)
                                                                               .maxRetriesNumber(s3Server.getMaxRetriesNumber())
                                                                               .retryBackOffMaxDuration(s3Server.getRetryBackOffMaxDuration())
                                                                               .retryBackOffBaseDuration(s3Server.getRetryBackOffBaseDuration())
                                                                               .build());

    }

    /**
     * Get the corresponding {@link Matcher} from the provided url with the regexp pattern
     */
    private static Matcher compileS3ServerPattern(String pattern, String sourceUrl) {
        if (StringUtils.isBlank(pattern)) {
            throw new PatternSyntaxException(
                "Input pattern of regex is empty from the configuration file from S3 server",
                pattern,
                0);
        }
        Matcher matcher = Pattern.compile(pattern).matcher(sourceUrl);
        if (!matcher.find()) {
            throw new PatternSyntaxException(String.format(
                "No match found for url %s from the S3Server pattern configuration.",
                sourceUrl), pattern, 0);
        }
        return matcher;
    }

    /**
     * Generic method to extract a group from the provided url with the corresponding matcher.
     */
    private static String extractRegexpGroupFromUrl(URL source,
                                                    S3Server s3Server,
                                                    Matcher matcher,
                                                    String requiredGroup) {
        String value = matcher.group(requiredGroup);
        if (StringUtils.isBlank(value)) {
            throw new PatternSyntaxException(String.format("Could not retrieve %s from url %s", requiredGroup, source),
                                             s3Server.getPattern(),
                                             0);
        }
        return value;
    }

    /**
     * Record with an S3 key and the S3 storage configuration
     *
     * @param key           an s3 key in S3 server(path with filename)
     * @param storageConfig an s3 storage configuration
     */
    public record KeyAndStorage(String key,
                                StorageConfigDto storageConfig) {
        //NOSONAR
    }

}
