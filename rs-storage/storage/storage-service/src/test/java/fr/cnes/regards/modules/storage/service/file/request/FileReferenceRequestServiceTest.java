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
package fr.cnes.regards.modules.storage.service.file.request;

import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.modules.fileaccess.amqp.input.FilesReferenceEvent;
import fr.cnes.regards.modules.fileaccess.dto.request.FileReferenceRequestDto;
import fr.cnes.regards.modules.storage.domain.database.FileLocation;
import fr.cnes.regards.modules.storage.domain.database.FileReference;
import fr.cnes.regards.modules.storage.domain.database.FileReferenceMetaInfo;
import fr.cnes.regards.modules.storage.service.file.FileReferenceEventPublisher;
import fr.cnes.regards.modules.storage.service.file.FileReferenceService;
import fr.cnes.regards.modules.storage.service.location.StoragePluginConfigurationHandler;
import fr.cnes.regards.modules.storage.service.session.SessionNotifier;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.util.MimeType;
import org.springframework.validation.Validator;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Thibaud Michaudel
 **/
public class FileReferenceRequestServiceTest {

    private static final String MD5 = "MD5";

    private static final String FILE_NAME = "fileName";

    private static final long FILE_SIZE = 1000L;

    private static final String MIME_TYPE = "text/plain";

    @Mock
    private FileReferenceRequestService fileReferenceRequestService;

    @Mock
    private FileReferenceEventPublisher fileReferenceEventPublisher;

    @Mock
    private FileReferenceService fileReferenceService;

    @Mock
    private FileDeletionRequestService fileDeletionRequestService;

    @BeforeEach()
    public void init() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void test_same_url_different_checksum() {
        String storage = "testStorage";
        String existingFileUrl = "http://myhost.com/fileName.txt";
        String existingFileChecksum = "differentChecksum";

        // Service Mocks Setup
        FileReference existingFileRef = mockServices(existingFileUrl, existingFileChecksum, true, false);

        // Given
        String groupId = "testGroupId";
        String fileChecksum = "fileChecksum";
        FileReferenceRequestDto dto = FileReferenceRequestDto.build(FILE_NAME,
                                                                    fileChecksum,
                                                                    MD5,
                                                                    MIME_TYPE,
                                                                    FILE_SIZE,
                                                                    "owner",
                                                                    storage,
                                                                    existingFileUrl,
                                                                    "sessionOwner",
                                                                    "session");
        FilesReferenceEvent event = new FilesReferenceEvent(dto, groupId);

        ArgumentCaptor<String> messageArgumentCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> checksumArgumentCaptor = ArgumentCaptor.forClass(String.class);

        // When
        fileReferenceRequestService.reference(List.of(event));

        // Then
        Mockito.verify(fileReferenceEventPublisher)
               .storeError(checksumArgumentCaptor.capture(),
                           Mockito.any(Set.class),
                           Mockito.any(),
                           messageArgumentCaptor.capture(),
                           Mockito.any(Set.class));

        Assertions.assertEquals(fileChecksum,
                                checksumArgumentCaptor.getValue(),
                                "There should be an error matching the given test checksum");

        Assertions.assertTrue(messageArgumentCaptor.getValue().contains("their checksums don't match"),
                              "There should be a message indicating that the new file couldn't be added because there"
                              + " is an existing file referencing the same url but with a different cheksum");
    }

