/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.utils.file.compression;

import java.io.File;

/**
 * Cette exception est levee lorsqu'un fichier cree lors de la compression ou de la decompression existe deja et qu'il
 * risquerai d'etre ecrase
 */
public class FileAlreadyExistException extends CompressionException {

    /**
     * File name
     */
    private final File fileName;

    /**
     * Constructeur de copie permettant d'ajouter un message.
     * @param pMessage Le message a ajouter
     * @param pException L'exception a copier.
     */
    public FileAlreadyExistException(String pMessage, Throwable pException) {
        super(pMessage, pException);
        fileName = null;
    }

    /**
     * Constructeur.
     * @param pMessage Le message textuel a afficher, permettant d'ajouter de la semantique a l'erreur.
     */
    public FileAlreadyExistException(String pMessage) {
        super(pMessage);
        fileName = null;
    }

    /**
     * Constructeur.
     * @param pMessage Le message textuel a afficher, permettant d'ajouter de la semantique a l'erreur.
     * @param pFileName Le fichier qui pose probleme.
     */
    public FileAlreadyExistException(String pMessage, File pFileName) {
        super(pMessage);
        fileName = pFileName;
    }
}
