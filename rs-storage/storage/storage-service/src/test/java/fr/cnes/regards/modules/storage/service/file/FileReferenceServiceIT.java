/*
 * Copyright 2017-2024 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.storage.service.file;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.test.integration.RandomChecksumUtils;
import fr.cnes.regards.modules.storage.dao.FileReferenceSpecification;
import fr.cnes.regards.modules.storage.domain.database.FileReference;
import fr.cnes.regards.modules.storage.service.AbstractStorageIT;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Test class
 *
 * @author Sébastien Binda
 */
@ActiveProfiles({ "noscheduler" })
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=storage_tests" },
                    locations = { "classpath:application-test.properties" })
public class FileReferenceServiceIT extends AbstractStorageIT {

    private static final String SESSION_OWNER_1 = "SOURCE 1";

    private static final String SESSION_1 = "SESSION 1";

    @Before
    @Override
    public void init() throws ModuleException {
        super.init();
    }

    @Test
    public void search() throws InterruptedException {
        // 1. Add reference for search tests
        String owner = "someone";
        OffsetDateTime beforeDate = OffsetDateTime.now().minusSeconds(1);
        FileReference fileRef = referenceRandomFile(owner,
                                                    null,
                                                    "file1.test",
                                                    "anywhere",
                                                    SESSION_OWNER_1,
                                                    SESSION_1,
                                                    false).get();
        OffsetDateTime afterFirstDate = OffsetDateTime.now();
        Thread.sleep(1);
        referenceRandomFile("someone-else", "Type1", "file2.test", "somewhere-else", SESSION_OWNER_1, SESSION_1, false);
        referenceRandomFile("someone-else", "Type2", "file3.test", "somewhere-else", SESSION_OWNER_1, SESSION_1, false);
        referenceRandomFile("someone-else", "Test", "data_4.nc", "somewhere-else", SESSION_OWNER_1, SESSION_1, false);
        referenceRandomFile("someone-else", "Test", "data_5.nc", "void", SESSION_OWNER_1, SESSION_1, false);
        OffsetDateTime afterEndDate = OffsetDateTime.now().plusSeconds(1);

        // Search all
        Assert.assertEquals("There should be 5 file references.",
                            5,
                            fileRefService.search(PageRequest.of(0, 100, Direction.ASC, "id")).getTotalElements());
        // Search by fileName
        PageRequest page = PageRequest.of(0, 100, Direction.ASC, "id");
        Assert.assertEquals("There should be one file references named file1.test.",
                            1,
                            fileRefService.search(FileReferenceSpecification.search("file1.test",
                                                                                    null,
                                                                                    null,
                                                                                    null,
                                                                                    null,
                                                                                    null,
                                                                                    null,
                                                                                    page), page).getTotalElements());
        Assert.assertEquals("There should be 3 file references with name containing file",
                            3,
                            fileRefService.search(FileReferenceSpecification.search("file",
                                                                                    null,
                                                                                    null,
                                                                                    null,
                                                                                    null,
                                                                                    null,
                                                                                    null,
                                                                                    page), page).getTotalElements());
        // Search by checksum
        Assert.assertEquals("There should be one file references with checksum given",
                            1,
                            fileRefService.search(FileReferenceSpecification.search(null,
                                                                                    fileRef.getMetaInfo().getChecksum(),
                                                                                    null,
                                                                                    null,
                                                                                    null,
                                                                                    null,
                                                                                    null,
                                                                                    page), page).getTotalElements());
        // Search by storage
        Assert.assertEquals("There should be 5 file references in given storages",
                            5,
                            fileRefService.search(FileReferenceSpecification.search(null,
                                                                                    null,
                                                                                    null,
                                                                                    Sets.newHashSet("anywhere",
                                                                                                    "somewhere-else",
                                                                                                    "void"),
                                                                                    null,
                                                                                    null,
                                                                                    null,
                                                                                    page), page).getTotalElements());
        Assert.assertEquals("There should be 3 file references in given storages",
                            3,
                            fileRefService.search(FileReferenceSpecification.search(null,
                                                                                    null,
                                                                                    null,
                                                                                    Sets.newHashSet("somewhere-else"),
                                                                                    null,
                                                                                    null,
                                                                                    null,
                                                                                    page), page).getTotalElements());
        // Search by type
        Assert.assertEquals("There should be 0 file references for given type",
                            0,
                            fileRefService.search(FileReferenceSpecification.search(null,
                                                                                    null,
                                                                                    Lists.newArrayList("Type0"),
                                                                                    null,
                                                                                    null,
                                                                                    null,
                                                                                    null,
                                                                                    page), page).getTotalElements());
        Assert.assertEquals("There should be 1 file references for given type",
                            1,
                            fileRefService.search(FileReferenceSpecification.search(null,
                                                                                    null,
                                                                                    Sets.newHashSet("Type2"),
                                                                                    null,
                                                                                    null,
                                                                                    null,
                                                                                    null,
                                                                                    page), page).getTotalElements());
        // Search by date
        Assert.assertEquals("There should be 5 file references for given from date",
                            5,
                            fileRefService.search(FileReferenceSpecification.search(null,
                                                                                    null,
                                                                                    null,
                                                                                    null,
                                                                                    null,
                                                                                    beforeDate,
                                                                                    null,
                                                                                    page), page).getTotalElements());
        Assert.assertEquals("There should be 4 file references for given from and to date",
                            4,
                            fileRefService.search(FileReferenceSpecification.search(null,
                                                                                    null,
                                                                                    null,
                                                                                    null,
                                                                                    null,
                                                                                    afterFirstDate,
                                                                                    afterEndDate,
                                                                                    page), page).getTotalElements());
    }

