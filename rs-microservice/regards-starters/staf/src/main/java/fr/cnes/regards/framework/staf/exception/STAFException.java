/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.staf.exception;

/**
 * Classe d'exception pour le paquetage de communication avec le STAF.
 */
@SuppressWarnings("serial")
public class STAFException extends Exception {

    /**
     * Constructeur de copie.
     *
     * @param pException L'exception a copier.
     */
    public STAFException(Throwable pException) {
        super(pException);
    }

    /**
     * Constructeur de copie permettant d'ajouter un message.
     *
     * @param pMessage Le message a ajouter
     * @param pException L'exception a copier.
     */
    public STAFException(String pMessage, Throwable pException) {
        super(pMessage, pException);
    }

    /**
     * Constructeur.
     *
     * @param pMessage Le message textuel a afficher, permettant d'ajouter de la
     * semantique a l'erreur.
     */
    public STAFException(String pMessage) {
        super(pMessage);
    }

}
