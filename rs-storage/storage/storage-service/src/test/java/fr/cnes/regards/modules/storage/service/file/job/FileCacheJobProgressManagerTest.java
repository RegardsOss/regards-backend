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
package fr.cnes.regards.modules.storage.service.file.job;

import fr.cnes.regards.framework.modules.jobs.domain.IJob;
import fr.cnes.regards.modules.storage.domain.database.FileLocation;
import fr.cnes.regards.modules.storage.domain.database.FileReference;
import fr.cnes.regards.modules.storage.domain.database.FileReferenceMetaInfo;
import fr.cnes.regards.modules.storage.domain.database.request.FileCacheRequest;
import fr.cnes.regards.modules.storage.service.file.request.FileCacheRequestService;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.MediaType;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

/**
 * @author Stephane Cortine
 */
@RunWith(MockitoJUnitRunner.class)
public class FileCacheJobProgressManagerTest {

    private static final Path TEST_ROOT_PATH = Paths.get("src/test/resources/input");

    @InjectMocks
    private FileCacheJobProgressManager fileCacheJobProgressManager;

    @Mock
    private FileCacheRequestService fileCacheRequestService;

    @Mock
    private IJob<?> job;

    @Test
    public void test_restoreSucceed_internalCache() throws MalformedURLException {
        // Given
        Path pathInputFile = TEST_ROOT_PATH.resolve("fileTest");
        Long inputFileSize = 25L;
        URL urlCacheInputFile = new URL("file", null, pathInputFile.toString());

        FileCacheRequest fileCacheRequest = createFileCacheRequest();

        // When
        fileCacheJobProgressManager.restoreSucceededInternalCache(fileCacheRequest.toDto(), pathInputFile);

        // Then
        ArgumentCaptor<Long> fileSizeCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<URL> urlCaptor = ArgumentCaptor.forClass(URL.class);

        verify(fileCacheRequestService).handleSuccessInternalCache(any(),
                                                                   urlCaptor.capture(),
                                                                   any(),
                                                                   fileSizeCaptor.capture(),
                                                                   any());
        verify(job).advanceCompletion();

        Assert.assertEquals(urlCacheInputFile, urlCaptor.getValue());
        Assert.assertEquals(inputFileSize, fileSizeCaptor.getValue());

        Assert.assertTrue(fileCacheJobProgressManager.isHandled(fileCacheRequest));
    }

    @Test
    public void test_restoreSucceed_internalCache_restored_filePath_unknown() throws MalformedURLException {
        // Given
        Path pathInputFile = TEST_ROOT_PATH.resolve("fileTest_unknown");

        FileCacheRequest fileCacheRequest = createFileCacheRequest();

        // When
        fileCacheJobProgressManager.restoreSucceededInternalCache(fileCacheRequest.toDto(), pathInputFile);

        // Then
        ArgumentCaptor<String> causeCaptor = ArgumentCaptor.forClass(String.class);

        verify(fileCacheRequestService).handleError(any(), causeCaptor.capture());
        verify(job).advanceCompletion();

        Assert.assertFalse(StringUtils.isBlank(causeCaptor.getValue()));
        Assert.assertTrue(fileCacheJobProgressManager.isHandled(fileCacheRequest));
    }

    @Test
    public void test_restoreSucceed_externalCache() throws MalformedURLException {
        // Given
        String pluginBusinessid = "plugin Business identifier";
        Long fileSize = 15L;
        fileCacheJobProgressManager.setPluginBusinessId(pluginBusinessid);

        FileCacheRequest fileCacheRequest = createFileCacheRequest();

        OffsetDateTime expirationDate = OffsetDateTime.now().plusHours(1);

        // When
        fileCacheJobProgressManager.restoreSucceededExternalCache(fileCacheRequest.toDto(),
                                                                  createFakeUrl(),
                                                                  fileSize,
                                                                  expirationDate);

        // Then
        ArgumentCaptor<Long> fileSizeCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<String> pluginBusinessIdCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<OffsetDateTime> expirationDateCaptor = ArgumentCaptor.forClass(OffsetDateTime.class);

        verify(fileCacheRequestService).handleSuccessExternalCache(any(),
                                                                   any(),
                                                                   any(),
                                                                   fileSizeCaptor.capture(),
                                                                   pluginBusinessIdCaptor.capture(),
                                                                   expirationDateCaptor.capture(),
                                                                   any());
        verify(job).advanceCompletion();

        Assert.assertEquals(pluginBusinessid, pluginBusinessIdCaptor.getValue());
        Assert.assertEquals(fileSize, fileSizeCaptor.getValue());
        Assert.assertEquals(expirationDate, expirationDateCaptor.getValue());
        Assert.assertTrue(fileCacheJobProgressManager.isHandled(fileCacheRequest));
    }

