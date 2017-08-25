package fr.cnes.regards.modules.storage.plugin.utils.encryption;

/**
 * <code>EncryptionTypeEnum</code> definit des valeurs enumeres permettant
 * de representer les modes de compression que le paquetage sait utiliser.
 * @author CS
 * @since 1.0
 * @version $Revision: 1.3 $
 */
public class EncryptionTypeEnum {

    /**
     * Constante definissant le mode AES
     * @since 1.0
     */
    public static EncryptionTypeEnum AES = new EncryptionTypeEnum("AES");

    /**
     * Constante definissant le mode MD5
     * @since 1.0
     */
    public static EncryptionTypeEnum MD5 = new EncryptionTypeEnum("MD5");

    /**
     * Contient la valeur numerique des modes de livraison possibles
     * @since 1.0
     */
    private String value_ = null;

    /**
     * Constructeur prive
     * @since 1.0
     * @param pType le type de chiffrage
     */
    private EncryptionTypeEnum(String pType) {
        value_ = pType;
    }

    /**
     * Constructeur par defaut interdit
     * @since 1.0
     */
    private EncryptionTypeEnum() {
        // explicit void
    }

    /**
     * Methode surchargee
     * @see java.lang.Object#toString()
     * @since 1.0
     * @return la chaine correspondante à l'objet
     */
    @Override
    public String toString() {
        return value_;
    }

}
