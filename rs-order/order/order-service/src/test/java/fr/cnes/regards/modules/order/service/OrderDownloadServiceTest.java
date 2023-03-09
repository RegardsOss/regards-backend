package fr.cnes.regards.modules.order.service;/*
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

import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.urn.DataType;
import fr.cnes.regards.framework.urn.EntityType;
import fr.cnes.regards.framework.urn.UniformResourceName;
import fr.cnes.regards.modules.indexer.domain.DataFile;
import fr.cnes.regards.modules.order.domain.FileState;
import fr.cnes.regards.modules.order.domain.OrderDataFile;
import fr.cnes.regards.modules.order.service.processing.IProcessingEventSender;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.util.MimeTypeUtils;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;
import java.util.zip.ZipFile;

/**
 * @author Sébastien Binda
 **/
public class OrderDownloadServiceTest {

    private final IOrderDataFileService dataFileService = Mockito.mock(IOrderDataFileService.class);

    private final IOrderJobService orderJobService = Mockito.mock(IOrderJobService.class);

    private final IRuntimeTenantResolver runtimeTenantResolver = Mockito.mock(IRuntimeTenantResolver.class);

    private final IProcessingEventSender processingEventSender = Mockito.mock(IProcessingEventSender.class);

    private OrderDownloadService service;

    @Before
    public void init() {
        service = new OrderDownloadService(null,
                                           dataFileService,
                                           orderJobService,
                                           null,
                                           null,
                                           null,
                                           null,
                                           runtimeTenantResolver,
                                           processingEventSender);
        service.afterPropertiesSet();
    }

    @Test
    public void test_download_zip() throws IOException {
        // GIVEN
        List<OrderDataFile> files = List.of(initFile("1", "file1_ql_sd.txt", "123"),
                                            initFile("2", "file1_ql_md.txt", "1234"),
                                            initFile("3", "file1_ql_hd.txt", "12345"));

        // WHEN
        try (FileOutputStream os = new FileOutputStream("target/test.zip")) {
            service.downloadOrderCurrentZip("owner", files, os);
        }

        // THEN
        Assert.assertTrue(files.stream().allMatch(f -> f.getState() == FileState.DOWNLOADED));
        try (ZipFile zipFile = new ZipFile("target/test.zip")) {
            Assert.assertEquals(files.size(), zipFile.stream().count());
        }

    }

    @Test
    public void test_download_zip_with_identical_files() throws IOException {
        // GIVEN
        // 4 Files in the order to download as :
        // - FILE 1 | file1_ql_sd.txt | checksum=123
        // - FILE 2 | file1_ql_md.txt | checksum=1234
        // - FILE 3 | file1_ql_md.txt | checksum=1234
        // - FILE 4 | file1_ql_md.txt | checksum=12345
        // - FILE 5 | file1_ql_hd.txt | checksum=1234
        // - FILE 6 | file1_ql_hd.txt | checksum null simulate external file
        List<OrderDataFile> files = List.of(initFile("1", "file1_ql_sd.txt", "123"),
                                            initFile("2", "file1_ql_md.txt", "1234"),
                                            initFile("3", "file1_ql_md.txt", "1234"),
                                            initFile("4", "file1_ql_md.txt", "12345"),
                                            initFile("5", "file1_ql_hd.txt", "1234"),
                                            initFile("6", "file1_ql_hd.txt", null));

        // WHEN
        try (FileOutputStream os = new FileOutputStream("target/test.zip")) {
            service.downloadOrderCurrentZip("owner", files, os);
        }

        // THEN
        // We expect to have FILE1, FILE2, FILE4 and FILE5 in zip. FILE3 is the same as FILE2 (filename and checksum) so
        // it is not in the result zip.
        Assert.assertTrue(files.stream().allMatch(f -> f.getState() == FileState.DOWNLOADED));
        try (ZipFile zipFile = new ZipFile("target/test.zip")) {
            Assert.assertEquals(files.size() - 1, zipFile.stream().count());
        }

    }

    private OrderDataFile initFile(String id, String file, String checksum) {
        Path filePath = Paths.get("src",
                                  "test",
                                  "resources",
                                  "files",
                                  "URN:AIP:DATA:ORDER:00000000-0000-0001-0000-000000000001:V1",
                                  file);
        DataFile dataFile = DataFile.build(DataType.RAWDATA,
                                           file,
                                           "file:" + filePath.toAbsolutePath(),
                                           MimeTypeUtils.APPLICATION_JSON,
                                           true,
                                           true);
        dataFile.setChecksum(checksum);
        dataFile.setDigestAlgorithm("MD5");
        return new OrderDataFile(dataFile,
                                 UniformResourceName.build(id, EntityType.DATA, "toto", UUID.randomUUID(), 1),
                                 1L);
    }

}
