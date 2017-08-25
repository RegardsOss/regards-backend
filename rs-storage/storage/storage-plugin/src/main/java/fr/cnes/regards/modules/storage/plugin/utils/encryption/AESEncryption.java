/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.storage.plugin.utils.encryption;

import org.apache.log4j.Logger;

/**
 * La classe <code>AESEncryption</code> realiqse l'encryption de chaine de
 * caractere a l'aide de l'algorithme AES.
 * La chiane ne doit pas etre plus longue que 16 caracteres.
 *
 * @author CS
 * @version 1.0
 * @since 1.0
 */
public class AESEncryption implements IEncryption {

    /**
     * La longueur max d'un mot qui peut etre encrypte.
     * @since 2.1
     */
    private static final int MAX_LENGTH = 16;

    /**
     * Le caractere espace: ' '
     * @since 2.1
     */
    private static final char WHITESPACE = ' ';

    /**
     * Le polynome de conversion
     * @since 1.0
     */
    private static final byte[] aes_ = new byte[] { -1, 0, 1, 2, 3, 4, 5, 6, -6, -5, -4, -3, -2, -1, -55, -56 };

    /**
     * Cette variable est utilisee pour logger les messages
     * @since 1.0
     */
    private static Logger logger = Logger.getLogger(AESEncryption.class);

    /**
     * Constructeur
     * @since 1.0
     */
    protected AESEncryption() {
        super();
    }

    /**
     * Methode surchargee.
     *
     * Realise le chiffrement d'une chaine de caractere a l'aide de
     * l'algorithme AES-Rijndael avec une cle de 128 bits.
     * <b>Le mot de passe a crypter ne doit pas depasser la taille de 16
     * caracteres, sinon il sera tronque !</b>
     *
     * @param pToBeEncoded la chaine en clair a chiffrer
     * @return le mot de passe crypte en hexadecimal (cipher)
     * @throws sipad.tools.encryption.EncryptionException Exception survnue lors du traitement
     * @see sipad.tools.encryption.IEncryption#crypt(java.lang.String)
     * @since 1.0
     */
    @Override
    public String crypt(String pToBeEncoded) throws EncryptionException {
        try {
            // Throw an exception if the word to be encoded is too long
            if (pToBeEncoded.length() > MAX_LENGTH) {
                // Error: word to long
                String msg = String.format("The word %s exceedes the limit of %s characters", pToBeEncoded,
                                           String.valueOf(MAX_LENGTH));
                logger.error(msg);
                throw new EncryptionException(msg);
            }

            char[] tab = new char[] { WHITESPACE, WHITESPACE, WHITESPACE, WHITESPACE, WHITESPACE, WHITESPACE,
                    WHITESPACE, WHITESPACE, WHITESPACE, WHITESPACE, WHITESPACE, WHITESPACE, WHITESPACE, WHITESPACE,
                    WHITESPACE, WHITESPACE };
            for (int i = 0; i < Math.min(MAX_LENGTH, pToBeEncoded.length()); i++) {
                tab[i] = pToBeEncoded.charAt(i);
            }
            byte[] in = new String(tab).getBytes();
            byte[] res = Rijndael_Algorithm.blockEncrypt(in, 0, Rijndael_Algorithm.makeKey(aes_));

            return HexConverter.toString(res);
        } catch (EncryptionException e) {
            // Just throw it. The exception is already logged
            throw e;
        } catch (Exception e) {
            String msg = "Error occured while trying to encrypt a string";
            logger.error(msg, e);
            throw new EncryptionException(msg, e);
        }
    }

    /**
     * Methode surchargee
     * Decrypte un mot de passe crypte en hexadecimal avec AES-Rinjdael 128
     * bits.
     *
     * @param pToBeDecoded le cipher a decoder
     * @return le mot de passe en clair
     * @see sipad.tools.encryption.IEncryption#decrypt(java.lang.String)
     * @throws sipad.tools.encryption.EncryptionException Exception survnue lors du traitement
     * @since 1.0
     */
    @Override
    public String decrypt(String pToBeDecoded) throws EncryptionException {
        byte[] res = null;
        try {
            byte[] in = HexConverter.hexadecimalToArrayOfByte(pToBeDecoded);
            res = Rijndael_Algorithm.blockDecrypt(in, 0, Rijndael_Algorithm.makeKey(aes_));
        } catch (Exception e) {
            throw new EncryptionException("Error occured while trying to decrypt cipher", e);
        }
        return new String(res).trim();
    }

    /**
     * Methode non implementee pour le cryptage AES.
     * @since 4.6
     */
    @Override
    public String getFileSignature(String pFile) throws EncryptionException {
        return null;
    }

    /** Methode permettant de retourner la taille maximal d'encryptage
     * @since 4.6
     * @return
     */
    @Override
    public int getMaxLength() {
        return MAX_LENGTH;
    }

}
