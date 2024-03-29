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
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Optional;

import static fr.cnes.regards.framework.s3.domain.S3Server.REGEX_GROUP_BUCKET;
import static fr.cnes.regards.framework.s3.domain.S3Server.REGEX_GROUP_PATHFILENAME;
import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Stephane Cortine
 */
class S3ServerUtilsTest {

    private static final String DEFAULT_PATTERN = "http[s]{0,1}://(?:.*?)/(?<"
                                                  + REGEX_GROUP_BUCKET
                                                  + ">.*?)/(?<"
                                                  + REGEX_GROUP_PATHFILENAME
                                                  + ">.*)";

    @Test
    void test_getKeyAndStorage() throws IOException {
        // Given
        String bucket = "bucket";
        String s3Key = "downloadFile.txt";
        URL url = createUrl("http://localhost:9000/" + bucket + File.separator + s3Key);
        S3Server s3Server = createS3Server("http://localhost:9000", "bucket", null);
        // When
        S3ServerUtils.KeyAndStorage keyAndStorage = S3ServerUtils.getKeyAndStorage(url, s3Server);
        // Then
        verify(keyAndStorage, s3Key, new StorageConfigBuilder(s3Server).bucket(bucket).build());
    }

    @Test
    void test_getKeyAndStorage_with_path() throws IOException {
        // Given
        String bucket = "bucket";
        String s3Key = "path0/downloadFile.txt";
        URL url = createUrl("http://localhost:9000/" + bucket + File.separator + s3Key);
        S3Server s3Server = createS3Server("http://localhost:9000",
                                           bucket,
                                           "http[s]{0,1}://.*?:.*?/(?<"
                                           + REGEX_GROUP_BUCKET
                                           + ">.*?)/(?<"
                                           + REGEX_GROUP_PATHFILENAME
                                           + ">.*)");
        // When
        S3ServerUtils.KeyAndStorage keyAndStorage = S3ServerUtils.getKeyAndStorage(url, s3Server);
        // Then
        verify(keyAndStorage, s3Key, new StorageConfigBuilder(s3Server).bucket(bucket).build());
    }

    @Test
    void test_getKeyAndStorage_with_path_without_bucket() throws IOException {
        // Given
        String bucket = "bucket";
        String s3Key = "path1/downloadFile.txt";
        URL url = createUrl("http://alias_enPoint.port/path0/" + bucket + File.separator + s3Key);
        S3Server s3Server = createS3Server("http://alias_enPoint.port",
                                           "",
                                           "http[s]{0,1}://(.*?)/(.*?)/(?<"
                                           + REGEX_GROUP_BUCKET
                                           + ">.*?)/(?<"
                                           + REGEX_GROUP_PATHFILENAME
                                           + ">.*)");
        // When
        S3ServerUtils.KeyAndStorage keyAndStorage = S3ServerUtils.getKeyAndStorage(url, s3Server);
        // Then
        verify(keyAndStorage, s3Key, new StorageConfigBuilder(s3Server).bucket(bucket).build());
    }

    @Test
    void test_getKeyAndStorage_pattern_not_available() {
        // Given
        String bucket = "bucket";
        String s3Key = "downloadFile.txt";
        URL url = createUrl("http://localhost:9000/" + bucket + File.separator + s3Key);
        S3Server s3Server = createS3Server("http://localhost:9000", bucket, "");
        // When
        // Then
        assertThrows(PatternSyntaxS3Exception.class, () -> {
            S3ServerUtils.getKeyAndStorage(url, s3Server);
        });
    }

    @Test
    void test_isUrlFromS3Server_single_matching_s3_config() throws PatternSyntaxS3Exception {
        // Given
        // create url with a single matching s3 server config
        String bucket = "bucket";
        String s3Key = "downloadFile.txt";
        URL url = createUrl(String.format("http://localhost:9000/%s/%s", bucket, s3Key));
        List<S3Server> s3Servers = List.of(createS3Server("http://localhost:9000", bucket, ""),
                                           createS3Server("http://localhost:9002", bucket, ""));

        // When
        Optional<S3Server> matchingS3Server = S3ServerUtils.isUrlFromS3Server(url, s3Servers);

        // Then
        Assertions.assertThat(matchingS3Server).as("S3 server should match url").isPresent();
        S3Server actualS3ServerFound = matchingS3Server.get();
        S3Server expectedS3Server = s3Servers.get(0);
        Assertions.assertThat(actualS3ServerFound.getEndpoint()).isEqualTo(expectedS3Server.getEndpoint());
        Assertions.assertThat(actualS3ServerFound.getBucket()).isEqualTo(expectedS3Server.getBucket());
    }

