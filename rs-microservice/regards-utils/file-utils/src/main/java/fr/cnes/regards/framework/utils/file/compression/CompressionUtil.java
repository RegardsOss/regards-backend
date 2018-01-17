/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.utils.file.compression;

import java.io.File;

/**
 * Cette classe definit des utilitaires associe au module de compression
 */
public final class CompressionUtil {

    /**
     * Extension de fichier gz
     */
    final static String GZ_EXT = ".gz";

    /**
     * Extension de fichier tar
     */
    final static String TAR_EXT = ".tar";

    /**
     * Extension de fichier zip
     */
    final static String ZIP_EXT = ".zip";

    /**
     * Constructeur
     */
    private CompressionUtil() {
        super();
    }

    /**
     * Cette methode permet de determiner a partir du nom du fichier
     * s'il s'agit d'un fichier que l'on sait decompresser.
     * @param pFile le fichier à tester
     * @return un booleen
     */
    public static boolean hasCompressedExtensionFilename(File pFile) {
        boolean ret = false;
        String name = pFile.getName().toLowerCase();
        if (name.endsWith(GZ_EXT) || name.endsWith(ZIP_EXT) || name.endsWith(TAR_EXT)) {
            ret = true;
        }
        return ret;
    }

    /**
     * Operateur de conversion
     * @param pFile
     * @return
     * @throws CompressionException
     */
    public static CompressionTypeEnum getCompressionType(File pFile) throws CompressionException {
        CompressionTypeEnum type = null;
        String name = pFile.getName().toLowerCase();
        if (!hasCompressedExtensionFilename(pFile)) {
            throw new CompressionException(String.format("The compression mode of file %s is not handled.", name));
        }

        if (name.endsWith(GZ_EXT)) {
            type = CompressionTypeEnum.GZIP;
        } else if (name.endsWith(ZIP_EXT)) {
            type = CompressionTypeEnum.ZIP;
        } else if (name.endsWith(TAR_EXT)) {
            type = CompressionTypeEnum.TAR;
        } else {
            throw new CompressionException(String.format("The compression mode of file %s is not handled.", name));
        }
        return type;
    }
}