    @Test
    public void test_same_url_same_checksum() {
        String storage = "testStorage";
        String existingFileUrl = "http://myhost.com/fileName.txt";
        String existingFileChecksum = "differentChecksum";

        // Service Mocks Setup
        FileReference existingFileRef = mockServices(existingFileUrl, existingFileChecksum, true, true);

        // Given
        String groupId = "testGroupId";
        String fileChecksum = existingFileChecksum;
        FileReferenceRequestDto dto = FileReferenceRequestDto.build(FILE_NAME,
                                                                    fileChecksum,
                                                                    MD5,
                                                                    MIME_TYPE,
                                                                    FILE_SIZE,
                                                                    "owner",
                                                                    storage,
                                                                    existingFileUrl,
                                                                    "sessionOwner",
                                                                    "session");
        FilesReferenceEvent event = new FilesReferenceEvent(dto, groupId);

        ArgumentCaptor<FileReference> fileReferenceArgumentCaptor = ArgumentCaptor.forClass(FileReference.class);
        ArgumentCaptor<String> messageArgumentCaptor = ArgumentCaptor.forClass(String.class);

        // When
        fileReferenceRequestService.reference(List.of(event));

        // Then
        Mockito.verify(fileReferenceEventPublisher)
               .storeSuccess(fileReferenceArgumentCaptor.capture(),
                             messageArgumentCaptor.capture(),
                             Mockito.any(Collection.class),
                             Mockito.any());

        Assertions.assertEquals(existingFileRef,
                                fileReferenceArgumentCaptor.getValue(),
                                "There should be a success matching the existing file reference");

        Assertions.assertTrue(messageArgumentCaptor.getValue().contains("added to existing referenced file"),
                              "There should be a message indicating that a new owner was added to the existing file reference");
    }

    @Test
    public void test_different_url_same_checksum() {
        String storage = "testStorage";
        String existingFileUrl = "http://myhost.com/fileName.txt";
        String existingFileChecksum = "differentChecksum";

        // Service Mocks Setup
        FileReference existingFileRef = mockServices(existingFileUrl, existingFileChecksum, false, true);

        // Given
        String groupId = "testGroupId";
        String fileChecksum = existingFileChecksum;
        String url = "http://mysecondhost.com/fileName.txt";
        FileReferenceRequestDto dto = FileReferenceRequestDto.build(FILE_NAME,
                                                                    fileChecksum,
                                                                    MD5,
                                                                    MIME_TYPE,
                                                                    FILE_SIZE,
                                                                    "owner",
                                                                    storage,
                                                                    url,
                                                                    "sessionOwner",
                                                                    "session");
        FilesReferenceEvent event = new FilesReferenceEvent(dto, groupId);

        ArgumentCaptor<FileReference> fileReferenceArgumentCaptor = ArgumentCaptor.forClass(FileReference.class);
        ArgumentCaptor<String> messageArgumentCaptor = ArgumentCaptor.forClass(String.class);

        // When
        fileReferenceRequestService.reference(List.of(event));

        // Then
        Mockito.verify(fileReferenceEventPublisher)
               .storeSuccess(fileReferenceArgumentCaptor.capture(),
                             messageArgumentCaptor.capture(),
                             Mockito.any(Collection.class),
                             Mockito.any());

        Assertions.assertEquals(existingFileRef,
                                fileReferenceArgumentCaptor.getValue(),
                                "There should be a success matching the existing file reference");

        Assertions.assertTrue(messageArgumentCaptor.getValue().contains("added to existing referenced file"),
                              "There should be a message indicating that a new owner was added to the existing file reference");
    }

