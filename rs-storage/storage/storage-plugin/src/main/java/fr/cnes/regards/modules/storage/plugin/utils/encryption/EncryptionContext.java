/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.storage.plugin.utils.encryption;

/**
 *
 * Cette classe permet de definir et de piloter l'algorithme de cryptage.
 *
 */
public class EncryptionContext {

    /**
     * Designe la strategie de cryptage
     */
    private IEncryption encryption_ = null;

    /**
     * La chaine a traiter
     */
    private String word_ = null;

    /**
     * Le fichier a traiter par la methode doGetFileSignature
     */
    private String file_ = null;

    /**
     * Constructeur
     */
    protected EncryptionContext() {
    }

    /**
     * Permet de crypter un mot en clair selon la methode definie par <code>encryption_</code>
     * @return le chiffre calcule a partir du mot en clair
     * @throws EncryptionException Exception survnue lors du traitement
     * @since 1.0
     */
    protected String doEncrypt() throws EncryptionException {
        return (encryption_.crypt(word_));
    }

    /**
     * Permet de decrypter un mot chiffre selon la methode definie par <code>encryption_</code>
     * @return le mot de passe decrypte
     * @throws EncryptionException Exception survnue lors du traitement
     * @since 1.0
     */
    protected String doDecrypt() throws EncryptionException {
        return (encryption_.decrypt(word_));
    }

    /**
     * Permet d'obtenir la signature MD5 du fichier contenu dans l'attribut file_
     * @return
     * @throws EncryptionException
     * @since 4.6
     */
    protected String doGetFileSignature() throws EncryptionException {
        return (encryption_.getFileSignature(file_));
    }

    /** Permet de connaitre la taille limite des chaines a encrypter
     * @since 4.6
     * @param file
     * @return max length
     */
    public int doGetMaxLength() {
        return (encryption_.getMaxLength());
    }

    // === setters === //

    /**
     * @param pEncryption
     * @since 1.0
     */
    protected void setEncryption(IEncryption pEncryption) {
        encryption_ = pEncryption;
    }

    /**
     * @param pWord
     * @since 1.0
     */
    protected void setWord(String pWord) {
        word_ = pWord;
    }

    /**
     * @param pFile
     * @since 4.6
     */
    protected void setFile(String pFile) {
        file_ = pFile;
    }

}
