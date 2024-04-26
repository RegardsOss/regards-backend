package fr.cnes.regards.framework.utils.file;

import com.google.common.collect.Sets;
import org.apache.commons.io.FileUtils;
import org.junit.*;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.Properties;
import java.util.Set;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
@Ignore("those tests are ignored for now because it would need a lot of work to make all of them work all the time and"
        + " they just are tests of java")
public class DownloadUtilsTests {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private String proxyHost;

    private int proxyPort;

    @Before
    public void initProxy() throws IOException {
        InputStream proxyInput = getClass().getClassLoader().getResourceAsStream("proxy.properties");
        Properties proxyProperties = new Properties();
        proxyProperties.load(proxyInput);
        proxyHost = proxyProperties.getProperty("proxyHost");
        proxyPort = Integer.parseInt(proxyProperties.getProperty("proxyPort"));
    }

    /**
     * For this test, lets get a file throw URL possibilities on one hand and directly thanks to Files on the other hand.
     * If both checksums are equals then it means it works perfectly.
     */
    @Test
    public void testDownloadWithFileProtocolWithoutProxy() throws IOException, NoSuchAlgorithmException {
        String fileLocation = "src/test/resources/data.txt";
        URL source = new URL("file", "localhost", fileLocation);
        InputStream is = DownloadUtils.getInputStream(source, Collections.emptyList());
        DigestInputStream dis = new DigestInputStream(is, MessageDigest.getInstance("MD5"));
        while (dis.read() != -1) {
        }
        byte[] urlDigest = dis.getMessageDigest().digest();
        dis.close();

        InputStream fileIs = Files.newInputStream(Paths.get(fileLocation));
        DigestInputStream fileDis = new DigestInputStream(fileIs, MessageDigest.getInstance("MD5"));
        while (fileDis.read() != -1) {
        }
        byte[] fileDigest = fileDis.getMessageDigest().digest();

        Assert.assertEquals(fileDigest.length, urlDigest.length);
        for (int i = 0; i < fileDigest.length; i++) {
            Assert.assertEquals(fileDigest[i], urlDigest[i]);
        }
    }

