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
import fr.cnes.regards.framework.s3.exception.S3ClientException;
import fr.cnes.regards.framework.s3.test.FileIdentificationEnum;
import fr.cnes.regards.framework.s3.test.S3BucketTestUtils;
import fr.cnes.regards.framework.s3.test.S3FileTestUtils;
import fr.cnes.regards.framework.test.integration.RegardsActiveProfileResolver;
import fr.cnes.regards.framework.test.integration.RegardsSpringRunner;
import fr.cnes.regards.modules.storage.domain.database.FileReferenceMetaInfo;
import fr.cnes.regards.modules.storage.domain.database.request.FileStorageRequest;
import fr.cnes.regards.modules.storage.domain.plugin.FileStorageWorkingSubset;
import org.apache.commons.io.FileUtils;
import org.junit.*;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.util.MimeType;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.Proxy;
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
@ActiveProfiles(resolver = RegardsActiveProfileResolver.class)
public class DownloadUtilsIT {

    @Value("${regards.IT.s3.protocol}")
    private String s3Protocol;

    @Value("${regards.IT.s3.host}")
    private String s3Host;

    @Value("${regards.IT.s3.port}")
    private int s3Port;

    @Value("${regards.IT.s3.key}")
    private String key;

    @Value("${regards.IT.s3.secret}")
    private String secret;

    @Value("${regards.IT.s3.region}")
    private String region;

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    // Storage used for this test
    private S3Server testServer;

    public static final String FILE_EXISTING = "/buckets/bucket-test-download-utils/file1.txt";

    public static final String FILE_NOT_EXISTING = "/buckets/bucket-test-download-utils/file11.txt";

    public static final String FILE_EXISTING_DEEP = "/buckets/bucket-test-download-utils/test/deep/file/sub/directory/file2.txt";

    @Before
    public void setUp() throws MalformedURLException {
        this.testServer = new S3Server(new URL(s3Protocol, s3Host, s3Port, "").toString(),
                                       region,
                                       key,
                                       secret,
                                       "bucket-test-download-utils",
                                       "http[s]{0,1}://(?:.*?)/(?:.*?)/(?<"
                                       + S3Server.REGEX_GROUP_BUCKET
                                       + ">.*?)/(?<"
                                       + S3Server.REGEX_GROUP_PATHFILENAME
                                       + ">.*)");
        S3BucketTestUtils.createBucket(testServer);
        createTestFileOnServer();
        createDeepTestFileOnServer();
    }

    @After
    public void clean() {
        S3BucketTestUtils.deleteBucket(testServer);
    }

    @Test
    public void testS3DownloadThroughInputStream() throws IOException {

        URL url = new URL(s3Protocol, s3Host, s3Port, FILE_EXISTING);

        List<S3Server> knownStorages = Arrays.asList(testServer);

        InputStream stream = DownloadUtils.getInputStream(url, knownStorages);

        String expected = "mundi placet et spiritus minima";

        Assert.assertEquals(expected, new String(stream.readAllBytes(), StandardCharsets.UTF_8));
    }

    @Test
    public void testS3DownloadThroughFileCopy() throws IOException, NoSuchAlgorithmException {
        URL url = new URL(s3Protocol, s3Host, s3Port, FILE_EXISTING);

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
        URL url = new URL(s3Protocol, s3Host, s3Port, FILE_EXISTING_DEEP);

        List<S3Server> knownStorages = Arrays.asList(testServer);

        InputStream stream = DownloadUtils.getInputStream(url, knownStorages);
        Assert.assertNotNull(stream);

        String expected = "Le roseau plie mais ne cède qu'en cas de pépin";

        Assert.assertEquals(expected, new String(stream.readAllBytes(), StandardCharsets.UTF_8));
    }

    @Test
    public void testS3DownloadFileUsingPattern() throws IOException {

        S3Server testServerWithoutBucket = new S3Server(new URL(s3Protocol, s3Host, s3Port, "").toString(),
                                                        region,
                                                        key,
                                                        secret,
                                                        "",
                                                        "http[s]{0,1}://(?:.*?)/(?:.*?)/(?<"
                                                        + S3Server.REGEX_GROUP_BUCKET
                                                        + ">.*?)/(?<"
                                                        + S3Server.REGEX_GROUP_PATHFILENAME
                                                        + ">.*)");

        List<S3Server> knownStorages = Arrays.asList(testServerWithoutBucket);

        URL url = new URL(s3Protocol, s3Host, s3Port, FILE_EXISTING);
        InputStream stream = DownloadUtils.getInputStream(url, knownStorages);
        Assert.assertNotNull(stream);

        String expected = "mundi placet et spiritus minima";

        Assert.assertEquals(expected, new String(stream.readAllBytes(), StandardCharsets.UTF_8));
    }

