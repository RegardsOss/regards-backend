/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.utils.file.compression;

/**
 * <code>CompressionTypeEnum</code> definit une liste d'enumere des modes de compression possibles.
 */
public class CompressionTypeEnum {

    /**
     * Constante definissant le mode ZIP
     */
    public static CompressionTypeEnum ZIP = new CompressionTypeEnum("ZIP", "zip");

    /**
     * Constante definissant le mode ZIP
     */
    public static CompressionTypeEnum GZIP = new CompressionTypeEnum("GZIP", "gz");

    /**
     * Constante définissant le mode TAR
     */
    public static CompressionTypeEnum TAR = new CompressionTypeEnum("TAR", "tar");

    /**
     * Contient la valeur numerique des modes de livraison possibles
     */
    private String value_ = null;

    /**
     * Contient la chaine de caractère correspondant à l'extension des fichiers de ce type
     */
    private String fileExtension_ = null;

    /**
     * Constructeur prive
     *
     * @param pType
     *            le type de compression à appliquer.
     */
    private CompressionTypeEnum(String pType, String pFileExtension) {
        value_ = pType;
        fileExtension_ = pFileExtension;
    }

    /**
     * Constructeur par defaut interdit
     *
     */
    private CompressionTypeEnum() {
        // explicit void
    }

    /**
     * Methode surchargee
     *
     * @return l'objet sous forme d'une chaine de caractères.
     */
    @Override
    public String toString() {
        return value_;
    }

    public String getFileExtension() {
        return fileExtension_;
    }

    /**
     * permet de recuperer l'instance de CompressionTypeEnum qui correspond a pValue. si pValue ne correspond a aucun
     * type, renvoie null
     *
     * @param pValue
     * @return
     */
    public static CompressionTypeEnum parse(String pValue) {
        CompressionTypeEnum returnValue = null;
        if (pValue.equals(ZIP.value_)) {
            returnValue = ZIP;
        } else if (pValue.equals(GZIP.value_)) {
            returnValue = GZIP;
        } else if (pValue.equals(TAR.value_)) {
            returnValue = TAR;
        }
        return returnValue;
    }
}
