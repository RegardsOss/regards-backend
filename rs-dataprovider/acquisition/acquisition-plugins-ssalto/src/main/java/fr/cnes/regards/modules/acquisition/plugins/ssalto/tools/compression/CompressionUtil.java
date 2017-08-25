/*
 * Copyright 2017 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
 *
 * This file is part of REGARDS.
 *
 * REGARDS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * REGARDS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with REGARDS. If not, see <http://www.gnu.org/licenses/>.
 */
package fr.cnes.regards.modules.acquisition.plugins.ssalto.tools.compression;

import java.io.File;

import fr.cnes.regards.modules.acquisition.plugins.ssalto.tools.compression.exception.CompressionException;

/**
 * Cette classe definit des utilitaires associe au module de compression
 * 
 * @author CS
 * @since 1.0
 * @version $Revision: 1.4 $
 */
public class CompressionUtil {

    /**
     * Extension de fichier gz
     * @since 1.0 
     */
    final static String GZ_EXT = ".gz";

    /**
     * Extension de fichier gz
     * @since 1.0 
     */
    final static String TAR_EXT = ".tar";

    /**
     * Extension de fichier gz
     * @since 1.0 
     */
    final static String ZIP_EXT = ".zip";

    /**
     * Constructeur
     * 
     * 
     * @since 1.0
     */
    private CompressionUtil() {
        super();
    }

    /**
     * Cette methode permet de determiner a partir du nom du fichier
     * s'il s'agit d'un fichier que l'on sait decompresser.
     * @param pFile le fichier à tester
     * @return un booleen
     * @since 1.0
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
     * @since 1.0
     */
    public static CompressionTypeEnum getCompressionType(File pFile) throws CompressionException {
        CompressionTypeEnum type = null;
        String name = pFile.getName().toLowerCase();
        if (!hasCompressedExtensionFilename(pFile)) {
            throw new CompressionException(String.format("The compression mode of file '%s' is not handled.", name));
        }

        if (name.endsWith(GZ_EXT)) {
            type = CompressionTypeEnum.GZIP;
        } else if (name.endsWith(ZIP_EXT)) {
            type = CompressionTypeEnum.ZIP;
        } else if (name.endsWith(TAR_EXT)) {
            type = CompressionTypeEnum.TAR;
        } else {
            throw new CompressionException(String.format("The compression mode of file '%s' is not handled.", name));
        }
        return type;
    }
}
