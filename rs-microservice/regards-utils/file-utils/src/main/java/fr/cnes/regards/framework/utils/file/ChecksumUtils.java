package fr.cnes.regards.framework.file.utils;

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
        for (int i = 0; i < checksumByte.length; i++) {
            String hex = Integer.toHexString(0xff & checksumByte[i]);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

    public static String computeHexChecksum(InputStream is, String checksumAlgorithm)
            throws NoSuchAlgorithmException, IOException {
        DigestInputStream dis=new DigestInputStream(is, MessageDigest.getInstance(checksumAlgorithm));
        while(dis.read()!=-1) {}
        dis.close();
        return getHexChecksum(dis.getMessageDigest().digest());
    }
}
