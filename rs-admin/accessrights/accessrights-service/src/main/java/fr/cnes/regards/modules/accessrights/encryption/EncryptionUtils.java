/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.encryption;

import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 *
 * Class EncryptionUtils
 *
 * Tools to encrypt passwords.
 *
 * @author Sébastien Binda
 * @since 1.0-SNAPSHOT
 */
public class EncryptionUtils {

    /**
     * Encryption algorithm
     */
    private static final String SHA_512 = "SHA-512";

    private static final String CHARSET = "UTF-8";

    private EncryptionUtils() {
    }

    /**
     *
     * Encrypt given password with SHA_512
     *
     * @param pPassword
     *            to encrypt
     * @return Encrypted password
     * @since 1.0-SNAPSHOT
     */
    public static String encryptPassword(final String pPassword) {
        try {
            final MessageDigest md = MessageDigest.getInstance(SHA_512);
            final Charset charset = Charset.forName(CHARSET);
            final byte[] bytes = md.digest(pPassword.getBytes(charset));
            final StringBuilder sb = new StringBuilder();
            for (final byte b : bytes) {
                sb.append(Integer.toString((b & 0xff) + 0x100, 16).substring(1));
            }
            return sb.toString();
        } catch (final NoSuchAlgorithmException e) {
            throw new RuntimeException(e);// NOSONAR: this is only a developpement exception and should never happens
        }
    }

}
