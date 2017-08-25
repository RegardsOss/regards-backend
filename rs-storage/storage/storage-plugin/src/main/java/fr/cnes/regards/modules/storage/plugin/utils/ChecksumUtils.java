/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.storage.plugin.utils;

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
}
