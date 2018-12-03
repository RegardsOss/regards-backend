package fr.cnes.regards.framework.encryption;

import fr.cnes.regards.framework.encryption.exception.EncryptionException;

/**
 * Encryption service API.
 * @author Sylvain VISSIERE-GUERINET
 */
public interface IEncryptionService {

    /**
     * Encrypt a message
     * @param toEncrypt message to be encrypted
     * @return encrypted message
     */
    String encrypt(String toEncrypt) throws EncryptionException;

    /**
     * Decrypt a message
     * @param toDecrypt message to be decrypted
     * @return decrypted message
     */
    String decrypt(String toDecrypt) throws EncryptionException;
}
