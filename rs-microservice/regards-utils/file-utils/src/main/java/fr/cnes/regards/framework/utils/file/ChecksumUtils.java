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

    public static String getHexChecksum(byte[] checksumByte) {
        StringBuffer hexString = new StringBuffer();
        for (byte element : checksumByte) {
            String hex = Integer.toHexString(0xff & element);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

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