    @Test
    public void test_restoreSucceed_externalCache_without_pluginBusinessid() throws MalformedURLException {
        // Given
        Long fileSize = 15L;

        FileCacheRequest fileCacheRequest = createFileCacheRequest();

        // When
        fileCacheJobProgressManager.restoreSucceededExternalCache(fileCacheRequest.toDto(),
                                                                  createFakeUrl(),
                                                                  fileSize,
                                                                  OffsetDateTime.now());

        // Then
        ArgumentCaptor<String> causeCaptor = ArgumentCaptor.forClass(String.class);

        verify(fileCacheRequestService).handleError(any(), causeCaptor.capture());
        verify(job).advanceCompletion();

        Assert.assertFalse(StringUtils.isBlank(causeCaptor.getValue()));
        Assert.assertTrue(fileCacheJobProgressManager.isHandled(fileCacheRequest));
    }

    @Test
    public void test_restoreSucceed_externalCache_without_fileSize() throws MalformedURLException {
        // Given
        String pluginBusinessid = "plugin Business identifier";
        Long fileSize = null;
        fileCacheJobProgressManager.setPluginBusinessId(pluginBusinessid);

        FileCacheRequest fileCacheRequest = createFileCacheRequest();

        OffsetDateTime expirationDate = OffsetDateTime.now().plusHours(10);

        // When
        fileCacheJobProgressManager.restoreSucceededExternalCache(fileCacheRequest.toDto(),
                                                                  createFakeUrl(),
                                                                  fileSize,
                                                                  expirationDate);

        // Then
        ArgumentCaptor<Long> fileSizeCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<String> pluginBusinessIdCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<OffsetDateTime> expirationDateCaptor = ArgumentCaptor.forClass(OffsetDateTime.class);

        verify(fileCacheRequestService).handleSuccessExternalCache(any(),
                                                                   any(),
                                                                   any(),
                                                                   fileSizeCaptor.capture(),
                                                                   pluginBusinessIdCaptor.capture(),
                                                                   expirationDateCaptor.capture(),
                                                                   any());
        verify(job).advanceCompletion();

        Assert.assertEquals(pluginBusinessid, pluginBusinessIdCaptor.getValue());
        Assert.assertEquals(fileCacheRequest.getFileSize(), fileSizeCaptor.getValue());
        Assert.assertEquals(expirationDate, expirationDateCaptor.getValue());
        Assert.assertTrue(fileCacheJobProgressManager.isHandled(fileCacheRequest));
    }

    @Test
    public void test_restoreFailed() throws MalformedURLException {
        // Given
        String cause = "error cause";

        FileCacheRequest fileCacheRequest = createFileCacheRequest();

        // When
        fileCacheJobProgressManager.restoreFailed(fileCacheRequest.toDto(), cause);

        // Then
        ArgumentCaptor<String> causeCaptor = ArgumentCaptor.forClass(String.class);

        verify(fileCacheRequestService).handleError(any(), causeCaptor.capture());
        verify(job).advanceCompletion();

        Assert.assertEquals(cause, causeCaptor.getValue());
        Assert.assertTrue(fileCacheJobProgressManager.isHandled(fileCacheRequest));
    }

    // ---------------------
    // -- UTILITY METHODS --
    // ---------------------

    private FileCacheRequest createFileCacheRequest() {
        FileReferenceMetaInfo metaInfo = new FileReferenceMetaInfo(UUID.randomUUID().toString(),
                                                                   "UUID",
                                                                   "file.test",
                                                                   10L,
                                                                   MediaType.APPLICATION_OCTET_STREAM);
        FileLocation location = new FileLocation("nearLineStorage", "storage://plop/file", false);
        FileReference fileRef = new FileReference(Collections.singletonList("owner"), metaInfo, location);

        return new FileCacheRequest(fileRef, "restoreDirectory", 24, "group id");
    }

    private URL createFakeUrl() throws MalformedURLException {
        return new URL("http", "s3", "file.test");
    }

}