    @Test
    void test_isUrlFromS3Server_multiple_s3_config() throws PatternSyntaxS3Exception {
        // Given
        // create url with a multiple matching s3 server config. The bucket will be used as a discriminator.
        String bucket = "bucket";
        String s3Key = "downloadFile.txt";
        String baseS3Url = "http://localhost:9000";
        URL url = createUrl(String.format("%s/%s/%s", baseS3Url, bucket, s3Key));
        List<S3Server> s3Servers = List.of(createS3Server(baseS3Url, "otherBucket", DEFAULT_PATTERN),
                                           createS3Server(baseS3Url, bucket, DEFAULT_PATTERN));

        // When
        Optional<S3Server> matchingS3Server = S3ServerUtils.isUrlFromS3Server(url, s3Servers);

        // Then
        Assertions.assertThat(matchingS3Server).as("S3 server should match url").isPresent();
        S3Server actualS3ServerFound = matchingS3Server.get();
        S3Server expectedS3Server = s3Servers.get(1);
        Assertions.assertThat(actualS3ServerFound.getEndpoint()).isEqualTo(expectedS3Server.getEndpoint());
        Assertions.assertThat(actualS3ServerFound.getBucket()).isEqualTo(expectedS3Server.getBucket());
    }

    @Test
    void test_isUrlFromS3Server_multiple_s3_config_wrong_pattern() {
        // Given
        // create url with a multiple matching s3 server config. The bucket will be used as a discriminator.
        String bucket = "bucket";
        String s3Key = "downloadFile.txt";
        String baseS3Url = "http://localhost:9000";
        URL url = createUrl(String.format("%s/%s/%s", baseS3Url, bucket, s3Key));
        List<S3Server> s3Servers = List.of(createS3Server(baseS3Url, "otherBucket", ""),
                                           createS3Server(baseS3Url, bucket, DEFAULT_PATTERN));

        // When / Then
        Assertions.assertThatThrownBy(() -> S3ServerUtils.isUrlFromS3Server(url, s3Servers),
                                      "The first pattern should be invalidated")
                  .isInstanceOf(PatternSyntaxS3Exception.class);
    }

    @Test
    void test_isUrlFromS3Server_not_s3_url() throws PatternSyntaxS3Exception {
        // Given
        // create url which is not a s3 url
        URL url = createUrl("http://superhost:9000/random/path");
        List<S3Server> s3Servers = List.of(createS3Server("http://localhost:9000", "otherBucket", DEFAULT_PATTERN));

        // When
        Optional<S3Server> matchingS3Server = S3ServerUtils.isUrlFromS3Server(url, s3Servers);

        // Then
        Assertions.assertThat(matchingS3Server)
                  .as("Unexpected s3 server config returned. The url is NOT a S3 url.")
                  .isEmpty();
    }

    private void verify(S3ServerUtils.KeyAndStorage keyAndStorage, String pathFile, StorageConfigDto storageConfig) {
        assertNotNull(keyAndStorage);

        assertEquals(keyAndStorage.key(), pathFile);
        assertEquals(keyAndStorage.storageConfig(), storageConfig);
    }

    /**
     * Create settings of S3 server
     *
     * @param endPoint the endPoint of S3 server
     * @param bucket   the bucket of S3 server
     * @param pattern  the pattern to retrieve bucket, path with filename (if null, using the default pattern {@link S3Server#DEFAULT_PATTERN})
     * @return settings of S3 server with a default pattern or with the given pattern
     */
    private S3Server createS3Server(String endPoint, String bucket, String pattern) {
        if (pattern == null) {
            return new S3Server(endPoint, "region", "key", "secret", bucket);
        }
        return new S3Server(endPoint, "region", "key", "secret", bucket, pattern);
    }

    private URL createUrl(String endPoint) {
        try {
            URL url = new URL(endPoint);

            return url;
        } catch (MalformedURLException e) {
            fail();
            return null;
        }
    }

}