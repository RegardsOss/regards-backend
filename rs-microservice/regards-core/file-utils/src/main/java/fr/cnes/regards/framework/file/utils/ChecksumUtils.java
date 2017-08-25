package fr.cnes.regards.framework.file.utils;

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
}
