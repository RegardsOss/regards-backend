package fr.cnes.regards.modules.storage.plugin.utils.encryption;

/**
 * Cette classe <code>EncryptionException</code> modelise une exception du paquetage de cryptage.
 *
 * @author CS
 * @since 1.0
 * @version $Revision: 1.3 $
 */
public class EncryptionException extends Exception {

    /**
     * serialVersionUID field.
     *
     * @author CS
     * @since 5.2
     */
    private static final long serialVersionUID = 1L;

    /**
     * Constructeur
     *
     * @since 1.0
     */
    public EncryptionException() {
        super();
    }

    /**
     * Constructeur
     *
     * @param pException
     *            l'exception d'origine
     * @since 1.0
     */
    public EncryptionException(Throwable pException) {
        super(pException);
    }

    /**
     * Constructeur
     *
     * @param pMessage
     *            le message explicatif
     * @param pException
     *            l'exception d'origine
     * @since 1.0
     */
    public EncryptionException(String pMessage, Throwable pException) {
        super(pMessage, pException);
    }

    /**
     * Constructeur
     *
     * @param pMessage
     *            le message explicatif
     * @since 1.0
     */
    public EncryptionException(String pMessage) {
        super(pMessage);
    }

}
