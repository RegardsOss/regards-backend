package fr.cnes.regards.modules.storage.plugin.utils.encryption;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * Cette classe est la Facade du paquetage d'encryptage.
 *
 *
 * @author CS
 * @version $Revision: 1.9 $
 * @since 1.0
 */
public class EncryptionFacade {

    /**
     * Le contexte permettant de piloter l'outil de cryptage
     */
    private final EncryptionContext context;

    /**
     * Constructeur de classe
     */
    public EncryptionFacade() {
        context = new EncryptionContext();
    }

    /**
     * Permet de declencher le cryptage
     *
     * @param pString
     *            la chaine a crypter
     * @param pMode
     *            la methode de chiffrage
     * @throws EncryptionException
     *             Exception survnue lors du traitement
     * @return le resultat du chiffrage
     */
    public String encrypt(EncryptionTypeEnum pMode, String pString) throws EncryptionException {

        initEncryptionMethod(pMode);
        context.setWord(pString);

        return context.doEncrypt();
    }

    /**
     * Permet de declencher le decryptage
     *
     * @param pEncryptedString
     *            : la chaine cryptee
     * @param pMode
     *            la methode de chiffrage
     * @throws EncryptionException
     *             Exception survnue lors du traitement
     * @return le resultat du dechiffrage
     */
    public String decrypt(EncryptionTypeEnum pMode, String pEncryptedString) throws EncryptionException {

        initEncryptionMethod(pMode);
        context.setWord(pEncryptedString);

        return context.doDecrypt();
    }

    /**
     * Permet de declencher le calcul de la signature du fichier passe en parametre Seul le mode MD5 est implemente
     *
     * @param pMode
     *            : mode d'encryption (seul le mode MD5 est implemente)
     * @param pFile
     *            : le fichier a traiter
     * @return
     * @throws EncryptionException
     */
    public String getFileSignature(EncryptionTypeEnum pMode, String pFile) throws EncryptionException {

        initEncryptionMethod(pMode);
        context.setFile(pFile);

        return context.doGetFileSignature();
    }

    /**
     * Permet de connaitre la taille limite des chaines a encrypter
     *
     * @param file
     * @return max length
     * @throws EncryptionException
     */
    public int getMaxLenght(EncryptionTypeEnum pMode) throws EncryptionException {
        initEncryptionMethod(pMode);
        return context.doGetMaxLength();
    }

    /**
     * Permet de declencher le decryptage
     *
     * @param pMode
     *            la methode de chiffrage
     * @throws EncryptionException
     *             Exception survnue lors du traitement
     */
    private void initEncryptionMethod(EncryptionTypeEnum pMode) throws EncryptionException {
        if (pMode == EncryptionTypeEnum.AES) {
            context.setEncryption(new AESEncryption());
        } else if (pMode == EncryptionTypeEnum.MD5) {
            context.setEncryption(new MD5Encryption());
        } else {
            throw new EncryptionException(String.format("This method %s for encryption is not valid", pMode));
        }
    }

    /**
     *
     * Méthode permettant de lire le résultat de la commande UNIX md5sum présente dans un fichier.
     *
     * @param pFile
     *            Fichier dans lequel est écrit le résultat de la commande md5sum pouvant contenir les signatures de
     *            plusieurs fichiers
     * @return
     * @throws IOException
     */
    public Map<String, String> readMD5File(File pFile) throws IOException {

        Map<String, String> result = new HashMap<>();

        BufferedReader br = new BufferedReader(new FileReader(pFile));
        String line;
        while ((line = br.readLine()) != null) {
            String[] splitLine = line.split("  ");
            if (splitLine.length == 2) {
                // Line is : <md5signature> <file>
                result.put(splitLine[1], splitLine[0].trim());
            }
        }
        br.close();

        return result;

    }

}