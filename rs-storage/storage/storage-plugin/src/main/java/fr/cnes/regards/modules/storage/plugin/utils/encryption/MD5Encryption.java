package fr.cnes.regards.modules.storage.plugin.utils.encryption;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.apache.commons.codec.binary.Hex;

/**
 * Cette classe implemente l'interface ICryptage pour realiser un cryptage MD5.
 *
 * @author CS
 * @version $Revision: 1.13 $
 * @since 1.0
 */
public class MD5Encryption implements IEncryption {

    /**
     * Le caractere '0'
     *
     * @since 2.1
     */
    private static final char ZERO = '0';

    /**
     * Constructeur
     *
     * @since 1.0
     */
    protected MD5Encryption() {
        super();
    }

    /**
     *
     * Encrypte la chaine de caractere passee en parametre afin de realiser un chiffrement MD5.
     *
     * @see sipad.tools.encryption.IEncryption#crypt(java.lang.String)
     * @param pToBeEncoded
     *            chaine à encoder
     * @throws sipad.tools.encryption.EncryptionException
     *             Exception survnue lors du traitement
     * @return chaine cryptée en MD5
     */
    @Override
    public String crypt(String pToBeEncoded) throws EncryptionException {
        byte[] uniqueKey = pToBeEncoded.getBytes();
        byte[] hash = null;
        try {
            hash = MessageDigest.getInstance("MD5").digest(uniqueKey);
        } catch (NoSuchAlgorithmException noSuAlEx) {
            throw new EncryptionException("No MD5 support in this VM", noSuAlEx);
        }

        StringBuilder hashString = new StringBuilder();
        for (byte element : hash) {
            String hex = Integer.toHexString(element);
            if (hex.length() == 1) {
                hashString.append(ZERO);
                hashString.append(hex.charAt(hex.length() - 1));
            } else {
                hashString.append(hex.substring(hex.length() - 2));
            }
        }
        return hashString.toString();
    }

    /**
     * MD5 n'est pas reversible !!
     *
     * @see sipad.tools.encryption.IEncryption#decrypt(java.lang.String)
     * @param pToBeDecoded
     *            chaine a decoder
     * @throws sipad.tools.encryption.EncryptionException
     *             Exception survnue lors du traitement
     * @return chaine décodée
     */
    @Override
    public String decrypt(String pToBeDecoded) throws EncryptionException {
        throw new EncryptionException("MD5 is irreversible");
    }

    /**
     * Recupere la signature MD5 du fichier passe en parametre
     *
     * @param pFile
     *            Fichier pour lequel on veut la signature MD5
     * @throws sipad.tools.encryption.EncryptionException
     *             Exception survnue lors du traitement
     * @return signature MD5 du fichier passe en parametre
     * @since 4.6
     * @DM SIPNG-DM-0074-CN
     */
    @Override
    public String getFileSignature(String pFile) throws EncryptionException {

        String signatureMD5 = "";
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            File f = new File(pFile);
            InputStream is = new FileInputStream(f);
            byte[] buffer = new byte[2048];
            int read = 0;
            try {
                while ((read = is.read(buffer)) > 0) {
                    digest.update(buffer, 0, read);
                }
                byte[] md5sum = digest.digest();

                signatureMD5 = new String(Hex.encodeHex(md5sum));
            } catch (IOException e) {
                throw new EncryptionException("Unable to close input stream for MD5 calculation");
            } finally {
                try {
                    is.close();
                } catch (IOException e) {
                    throw new EncryptionException("Unable to close input stream for MD5 calculation");
                }
            }
        } catch (NoSuchAlgorithmException noSuAlEx) {
            throw new EncryptionException("No MD5 support in this VM", noSuAlEx);
        } catch (FileNotFoundException e) {
            throw new EncryptionException(String.format("No such file %s", pFile), e);
        }

        return signatureMD5;
    }

    /**
     * Methode permettant de retourner la taille maximal d'encryptage
     *
     * @since 4.6
     * @return
     */
    @Override
    public int getMaxLength() {
        return 0;
    }
}
