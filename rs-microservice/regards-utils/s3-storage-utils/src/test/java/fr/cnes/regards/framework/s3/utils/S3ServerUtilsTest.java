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

import fr.cnes.regards.framework.s3.client.GlacierFileStatus;
import fr.cnes.regards.framework.s3.client.S3AsyncClientReactorWrapper;
import fr.cnes.regards.framework.s3.domain.S3Server;
import fr.cnes.regards.framework.s3.domain.StorageConfig;
import fr.cnes.regards.framework.s3.exception.PatternSyntaxS3Exception;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.StorageClass;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Stephane Cortine
 */
public class S3ServerUtilsTest {

    @Test
    public void test_getKeyAndStorage() throws IOException {
        // Given
        String bucket = "bucket";
        String s3Key = "downloadFile.txt";
        URL url = createUrl("http://localhost:9000/" + bucket + File.separator + s3Key);
        S3Server s3Server = createS3Server("http://localhost:9000", "bucket", null);
        // When
        S3ServerUtils.KeyAndStorage keyAndStorage = S3ServerUtils.getKeyAndStorage(url, s3Server);
        // Then
        verify(keyAndStorage, s3Key, StorageConfig.builder(s3Server).bucket(bucket).build());
    }

    @Test
    public void test_getKeyAndStorage_with_path() throws IOException {
        // Given
        String bucket = "bucket";
        String s3Key = "path0/downloadFile.txt";
        URL url = createUrl("http://localhost:9000/" + bucket + File.separator + s3Key);
        S3Server s3Server = createS3Server("http://localhost:9000",
                                           bucket,
                                           "http[s]{0,1}://.*?:.*?/(?<"
                                           + S3Server.REGEX_GROUP_BUCKET
                                           + ">.*?)/(?<"
                                           + S3Server.REGEX_GROUP_PATHFILENAME
                                           + ">.*)");
        // When
        S3ServerUtils.KeyAndStorage keyAndStorage = S3ServerUtils.getKeyAndStorage(url, s3Server);
        // Then
        verify(keyAndStorage, s3Key, StorageConfig.builder(s3Server).bucket(bucket).build());
    }

    @Test
    public void test_getKeyAndStorage_with_path_without_bucket() throws IOException {
        // Given
        String bucket = "bucket";
        String s3Key = "path1/downloadFile.txt";
        URL url = createUrl("http://alias_enPoint.port/path0/" + bucket + File.separator + s3Key);
        S3Server s3Server = createS3Server("http://alias_enPoint.port",
                                           "",
                                           "http[s]{0,1}://(.*?)/(.*?)/(?<"
                                           + S3Server.REGEX_GROUP_BUCKET
                                           + ">.*?)/(?<"
                                           + S3Server.REGEX_GROUP_PATHFILENAME
                                           + ">.*)");
        // When
        S3ServerUtils.KeyAndStorage keyAndStorage = S3ServerUtils.getKeyAndStorage(url, s3Server);
        // Then
        verify(keyAndStorage, s3Key, StorageConfig.builder(s3Server).bucket(bucket).build());
    }

    @Test
    public void test_getKeyAndStorage_pattern_not_available() {
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
    public void test_head_restore_response() {

        // Given head response indicates that restore request is not running and restoration is not expired
        HeadObjectResponse responseMock = HeadObjectResponse.builder()
                                                            .storageClass(StorageClass.DEEP_ARCHIVE)
                                                            .restore("ongoing-request=\"False\", "
                                                                     + "expiry-date=\"Wed, 07 Sep "
                                                                     + "2033 09:45:14 GMT\"")
                                                            .build();
        // Then
        assertEquals(GlacierFileStatus.AVAILABLE,
                     S3AsyncClientReactorWrapper.checkHeadRestoreState(responseMock,
                                                                       StorageClass.STANDARD.name(),
                                                                       "file_key",
                                                                       "bucket"));

        // Given head response indicates that restore request is running
        responseMock = HeadObjectResponse.builder()
                                         .storageClass(StorageClass.DEEP_ARCHIVE)
                                         .restore("ongoing-request=\"True\"")
                                         .build();
        // Then
        assertEquals(GlacierFileStatus.RESTORE_PENDING,
                     S3AsyncClientReactorWrapper.checkHeadRestoreState(responseMock,
                                                                       StorageClass.STANDARD.name(),
                                                                       "file_key",
                                                                       "bucket"));

        // Given head response indicates that restore request is not running and restoration is expired
        responseMock = HeadObjectResponse.builder()
                                         .storageClass(StorageClass.DEEP_ARCHIVE)
                                         .restore("ongoing-request=\"False\", "
                                                  + "expiry-date=\"Sat, 07 Sep "
                                                  + "2019 "
                                                  + "09:45:14 GMT\"")
                                         .build();
        // Then
        assertEquals(GlacierFileStatus.EXPIRED,
                     S3AsyncClientReactorWrapper.checkHeadRestoreState(responseMock,
                                                                       StorageClass.STANDARD.name(),
                                                                       "file_key",
                                                                       "bucket"));

        // Given head response indicates that file stored on glacier storage class and no restoration request exists
        responseMock = HeadObjectResponse.builder().storageClass(StorageClass.DEEP_ARCHIVE).build();
        // Then
        assertEquals(GlacierFileStatus.NOT_AVAILABLE,
                     S3AsyncClientReactorWrapper.checkHeadRestoreState(responseMock,
                                                                       StorageClass.STANDARD.name(),
                                                                       "file_key",
                                                                       "bucket"));

        // Given head response indicates that file is stored on standard class (no need restoration)
        responseMock = HeadObjectResponse.builder().storageClass(StorageClass.STANDARD).build();
        // Then
        assertEquals(GlacierFileStatus.AVAILABLE,
                     S3AsyncClientReactorWrapper.checkHeadRestoreState(responseMock,
                                                                       StorageClass.STANDARD.name(),
                                                                       "file_key",
                                                                       "bucket"));

        // Given head response with invalid date format
        responseMock = HeadObjectResponse.builder()
                                         .storageClass(StorageClass.DEEP_ARCHIVE)
                                         .restore("ongoing-request=\"False\", "
                                                  + "expiry-date=\"Sat, Sep 07"
                                                  + "2019 "
                                                  + "09:45:14 GMT\"")
                                         .build();
        // Then
        assertEquals(GlacierFileStatus.NOT_AVAILABLE,
                     S3AsyncClientReactorWrapper.checkHeadRestoreState(responseMock,
                                                                       StorageClass.STANDARD.name(),
                                                                       "file_key",
                                                                       "bucket"));
    }

    private void verify(S3ServerUtils.KeyAndStorage keyAndStorage, String pathFile, StorageConfig storageConfig) {
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