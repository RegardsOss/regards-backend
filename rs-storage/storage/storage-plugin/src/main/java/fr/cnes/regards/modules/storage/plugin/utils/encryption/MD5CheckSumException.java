package fr.cnes.regards.modules.storage.plugin.utils.encryption;

/**
 * Cette classe <code>EncryptionException</code> modelise une exception du paquetage de cryptage.
 *
 * @author CS
 * @since 5.5
 */
public class MD5CheckSumException extends Exception {

    /**
     * serialVersionUID field.
     *
     * @author CS
     * @since 5.5
     */
    private static final long serialVersionUID = 1L;

    /**
     * Constructeur
     *
     * @since 5.5
     */
    public MD5CheckSumException() {
        super();
    }

    /**
     * Constructeur
     *
     * @param pException
     *            l'exception d'origine
     * @since 5.5
     */
    public MD5CheckSumException(Throwable pException) {
        super(pException);
    }

    /**
     * Constructeur
     *
     * @param pMessage
     *            le message explicatif
     * @param pException
     *            l'exception d'origine
     * @since 5.5
     */
    public MD5CheckSumException(String pMessage, Throwable pException) {
        super(pMessage, pException);
    }

    /**
     * Constructeur
     *
     * @param pMessage
     *            le message explicatif
     * @since 5.5
     */
    public MD5CheckSumException(String pMessage) {
        super(pMessage);
    }

}
