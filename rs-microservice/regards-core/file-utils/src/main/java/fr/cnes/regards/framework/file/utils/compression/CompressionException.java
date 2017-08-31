/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.file.utils.compression;

/**
 *
 * Cette classe designe l'exception que peut lever le paquetage de Compression. Elle doit etre traitee par le module ou
 * l'application appelant.
 */
public class CompressionException extends Exception {

    /**
     * serialVersionUID field.
     */
    private static final long serialVersionUID = 1L;

    /**
     * Constructeur
     */
    public CompressionException() {

    }

    /**
     * Constructeur de copie.
     *
     * @param pException
     *            L'exception a copier.
     */
    public CompressionException(Throwable pException) {
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
    public CompressionException(String pMessage, Throwable pException) {
        super(pMessage, pException);
    }

    /**
     * Constructeur.
     *
     * @param pMessage
     *            Le message textuel a afficher, permettant d'ajouter de la semantique a l'erreur.
     */
    public CompressionException(String pMessage) {
        super(pMessage);
    }

}
