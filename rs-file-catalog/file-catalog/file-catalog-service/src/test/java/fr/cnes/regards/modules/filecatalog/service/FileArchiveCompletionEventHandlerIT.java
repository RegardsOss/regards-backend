/*
 * Copyright 2017-2025 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.filecatalog.service;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.test.integration.RandomChecksumUtils;
import fr.cnes.regards.modules.file.packager.amqp.FileArchiveCompletionEvent;
import fr.cnes.regards.modules.fileaccess.dto.FileArchiveStatus;
import fr.cnes.regards.modules.filecatalog.dao.IFileReferenceRepository;
import fr.cnes.regards.modules.filecatalog.domain.FileLocation;
import fr.cnes.regards.modules.filecatalog.domain.FileReference;
import fr.cnes.regards.modules.filecatalog.domain.FileReferenceMetaInfo;
import fr.cnes.regards.modules.filecatalog.service.handler.FileArchiveCompletionEventHandler;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.util.MimeType;

import java.util.List;
import java.util.Optional;

/**
 * Test for {@link fr.cnes.regards.modules.filecatalog.service.handler.FileArchiveCompletionEventHandler}
 *
 * @author Thibaud Michaudel
 **/
@ActiveProfiles({ "nojobs", "noscheduler", "test" })
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=file_archive_response_test" },
                    locations = { "classpath:application-test.properties" })
public class FileArchiveCompletionEventHandlerIT extends AbstractFileCatalogIT {

    @Autowired
    private FileArchiveCompletionEventHandler fileArchiveCompletionEventHandler;

    @Autowired
    private FileReferenceService fileReferenceService;

    @Autowired
    private IFileReferenceRepository fileReferenceRepository;

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    @Before
    public void init() throws ModuleException {
        super.init();
    }

    @Test
    public void test_update_file_reference() {
        // Given
        String storage1 = "storage1";
        String checksum1 = RandomChecksumUtils.generateRandomChecksum();
        FileReference savedFileReference1 = saveNewFileReference(storage1, "file1", checksum1);

        String checksum2 = RandomChecksumUtils.generateRandomChecksum();
        FileReference savedFileReference2 = saveNewFileReference(storage1, "file2", checksum2);

        String storage2 = "storage2";
        FileReference savedFileReference3 = saveNewFileReference(storage2, "file3", checksum2);

        String checksum4 = RandomChecksumUtils.generateRandomChecksum();
        FileReference savedFileReference4 = saveNewFileReference(storage2, "file4", checksum4);

        List<FileArchiveCompletionEvent> events = List.of(new FileArchiveCompletionEvent(storage1, checksum1),
                                                          new FileArchiveCompletionEvent(storage1, checksum2),
                                                          new FileArchiveCompletionEvent(storage2, checksum4));

        // When
        fileArchiveCompletionEventHandler.handleBatch(events);

        // Then
        Optional<FileReference> foundFileReference = fileReferenceRepository.findById(savedFileReference1.getId());
        Assertions.assertTrue(foundFileReference.isPresent(), "The FileReference should still be there");
        Assertions.assertEquals(FileArchiveStatus.STORED,
                                foundFileReference.get().getLocation().getFileArchiveStatus(),
                                "The status should now be STORED");

        foundFileReference = fileReferenceRepository.findById(savedFileReference2.getId());
        Assertions.assertTrue(foundFileReference.isPresent(), "The FileReference should still be there");
        Assertions.assertEquals(FileArchiveStatus.STORED,
                                foundFileReference.get().getLocation().getFileArchiveStatus(),
                                "The status should now be STORED");

        foundFileReference = fileReferenceRepository.findById(savedFileReference3.getId());
        Assertions.assertTrue(foundFileReference.isPresent(), "The FileReference should still be there");
        Assertions.assertEquals(FileArchiveStatus.TO_STORE,
                                foundFileReference.get().getLocation().getFileArchiveStatus(),
                                "The status should still be TO_STORE");

        foundFileReference = fileReferenceRepository.findById(savedFileReference4.getId());
        Assertions.assertTrue(foundFileReference.isPresent(), "The FileReference should still be there");
        Assertions.assertEquals(FileArchiveStatus.STORED,
                                foundFileReference.get().getLocation().getFileArchiveStatus(),
                                "The status should now be STORED");

    }

    private FileReference saveNewFileReference(String storage, String file, String checksum) {
        FileReferenceMetaInfo metaInfo = new FileReferenceMetaInfo(checksum,
                                                                   "MD5",
                                                                   file,
                                                                   1000L,
                                                                   MimeType.valueOf("text/plain"));
        FileLocation fileLoc = new FileLocation(storage,
                                                "https://originurl.com/file/" + file,
                                                FileArchiveStatus.TO_STORE);

        FileReference fileRef = new FileReference("owner0", metaInfo, fileLoc);
        return fileReferenceRepository.save(fileRef);
    }
}
