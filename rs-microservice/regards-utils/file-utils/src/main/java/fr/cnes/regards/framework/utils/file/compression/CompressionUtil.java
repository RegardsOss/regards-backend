/*
 * Copyright 2017-2019 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.framework.utils.file.compression;

import java.io.File;

/**
 * Cette classe definit des utilitaires associe au module de compression
 */
public final class CompressionUtil {

    /**
     * Extension de fichier gz
     */
    static final String GZ_EXT = ".gz";

    /**
     * Extension de fichier tar
     */
    static final String TAR_EXT = ".tar";

    /**
     * Extension de fichier zip
     */
    static final String ZIP_EXT = ".zip";

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
    private static boolean hasCompressedExtensionFilename(File pFile) {
        boolean ret = false;
        String name = pFile.getName().toLowerCase();
        if (name.endsWith(GZ_EXT) || name.endsWith(ZIP_EXT) || name.endsWith(TAR_EXT)) {
            ret = true;
        }
        return ret;
    }

    /**
     * Operateur de conversion
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