    @Test
    public void test_different_url_different_checksum() {
        // This is the nominal case when an actual new file is being added
        String storage = "testStorage";
        String existingFileUrl = "http://myhost.com/fileName.txt";
        String existingFileChecksum = "differentChecksum";
        String fileChecksum = "fileChecksum";
        String url = "http://mysecondhost.com/fileName.txt";

        // Service Mocks Setup
        mockServices(existingFileUrl, existingFileChecksum, url, fileChecksum, false, false);

        // Given
        String groupId = "testGroupId";
        FileReferenceRequestDto dto = FileReferenceRequestDto.build(FILE_NAME,
                                                                    fileChecksum,
                                                                    MD5,
                                                                    MIME_TYPE,
                                                                    FILE_SIZE,
                                                                    "owner",
                                                                    storage,
                                                                    url,
                                                                    "sessionOwner",
                                                                    "session");
        FilesReferenceEvent event = new FilesReferenceEvent(dto, groupId);

        ArgumentCaptor<FileReference> fileReferenceArgumentCaptor = ArgumentCaptor.forClass(FileReference.class);
        ArgumentCaptor<String> messageArgumentCaptor = ArgumentCaptor.forClass(String.class);

        // When
        fileReferenceRequestService.reference(List.of(event));

        // Then
        Mockito.verify(fileReferenceEventPublisher)
               .storeSuccess(fileReferenceArgumentCaptor.capture(),
                             messageArgumentCaptor.capture(),
                             Mockito.any(Collection.class),
                             Mockito.any());

        Assertions.assertEquals(fileChecksum,
                                fileReferenceArgumentCaptor.getValue().getMetaInfo().getChecksum(),
                                "There should be a success with matching checksum");

        Assertions.assertEquals(url,
                                fileReferenceArgumentCaptor.getValue().getLocation().getUrl(),
                                "There should be a success with matching url");

        Assertions.assertTrue(messageArgumentCaptor.getValue().startsWith("New file"),
                              "There should be a message indicating that a new file was created");
    }

    private FileReference mockServices(String existingFileUrl,
                                       String existingFileChecksum,
                                       boolean urlExists,
                                       boolean checksumExists) {
        return mockServices(existingFileUrl, existingFileChecksum, null, null, urlExists, checksumExists);
    }

    private FileReference mockServices(String existingFileUrl,
                                       String existingFileChecksum,
                                       String newFileUrl,
                                       String newFileChecksum,
                                       boolean urlExists,
                                       boolean checksumExists) {
        String storage1 = "testStorage";

        FileLocation existingFileLoc = new FileLocation(storage1, existingFileUrl, false);
        FileReferenceMetaInfo existingFileMetaInfo = new FileReferenceMetaInfo(existingFileChecksum,
                                                                               MD5,
                                                                               FILE_NAME,
                                                                               FILE_SIZE,
                                                                               MimeType.valueOf(MIME_TYPE));
        FileReference existingFileRef = new FileReference("owner", existingFileMetaInfo, existingFileLoc);

        if (checksumExists) {
            Mockito.when(fileReferenceService.search(Mockito.any(Collection.class)))
                   .thenReturn(new HashSet<>(List.of(existingFileRef)));
        }
        if (urlExists) {
            Mockito.when(fileReferenceService.searchByUrls(Mockito.any(Collection.class)))
                   .thenReturn(new HashSet<>(List.of(existingFileRef)));
        }
        if (newFileUrl != null) {
            FileLocation newFileLoc = new FileLocation(storage1, newFileUrl, false);
            FileReferenceMetaInfo newFileMetaInfo = new FileReferenceMetaInfo(newFileChecksum,
                                                                              MD5,
                                                                              FILE_NAME,
                                                                              FILE_SIZE,
                                                                              MimeType.valueOf(MIME_TYPE));
            FileReference newFileRef = new FileReference("owner", newFileMetaInfo, newFileLoc);
            Mockito.when(fileReferenceService.create(Mockito.any(Collection.class),
                                                     Mockito.any(),
                                                     Mockito.any(),
                                                     Mockito.anyBoolean())).thenReturn(newFileRef);
        }

        Mockito.when(fileDeletionRequestService.search(Mockito.any(Set.class))).thenReturn(new HashSet<>());

        fileReferenceRequestService = new FileReferenceRequestService(fileReferenceEventPublisher,
                                                                      Mockito.mock(RequestsGroupService.class),
                                                                      fileDeletionRequestService,
                                                                      fileReferenceService,
                                                                      Mockito.mock(Validator.class),
                                                                      Mockito.mock(IPluginService.class),
                                                                      Mockito.mock(StoragePluginConfigurationHandler.class),
                                                                      Mockito.mock(SessionNotifier.class));
        return existingFileRef;
    }

}
