/*
 * Copyright 2017-2023 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.framework.s3.client;

import fr.cnes.regards.framework.s3.domain.GlacierFileStatus;
import fr.cnes.regards.framework.s3.domain.RestorationStatus;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.StorageClass;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Stephane Cortine
 */
public class S3AsyncClientReactorWrapperTest {

    @Test
    public void test_head_restore_response_AVAILABLE() {
        // Given head response indicates that restore request is not running and restoration is not expired
        HeadObjectResponse responseMock = HeadObjectResponse.builder()
                                                            .storageClass(StorageClass.DEEP_ARCHIVE)
                                                            .restore("ongoing-request=\"False\", "
                                                                     + "expiry-date=\"Wed, 07 Sep "
                                                                     + "2033 09:45:14 GMT\"")
                                                            .build();
        // When
        GlacierFileStatus glacierFileStatus = S3AsyncClientReactorWrapper.checkHeadRestoreState(responseMock,
                                                                                                StorageClass.STANDARD.name(),
                                                                                                "file_key",
                                                                                                "bucket");
        // Then
        assertEquals(RestorationStatus.AVAILABLE, glacierFileStatus.getStatus());
        assertNotNull(glacierFileStatus.getExpirationDate());
    }

    @Test
    public void test_head_restore_response_RESTORE_PENDING() {
        // Given head response indicates that restore request is running
        HeadObjectResponse responseMock = HeadObjectResponse.builder()
                                                            .storageClass(StorageClass.DEEP_ARCHIVE)
                                                            .restore("ongoing-request=\"True\"")
                                                            .build();
        // When
        GlacierFileStatus glacierFileStatus = S3AsyncClientReactorWrapper.checkHeadRestoreState(responseMock,
                                                                                                StorageClass.STANDARD.name(),
                                                                                                "file_key",
                                                                                                "bucket");

        // Then
        assertEquals(RestorationStatus.RESTORE_PENDING, glacierFileStatus.getStatus());
        assertNull(glacierFileStatus.getExpirationDate());
    }

    @Test
    public void test_head_restore_response_EXPIRED() {
        // Given head response indicates that restore request is not running and restoration is expired
        HeadObjectResponse responseMock = HeadObjectResponse.builder()
                                                            .storageClass(StorageClass.DEEP_ARCHIVE)
                                                            .restore("ongoing-request=\"False\", "
                                                                     + "expiry-date=\"Sat, 07 Sep "
                                                                     + "2019 "
                                                                     + "09:45:14 GMT\"")
                                                            .build();
        // When
        GlacierFileStatus glacierFileStatus = S3AsyncClientReactorWrapper.checkHeadRestoreState(responseMock,
                                                                                                StorageClass.STANDARD.name(),
                                                                                                "file_key",
                                                                                                "bucket");
        // Then
        assertEquals(RestorationStatus.EXPIRED, glacierFileStatus.getStatus());
        assertNotNull(glacierFileStatus.getExpirationDate());
    }

    @Test
    public void test_head_restore_response_NOT_AVAILABLE() {
        // Given head response indicates that file stored on glacier storage class and no restoration request exists
        HeadObjectResponse responseMock = HeadObjectResponse.builder().storageClass(StorageClass.DEEP_ARCHIVE).build();

        // When
        GlacierFileStatus glacierFileStatus = S3AsyncClientReactorWrapper.checkHeadRestoreState(responseMock,
                                                                                                StorageClass.STANDARD.name(),
                                                                                                "file_key",
                                                                                                "bucket");

        // Then
        assertEquals(RestorationStatus.NOT_AVAILABLE, glacierFileStatus.getStatus());
        assertNull(glacierFileStatus.getExpirationDate());

        // Given head response with invalid date format
        responseMock = HeadObjectResponse.builder()
                                         .storageClass(StorageClass.DEEP_ARCHIVE)
                                         .restore("ongoing-request=\"False\", "
                                                  + "expiry-date=\"Sat, Sep 07"
                                                  + "2019 "
                                                  + "09:45:14 GMT\"")
                                         .build();

        // When
        glacierFileStatus = S3AsyncClientReactorWrapper.checkHeadRestoreState(responseMock,
                                                                              StorageClass.STANDARD.name(),
                                                                              "file_key",
                                                                              "bucket");

        // Then
        assertEquals(RestorationStatus.NOT_AVAILABLE, glacierFileStatus.getStatus());
        assertNull(glacierFileStatus.getExpirationDate());
    }

    @Test
    public void test_head_restore_response_AVAILABLE_in_STANDARD() {
        // Given head response indicates that file is stored on standard class (no need restoration)
        Long fileSize = 100L;
        HeadObjectResponse responseMock = HeadObjectResponse.builder()
                                                            .storageClass(StorageClass.STANDARD)
                                                            .contentLength(fileSize)
                                                            .build();

        // When
        GlacierFileStatus glacierFileStatus = S3AsyncClientReactorWrapper.checkHeadRestoreState(responseMock,
                                                                                                StorageClass.STANDARD.name(),
                                                                                                "file_key",
                                                                                                "bucket");

        // Then
        assertEquals(RestorationStatus.AVAILABLE, glacierFileStatus.getStatus());
        assertNull(glacierFileStatus.getExpirationDate());
        assertEquals(fileSize, glacierFileStatus.getFileSize());
    }

}
