/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.file.utils.compression;

import java.io.File;

/**
 * Cette exception est levee lorsqu'un fichier cree lors de la compression ou de la decompression existe deja et qu'il
 * risquerai d'etre ecrase
 */
public class FileAlreadyExistException extends CompressionException {

    /**
     * serialVersionUID field.
     */
    private static final long serialVersionUID = 1L;

    /**
     * File name
     */
    private File fileName_ = null;

    /**
     * Constructeur
     */
    public FileAlreadyExistException() {
        super();
    }

    /**
     * Constructeur de copie.
     *
     * @param pException
     *            L'exception a copier.
     */
    public FileAlreadyExistException(Throwable pException) {
        super(pException);
    }

    /**
     * Constructeur de copie permettant d'ajouter un message.
     *
     * @param pMessage
     *            Le message a ajouter
     * @param pException
     *            L'exception a copier.
     */
    public FileAlreadyExistException(String pMessage, Throwable pException) {
        super(pMessage, pException);
    }

    /**
     * Constructeur.
     *
     * @param pMessage
     *            Le message textuel a afficher, permettant d'ajouter de la semantique a l'erreur.
     */
    public FileAlreadyExistException(String pMessage) {
        super(pMessage);
    }

    /**
     * Constructeur.
     *
     * @param pMessage
     *            Le message textuel a afficher, permettant d'ajouter de la semantique a l'erreur.
     * @param pFileName
     *            Le fichier qui pose probleme.
     */
    public FileAlreadyExistException(String pMessage, File pFileName) {
        super(pMessage);
        setFileName(pFileName);
    }

    public File getFileName() {
        return fileName_;
    }

    public void setFileName(File fileName_) {
        this.fileName_ = fileName_;
    }

}
