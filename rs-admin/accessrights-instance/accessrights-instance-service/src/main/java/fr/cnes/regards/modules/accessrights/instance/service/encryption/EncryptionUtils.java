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
package fr.cnes.regards.modules.accessrights.instance.service.encryption;

import fr.cnes.regards.framework.utils.RsRuntimeException;

import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Class EncryptionUtils
 * <p>
 * Tools to encrypt passwords.
 *
 * @author Sébastien Binda
 */
public final class EncryptionUtils {

    /**
     * Encryption algorithm
     */
    private static final String SHA_512 = "SHA-512";

    private static final String CHARSET = "UTF-8";

    private EncryptionUtils() {
    }

    /**
     * Encrypt given password with SHA_512
     *
     * @param pPassword to encrypt
     * @return Encrypted password
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
            throw new RsRuntimeException(e);
        }
    }

}
