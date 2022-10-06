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

package fr.cnes.regards.framework.utils.file;
import fr.cnes.regards.framework.s3.domain.S3Server;
import fr.cnes.regards.framework.s3.test.FileIdentificationEnum;
import fr.cnes.regards.framework.s3.test.S3FileTestUtils;
import fr.cnes.regards.framework.test.integration.RegardsSpringRunner;
import fr.cnes.regards.modules.storage.domain.database.FileReferenceMetaInfo;
import fr.cnes.regards.modules.storage.domain.database.request.FileStorageRequest;
import fr.cnes.regards.modules.storage.domain.plugin.FileStorageWorkingSubset;
import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.util.MimeType;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * @author Thibaud Michaudel
 **/

@RunWith(RegardsSpringRunner.class)
@SpringBootTest
@SpringBootConfiguration
public class DownloadUtilsIT {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    // Storage used for this test
    private final S3Server testServer = new S3Server("http://rs-s3-minio:9000",
                                                     "fr-regards-1",
                                                     "regards",
                                                     "regardspwd",
                                                     "bucket-test-download-utils",
                                                     "http[s]{0,1}://(?:.*?)/(?:.*?)/(.*?)/(.*)");

    @Before
    public void setUp() {
        S3FileTestUtils.createBucket(testServer);
        createTestFileOnServer();
        createDeepTestFileOnServer();
    }

    @Test
    public void testS3DownloadThroughInputStream() throws IOException {

        URL url = new URL("http", "rs-s3-minio", "/buckets/bucket-test-download-utils/file1.txt");

        List<S3Server> knownStorages = Arrays.asList(testServer);

        InputStream stream = DownloadUtils.getInputStream(url, knownStorages);

        String expected = "mundi placet et spiritus minima";

        Assert.assertEquals(expected, new String(stream.readAllBytes(), StandardCharsets.UTF_8));
    }

    @Test
    public void testS3DownloadThroughFileCopy() throws IOException, NoSuchAlgorithmException {
        URL url = new URL("http", "rs-s3-minio", "/buckets/bucket-test-download-utils/file1.txt");

        List<S3Server> knownStorages = Arrays.asList(testServer);

        Path destination = Paths.get(temporaryFolder.getRoot().toString() + "/file1.txt");
        String checksum = DownloadUtils.download(url, destination, "MD5", knownStorages);

        String expectedChecksum = "040befd332b3ce3d7d5f12943771af4e";
        String expectedContent = "mundi placet et spiritus minima";

        Assert.assertTrue(destination.toFile().exists());
        Assert.assertEquals(expectedChecksum, checksum);
        Assert.assertEquals(expectedContent, FileUtils.readFileToString(destination.toFile(), "utf-8"));
    }

    @Test
    public void testS3DownloadDeepFile() throws IOException {
        URL url = new URL("http",
                          "rs-s3-minio",
                          "/buckets/bucket-test-download-utils/test/deep/file/sub/directory/file2.txt");

        List<S3Server> knownStorages = Arrays.asList(testServer);

        InputStream stream = DownloadUtils.getInputStream(url, knownStorages);
        Assert.assertNotNull(stream);

        String expected = "Le roseau plie mais ne cède qu'en cas de pépin";

        Assert.assertEquals(expected, new String(stream.readAllBytes(), StandardCharsets.UTF_8));
    }

    @Test
    public void testS3DownloadFileUsingPattern() throws IOException {

        S3Server testServerWithoutBucket = new S3Server("http://rs-s3-minio:9000",
                                                        "fr-regards-1",
                                                        "regards",
                                                        "regardspwd",
                                                        "",
                                                        "http[s]{0,1}://(?:.*?)/(?:.*?)/(.*?)/(.*)");

        List<S3Server> knownStorages = Arrays.asList(testServerWithoutBucket);

        URL url = new URL("http", "rs-s3-minio", "/buckets/bucket-test-download-utils/file1.txt");
        InputStream stream = DownloadUtils.getInputStream(url, knownStorages);
        Assert.assertNotNull(stream);

        String expected = "mundi placet et spiritus minima";

        Assert.assertEquals(expected, new String(stream.readAllBytes(), StandardCharsets.UTF_8));

        URL urlFromString = new URL("http://rs-s3-minio:9000/buckets/bucket-test-download-utils/file1.txt");
        stream = DownloadUtils.getInputStream(urlFromString, knownStorages);
        Assert.assertNotNull(stream);
        Assert.assertEquals(expected, new String(stream.readAllBytes(), StandardCharsets.UTF_8));

    }

    @Test
    public void testS3DownloadDeepFileUsingPattern() throws IOException {

        S3Server testServerWithoutBucket = new S3Server("http://rs-s3-minio:9000",
                                                        "fr-regards-1",
                                                        "regards",
                                                        "regardspwd",
                                                        "",
                                                        "http[s]{0,1}://(?:.*?)/(?:.*?)/(.*?)/(.*)");

        List<S3Server> knownStorages = Arrays.asList(testServerWithoutBucket);

        URL url = new URL("http",
                          "rs-s3-minio",
                          "/buckets/bucket-test-download-utils/test/deep/file/sub/directory/file2.txt");
        InputStream stream = DownloadUtils.getInputStream(url, knownStorages);
        Assert.assertNotNull(stream);

        String expected = "Le roseau plie mais ne cède qu'en cas de pépin";

        Assert.assertEquals(expected, new String(stream.readAllBytes(), StandardCharsets.UTF_8));

    }

    private void createTestFileOnServer() {

        FileStorageRequest fileStorageRequest = new FileStorageRequest();
        fileStorageRequest.setOriginUrl("file:./src/test/resources/file1.txt");
        fileStorageRequest.setStorageSubDirectory("");
        FileReferenceMetaInfo fileReferenceMetaInfo = new FileReferenceMetaInfo();
        fileReferenceMetaInfo.setFileName("file1.txt");
        fileReferenceMetaInfo.setAlgorithm("MD5");
        fileReferenceMetaInfo.setMimeType(MimeType.valueOf("text/plain"));
        fileReferenceMetaInfo.setChecksum("040befd332b3ce3d7d5f12943771af4e");
        fileStorageRequest.setMetaInfo(fileReferenceMetaInfo);
        FileStorageWorkingSubset subset = new FileStorageWorkingSubset(Collections.singletonList(fileStorageRequest));
        S3FileTestUtils.store(subset, testServer, FileIdentificationEnum.FILENAME);
    }

    private void createDeepTestFileOnServer() {

        FileStorageRequest fileStorageRequest = new FileStorageRequest();
        fileStorageRequest.setOriginUrl("file:./src/test/resources/file2.txt");
        fileStorageRequest.setStorageSubDirectory("/test/deep/file/sub/directory");
        FileReferenceMetaInfo fileReferenceMetaInfo = new FileReferenceMetaInfo();
        fileReferenceMetaInfo.setFileName("file2.txt");
        fileReferenceMetaInfo.setAlgorithm("MD5");
        fileReferenceMetaInfo.setMimeType(MimeType.valueOf("text/plain"));
        fileReferenceMetaInfo.setChecksum("9dad4a34995d58e0a55aebaed5029f43");
        fileStorageRequest.setMetaInfo(fileReferenceMetaInfo);
        FileStorageWorkingSubset subset = new FileStorageWorkingSubset(Collections.singletonList(fileStorageRequest));
        S3FileTestUtils.store(subset, testServer, FileIdentificationEnum.FILENAME);
    }
}
