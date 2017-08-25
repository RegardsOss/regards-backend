package fr.cnes.regards.modules.storage.plugin.utils.encryption;

/**
 *
 * Interface de cryptage du paquetage.
 *
 *
 * @author CS
 * @version $Revision: 1.10 $
 * @since 1.0
 */
public interface IEncryption {

    /**
     * Permet d'encoder la chaine de caractere
     * @return la chaine encryptee
     * @since 1.0
     * @param pToBeEncoded : la chaine en clair.
     * @throws sipad.tools.encryption.EncryptionException Exception survnue lors du traitement
     */
    public abstract String crypt(String pToBeEncoded) throws EncryptionException;

    /**
     * Permet de decoder la chaine de caractere parametre dans le cas ou l'algorithme est reversible.
     * @return la chaine de caractere decodee
     * @since 1.0
     * @param pToBeDecoded : la chaine de caractere a decoder
     * @throws sipad.tools.encryption.EncryptionException Exception survnue lors du traitement
     */
    public abstract String decrypt(String pToBeDecoded) throws EncryptionException;

    /**
     * Permet de decoder la chaine de caractere parametre dans le cas ou l'algorithme est reversible.
     * @return signature MD5 du fichier passe en parametre
     * @since 4.6
     * @param pFile Fichier pour lequel on veut la signature MD5
     * @throws sipad.tools.encryption.EncryptionException Exception survnue lors du traitement
     * @DM SIPNG-DM-0074-CN
     */
    public abstract String getFileSignature(String pFile) throws EncryptionException;

    /** Permet de connaitre la taille limite des chaines a encrypter
     * @since 4.6
     * @param
     * @return max length
     */
    public abstract int getMaxLength();
}
