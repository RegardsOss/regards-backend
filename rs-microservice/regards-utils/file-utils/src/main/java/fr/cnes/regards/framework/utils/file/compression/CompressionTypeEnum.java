/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.utils.file.compression;

/**
 * <code>CompressionTypeEnum</code> definit une liste d'enumere des modes de compression possibles.
 */
public enum CompressionTypeEnum {

    /**
     * Constante definissant le mode ZIP
     */
    ZIP("zip"),

    /**
     * Constante definissant le mode ZIP
     */
    GZIP("gz"),

    /**
     * Constante définissant le mode TAR
     */
    TAR("tar");

    /**
     * Contient la chaine de caractère correspondant à l'extension des fichiers de ce type
     */
    private final String fileExtension;

    CompressionTypeEnum(String fileExtension) {
        this.fileExtension = fileExtension;
    }

    public String getFileExtension() {
        return fileExtension;
    }

    /**
     * permet de recuperer l'instance de CompressionTypeEnum qui correspond a name. si name ne correspond a aucun
     * type, renvoie null
     */
    public static CompressionTypeEnum parse(String name) {
        try {
            return CompressionTypeEnum.valueOf(name);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
