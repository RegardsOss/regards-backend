package fr.cnes.regards.framework.utils.file;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.xml.bind.DatatypeConverter;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
public final class ChecksumUtils {

    private ChecksumUtils() {
    }

    /**
     * transform a byte array to an hexadecimal string
     */
    public static String getHexChecksum(byte[] checksumByte) {
        return DatatypeConverter.printHexBinary(checksumByte).toLowerCase();
    }

    /**
     * Compute checksum and closes provided input stream
     * @return checksum computed from the input stream thanks to the algorithm as an hexadecimal string.
     * @throws NoSuchAlgorithmException if checksumAlgorithm is not handled and known by the java process
     * @throws IOException              see possible causes from InputStream
     */
    public static String computeHexChecksum(Path filePath, String checksumAlgorithm)
            throws NoSuchAlgorithmException, IOException {
        MessageDigest md = MessageDigest.getInstance("MD5");
        try (InputStream is = Files.newInputStream(filePath); DigestInputStream dis = new DigestInputStream(is, md)) {
            byte[] buffer = new byte[8192];
            while (dis.read(buffer) != -1) {
            }
        }
        return getHexChecksum(md.digest());
    }

    /**
     * Compute checksum and closes provided input stream
     * @return checksum computed from the input stream thanks to the algorithm as an hexadecimal string.
     * @throws NoSuchAlgorithmException if checksumAlgorithm is not handled and known by the java process
     * @throws IOException              see possible causes from InputStream
     */
    public static String computeHexChecksum(InputStream is, String checksumAlgorithm)
            throws NoSuchAlgorithmException, IOException {
        MessageDigest md = MessageDigest.getInstance("MD5");
        try (DigestInputStream dis = new DigestInputStream(is, md)) {
            /* Read decorated stream (dis) to EOF as normal... */
            byte[] buffer = new byte[8192];
            while (dis.read(buffer) != -1) {
            }
        } finally {
            is.close();
        }
        return getHexChecksum(md.digest());
    }

    /**
     * @return checksum for specified text and algorithm
     */
    public static String computeHexChecksum(String text, String checksumAlgorithm)
            throws NoSuchAlgorithmException, IOException {
        try (InputStream inputStream = new ByteArrayInputStream(text.getBytes())) {
            return ChecksumUtils.computeHexChecksum(inputStream, checksumAlgorithm);
        }
    }
}
