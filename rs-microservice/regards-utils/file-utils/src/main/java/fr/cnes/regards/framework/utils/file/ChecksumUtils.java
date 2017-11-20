package fr.cnes.regards.framework.utils.file;

import java.io.IOException;
import java.io.InputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
public class ChecksumUtils {

    private ChecksumUtils() {}

    /**
     * transform a byte array to an hexadecimal string
     */
    public static String getHexChecksum(byte[] checksumByte) {
        StringBuilder hexString = new StringBuilder();
        for (byte element : checksumByte) {
            String hex = Integer.toHexString(0xff & element);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

    /**
     * @return checksum computed from the input stream thanks to the algorithm as an hexadecimal string.
     * @throws NoSuchAlgorithmException if checksumAlgorithm is not handled and known by the java process
     * @throws IOException see possible causes from InputStream
     */
    public static String computeHexChecksum(InputStream is, String checksumAlgorithm)
            throws NoSuchAlgorithmException, IOException {
        byte[] buffer = new byte[8192];
        DigestInputStream dis = new DigestInputStream(is, MessageDigest.getInstance(checksumAlgorithm));
        while (dis.read(buffer) != -1) {
        }
        dis.close();
        return getHexChecksum(dis.getMessageDigest().digest());
    }
}