    @Test
    public void update_file_reference_pending_action() {
        FileReference fileRef = referenceRandomFile("owner",
                                                    null,
                                                    "pending.file1.test",
                                                    NEARLINE_CONF_LABEL,
                                                    SESSION_OWNER_1,
                                                    SESSION_1,
                                                    true).get();
        Assert.assertTrue(fileRef.getLocation().isPendingActionRemaining());
        fileRefService.handleRemainingPendingActionSuccess(Sets.newHashSet(fileRef.getLocation().getUrl()));
        fileRef = fileRefService.search(fileRef.getLocation().getStorage(), fileRef.getMetaInfo().getChecksum()).get();
        Assert.assertFalse(fileRef.getLocation().isPendingActionRemaining());
    }

    @Test
    public void search_by_storage_checksums() {
        // --- GIVEN ---
        // Add reference for search tests
        Set<String> checksums = new HashSet<>();
        String owner = "someone";
        String type = "";
        String fileName = "file_";
        String storage = "somewhere";
        String sessionOwner = "sessionOwner";
        String session = "session";
        int nbFiles = 5;
        for (int i = 0; i < nbFiles; i++) {
            String checksum = RandomChecksumUtils.generateRandomChecksum();
            checksums.add(checksum);
            referenceFile(checksum, owner, type, fileName + i, storage, sessionOwner, session, false);
        }
        // reference other file
        referenceFile(UUID.randomUUID().toString(),
                      owner,
                      type,
                      fileName + "other",
                      storage,
                      sessionOwner,
                      session,
                      false);

        // --- WHEN ---
        Set<FileReference> filesReferenced = fileRefService.search(storage, checksums);

        // --- THEN ---
        Assert.assertEquals("Unexpected number of file references", nbFiles, filesReferenced.size());
        Set<String> checksumsFound = filesReferenced.stream()
                                                    .map(fileRef -> fileRef.getMetaInfo().getChecksum())
                                                    .collect(Collectors.toSet());
        Assert.assertEquals("Unexpected checksums returned", checksums, checksumsFound);
    }

}