    /**
     * Just to test that we can provide a proxy configuration even if it's not needed, and it still works
     */
    @Test
    public void testDownloadWithFileProtocolWithProxy() throws IOException, NoSuchAlgorithmException {
        String fileLocation = "src/test/resources/data.txt";
        URL source = new URL("file", "localhost", fileLocation);
        Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHost, proxyPort));
        InputStream is = DownloadUtils.getInputStreamThroughProxy(source, proxy, null, Collections.emptyList());
        DigestInputStream dis = new DigestInputStream(is, MessageDigest.getInstance("MD5"));
        while (dis.read() != -1) {
        }
        byte[] urlDigest = dis.getMessageDigest().digest();
        dis.close();

        InputStream fileIs = Files.newInputStream(Paths.get(fileLocation));
        DigestInputStream fileDis = new DigestInputStream(fileIs, MessageDigest.getInstance("MD5"));
        while (fileDis.read() != -1) {
        }
        byte[] fileDigest = fileDis.getMessageDigest().digest();

        Assert.assertEquals(fileDigest.length, urlDigest.length);
        for (int i = 0; i < fileDigest.length; i++) {
            Assert.assertEquals(fileDigest[i], urlDigest[i]);
        }
    }

    @Test
    public void testDownloadWithHttpProtocolWithoutProxy() throws IOException, NoSuchAlgorithmException {
        URL source = new URL("http://172.26.47.107:9020/conf/staticConfiguration.js");
        InputStream is = DownloadUtils.getInputStream(source, Collections.emptyList());
        DigestInputStream dis = new DigestInputStream(is, MessageDigest.getInstance("MD5"));
        while (dis.read() != -1) {
        }
        String checksum = ChecksumUtils.getHexChecksum(dis.getMessageDigest().digest());
        dis.close();
        // expected checksum was calculated thanks to md5sum after a wget of the file
        Assert.assertEquals("5cb51272a80331a4560565bbd33a0fe5", checksum);
    }

    @Test
    public void testDownloadWithHttpProtocolWithProxy() throws IOException, NoSuchAlgorithmException {
        URL source = new URL("http://mirror.centos.org/centos/RPM-GPG-KEY-CentOS-3");
        Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHost, proxyPort));
        InputStream is = DownloadUtils.getInputStreamThroughProxy(source, proxy, null, Collections.emptyList());
        DigestInputStream dis = new DigestInputStream(is, MessageDigest.getInstance("MD5"));
        while (dis.read() != -1) {
        }
        String checksum = ChecksumUtils.getHexChecksum(dis.getMessageDigest().digest());
        dis.close();
        // expected checksum was calculated thanks to md5sum after a wget of the file
        Assert.assertEquals("464530a4e23f4f831eeabf9678c43bdf", checksum);
    }

    @Test
    public void testDownloadWithHttpProtocolThroughTmpFile() throws IOException, NoSuchAlgorithmException {
        // Given

        // expected checksum was calculated thanks to md5sum after a wget of the file
        String checksum = "464530A4E23F4F831EEABF9678C43BDF";

        URL source = new URL("http://mirror.centos.org/centos/RPM-GPG-KEY-CentOS-3");
        Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHost, proxyPort));
        Path tmpDirPath = temporaryFolder.newFolder("workspace").toPath();
        Path downloadedFileDir = temporaryFolder.newFolder("targetDownload").toPath();

        // When
        InputStream is = DownloadUtils.getInputStreamThroughProxy(source,
                                                                  proxy,
                                                                  null,
                                                                  Collections.emptyList(),
                                                                  new DownloadTmpConfigDto(false,
                                                                                           500L,
                                                                                           tmpDirPath.resolve(checksum),
                                                                                           true));

        // Then
        Assert.assertEquals("There should be one and only one tmp file", 1, tmpDirPath.toFile().list().length);
        File tmpFile = tmpDirPath.toFile().listFiles()[0];
        DigestInputStream dis = new DigestInputStream(new FileInputStream(tmpFile), MessageDigest.getInstance("MD5"));
        while (dis.read() != -1) {
        }
        String tmpFileChecksum = ChecksumUtils.getHexChecksum(dis.getMessageDigest().digest()).toUpperCase();
        Assert.assertEquals("The tmp file should have the sent file checksum", checksum, tmpFileChecksum);

        // When
        FileUtils.copyInputStreamToFile(is, downloadedFileDir.resolve("file").toFile());

        // Then
        Assert.assertEquals("There should be one and only one downloaded file",
                            1,
                            downloadedFileDir.toFile().list().length);
        File downloadedFile = downloadedFileDir.toFile().listFiles()[0];
        dis = new DigestInputStream(new FileInputStream(downloadedFile), MessageDigest.getInstance("MD5"));
        while (dis.read() != -1) {
        }
        String downloadedFileChecksum = ChecksumUtils.getHexChecksum(dis.getMessageDigest().digest()).toUpperCase();

        Assert.assertEquals("The downloaded file should have the sent file checksum", checksum, downloadedFileChecksum);
        Assert.assertEquals("There should be no more tmp file", 0, tmpDirPath.toFile().list().length);

    }

    @Test
    public void testContentLengthWithHttpProtocolWithProxy() throws IOException, NoSuchAlgorithmException {
        URL source = new URL("http://mirror.centos.org/centos/RPM-GPG-KEY-CentOS-3");
        Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHost, proxyPort));
        long fileSize = DownloadUtils.getContentLengthThroughProxy(source, proxy, null, 10, Collections.emptyList())
                                     .longValue();

        Assert.assertEquals(1795L, fileSize);
    }

    @Test
    public void testContentLengthWithFile() throws IOException {
        String fileLocation = "src/test/resources/data.txt";
        URL source = new URL("file", "localhost", fileLocation);
        long size = DownloadUtils.getContentLength(source, 50, Collections.emptyList()).longValue();
        Assert.assertEquals(445L, size);
    }

    @Test
    public void testNonProxyHosts() throws MalformedURLException {
        Set<String> nonProxyHosts = Sets.newHashSet("localhost", "plop.com");
        Assert.assertFalse(DownloadUtils.needProxy(new URL("http://plop.com/files/myFile.txt"), nonProxyHosts));

        Assert.assertTrue(DownloadUtils.needProxy(new URL("http://plip.com/files/myFile.txt"), nonProxyHosts));
    }

    @Test
    public void testExistsWithHttpProtocolWithProxy() {
        Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHost, proxyPort));
        try {
            Assert.assertTrue(DownloadUtils.exists(new URL("http://mirror.centos.org/centos/RPM-GPG-KEY-CentOS-3"),
                                                   proxy,
                                                   null,
                                                   null,
                                                   null));
        } catch (IOException e) {
            Assert.fail();
        }

        try {
            Assert.assertFalse(DownloadUtils.exists(new URL("http://mirror.centos.org/centos/RPM-GPG-KEY-CentOS-33"),
                                                    proxy,
                                                    null,
                                                    null,
                                                    null));
        } catch (IOException e) {
            Assert.fail();
        }
    }

    @Test
    public void testExistsWithHttpProtocolWithoutProxy() {
        try {
            Assert.assertTrue(DownloadUtils.exists(new URL("http://172.26.47.107:9020/conf/staticConfiguration.js"),
                                                   Proxy.NO_PROXY,
                                                   null,
                                                   null,
                                                   null));
        } catch (IOException e) {
            Assert.fail();
        }

        try {
            Assert.assertFalse(DownloadUtils.exists(new URL("http://172.26.47.107:9020/conf/staticConfiguration.jss"),
                                                    Proxy.NO_PROXY,
                                                    null,
                                                    null,
                                                    null));
        } catch (IOException e) {
            Assert.fail();
        }
    }

    @Test
    public void testExistsWithFileProtocolWithProxy() {
        Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHost, proxyPort));
        try {
            Assert.assertTrue(DownloadUtils.exists(new URL("file", "localhost", "src/test/resources/data.txt"),
                                                   proxy,
                                                   null,
                                                   null,
                                                   null));
        } catch (IOException e) {
            Assert.fail();
        }

        try {
            Assert.assertFalse(DownloadUtils.exists(new URL("file", "localhost", "src/test/resources/dataa.txt"),
                                                    proxy,
                                                    null,
                                                    null,
                                                    null));
        } catch (IOException e) {
            Assert.fail();
        }
    }
}
