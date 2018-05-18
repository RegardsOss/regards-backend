package fr.cnes.regards.framework.utils.file;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
@Ignore("those tests are ignored for now because it would need a lot of work to make all of them work all the time and"
        + " they just are tests of java")
public class DownloadUtilsTests {

    /**
     * For this test, lets get a file throw URL possibilities on one hand and directly thanks to Files on the other hand.
     * If both checksums are equals then it means it works perfectly.
     * @throws IOException
     * @throws NoSuchAlgorithmException
     */
    @Test
    public void testDownloadWithFileProtocolWithoutProxy() throws IOException, NoSuchAlgorithmException {
        String fileLocation = "src/test/resources/data.txt";
        URL source = new URL("file", "localhost", fileLocation);
        InputStream is = DownloadUtils.getInputStream(source);
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
     * @throws IOException
     * @throws NoSuchAlgorithmException
     */
    @Test
    public void testDownloadWithFileProtocolWithProxy() throws IOException, NoSuchAlgorithmException {
        String fileLocation = "src/test/resources/data.txt";
        URL source = new URL("file", "localhost", fileLocation);
        Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("proxy2.si.c-s.fr", 3128));
        InputStream is = DownloadUtils.getInputStreamThroughProxy(source, proxy);
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
        InputStream is = DownloadUtils.getInputStream(source);
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
        Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("proxy2.si.c-s.fr", 3128));
        InputStream is = DownloadUtils.getInputStreamThroughProxy(source, proxy);
        DigestInputStream dis = new DigestInputStream(is, MessageDigest.getInstance("MD5"));
        while (dis.read() != -1) {
        }
        String checksum = ChecksumUtils.getHexChecksum(dis.getMessageDigest().digest());
        dis.close();
        // expected checksum was calculated thanks to md5sum after a wget of the file
        Assert.assertEquals("464530a4e23f4f831eeabf9678c43bdf", checksum);
    }
}
