/*
 * Copyright 2017-2021 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import java.time.OffsetDateTime;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.modules.storage.dao.FileReferenceSpecification;
import fr.cnes.regards.modules.storage.domain.database.FileReference;
import fr.cnes.regards.modules.storage.service.AbstractStorageTest;

/**
 * Test class
 *
 * @author Sébastien Binda
 *
 */
@ActiveProfiles({ "noschedule" })
@TestPropertySource(
        properties = { "spring.jpa.properties.hibernate.default_schema=storage_tests",
                "regards.storage.cache.path=target/cache", "regards.storage.cache.size.limit.ko.per.tenant:10" },
        locations = { "classpath:application-test.properties" })
public class FileReferenceServiceTest extends AbstractStorageTest {

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
        FileReference fileRef = referenceRandomFile(owner, null, "file1.test", "anywhere").get();
        OffsetDateTime afterFirstDate = OffsetDateTime.now();
        Thread.sleep(1);
        referenceRandomFile("someone-else", "Type1", "file2.test", "somewhere-else");
        referenceRandomFile("someone-else", "Type2", "file3.test", "somewhere-else");
        referenceRandomFile("someone-else", "Test", "data_4.nc", "somewhere-else");
        referenceRandomFile("someone-else", "Test", "data_5.nc", "void");
        OffsetDateTime afterEndDate = OffsetDateTime.now().plusSeconds(1);

        // Search all
        Assert.assertEquals("There should be 5 file references.", 5,
                            fileRefService.search(PageRequest.of(0, 100, Direction.ASC, "id")).getTotalElements());
        // Search by fileName
        PageRequest page = PageRequest.of(0, 100, Direction.ASC, "id");
        Assert.assertEquals("There should be one file references named file1.test.", 1, fileRefService
                .search(FileReferenceSpecification.search("file1.test", null, null, null, null, null, null, page), page)
                .getTotalElements());
        Assert.assertEquals("There should be 3 file references with name containing file", 3, fileRefService
                .search(FileReferenceSpecification.search("file", null, null, null, null, null, null, page), page)
                .getTotalElements());
        // Search by checksum
        Assert.assertEquals("There should be one file references with checksum given", 1,
                            fileRefService
                                    .search(FileReferenceSpecification.search(null, fileRef.getMetaInfo().getChecksum(),
                                                                              null, null, null, null, null, page),
                                            page)
                                    .getTotalElements());
        // Search by storage
        Assert.assertEquals("There should be 5 file references in given storages", 5,
                            fileRefService
                                    .search(FileReferenceSpecification.search(null, null, null,
                                                                              Sets.newHashSet("anywhere",
                                                                                              "somewhere-else", "void"),
                                                                              null, null, null, page),
                                            page)
                                    .getTotalElements());
        Assert.assertEquals("There should be 3 file references in given storages", 3, fileRefService
                .search(FileReferenceSpecification.search(null, null, null, Sets.newHashSet("somewhere-else"), null,
                                                          null, null, page),
                        page)
                .getTotalElements());
        // Search by type
        Assert.assertEquals("There should be 0 file references for given type", 0,
                            fileRefService
                                    .search(FileReferenceSpecification.search(null, null, Lists.newArrayList("Type0"),
                                                                              null, null, null, null, page),
                                            page)
                                    .getTotalElements());
        Assert.assertEquals("There should be 1 file references for given type", 1,
                            fileRefService
                                    .search(FileReferenceSpecification.search(null, null, Sets.newHashSet("Type2"),
                                                                              null, null, null, null, page),
                                            page)
                                    .getTotalElements());
        // Search by date
        Assert.assertEquals("There should be 5 file references for given from date", 5, fileRefService
                .search(FileReferenceSpecification.search(null, null, null, null, null, beforeDate, null, page), page)
                .getTotalElements());
        Assert.assertEquals("There should be 4 file references for given from and to date", 4,
                            fileRefService
                                    .search(FileReferenceSpecification.search(null, null, null, null, null,
                                                                              afterFirstDate, afterEndDate, page),
                                            page)
                                    .getTotalElements());

    }

}