    @Test
    public void testS3DownloadDeepFileUsingPattern() throws IOException {

        S3Server testServerWithoutBucket = new S3Server(new URL(s3Protocol, s3Host, s3Port, "").toString(),
                                                        region,
                                                        key,
                                                        secret,
                                                        "",
                                                        "http[s]{0,1}://(?:.*?)/(?:.*?)/(?<"
                                                        + S3Server.REGEX_GROUP_BUCKET
                                                        + ">.*?)/(?<"
                                                        + S3Server.REGEX_GROUP_PATHFILENAME
                                                        + ">.*)");

        List<S3Server> knownStorages = Arrays.asList(testServerWithoutBucket);

        URL url = new URL(s3Protocol,
                          s3Host,
                          s3Port,
                          "/buckets/bucket-test-download-utils/test/deep/file/sub/directory/file2.txt");
        InputStream stream = DownloadUtils.getInputStream(url, knownStorages);
        Assert.assertNotNull(stream);

        String expected = "Le roseau plie mais ne cède qu'en cas de pépin";

        Assert.assertEquals(expected, new String(stream.readAllBytes(), StandardCharsets.UTF_8));

    }

    @Test
    public void testS3Exists() throws IOException {
        URL url = new URL(s3Protocol, s3Host, s3Port, FILE_EXISTING);
        List<S3Server> knownStorages = Arrays.asList(testServer);
        Assert.assertTrue(DownloadUtils.exists(url, Proxy.NO_PROXY, knownStorages, null, null));

        URL url2 = new URL(s3Protocol, s3Host, s3Port, FILE_NOT_EXISTING);
        Assert.assertFalse(DownloadUtils.exists(url2, Proxy.NO_PROXY, knownStorages, null, null));
    }

    @Test
    public void testMissingS3File() throws IOException {
        URL url = new URL(s3Protocol, s3Host, s3Port, FILE_NOT_EXISTING);

        List<S3Server> knownStorages = Arrays.asList(testServer);

        try {
            InputStream stream = DownloadUtils.getInputStream(url, knownStorages);
            Assert.fail();
        } catch (FileNotFoundException e) {
        }
    }

    @Test(expected = FileNotFoundException.class)
    public void testS3FileContentLengthFileNotFound() throws IOException {
        URL url = new URL(s3Protocol, s3Host, s3Port, FILE_NOT_EXISTING);

        List<S3Server> knownStorages = Arrays.asList(testServer);

        DownloadUtils.getContentLength(url, 60, knownStorages);
    }

    @Test
    public void testS3FileContentLength() throws IOException {
        URL url = new URL(s3Protocol, s3Host, s3Port, FILE_EXISTING);

        List<S3Server> knownStorages = Arrays.asList(testServer);

        long size = DownloadUtils.getContentLength(url, 60, knownStorages);
        Assert.assertEquals(31L, size);
    }

    @Test
    public void testUnreachableS3() throws IOException {
        S3Server testUnreachableServer = new S3Server(new URL(s3Protocol, s3Host, s3Port, "").toString(),
                                                      "bad-region",
                                                      key,
                                                      secret,
                                                      "bucket-test-download-utils",
                                                      "http[s]{0,1}://(?:.*?)/(?:.*?)/(?<"
                                                      + S3Server.REGEX_GROUP_BUCKET
                                                      + ">.*?)/(?<"
                                                      + S3Server.REGEX_GROUP_PATHFILENAME
                                                      + ">.*)");

        List<S3Server> knownStorages = Arrays.asList(testUnreachableServer);

        URL url = new URL(s3Protocol, s3Host, s3Port, FILE_EXISTING);
        try {
            InputStream stream = DownloadUtils.getInputStream(url, knownStorages);
            Assert.fail();
        } catch (S3ClientException e) {
        }

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

        S3FileTestUtils.store(new FileStorageWorkingSubset(Collections.singletonList(fileStorageRequest)),
                              testServer,
                              FileIdentificationEnum.FILENAME);
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

        S3FileTestUtils.store(new FileStorageWorkingSubset(Collections.singletonList(fileStorageRequest)),
                              testServer,
                              FileIdentificationEnum.FILENAME);
    }
}
