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

/**
 * Definit une liste d'enumere des modes de compression possibles.
 * 
 * @author Christophe Mertz
 *
 */
public class CompressionTypeEnum {

    /**
     * Constante definissant le mode ZIP
     */
    public static CompressionTypeEnum ZIP = new CompressionTypeEnum("ZIP", "zip");

    /**
     * Constante definissant le mode GZIP
     */
    public static CompressionTypeEnum GZIP = new CompressionTypeEnum("GZIP", "gz");

    /**
     * Constante définissant le mode TAR
     */
    public static CompressionTypeEnum TAR = new CompressionTypeEnum("TAR", "tar");

    /**
     * Constante definissant le mode Z (Unix Compress)
     */
    public static CompressionTypeEnum Z = new CompressionTypeEnum("Z", "z");

    /**
     * Contient la valeur numerique des modes de livraison possibles
     */
    private String value = null;

    /**
     * Contient la chaine de caractère correspondant à l'extension des fichiers de ce type
     */
    private String extension = null;

    private CompressionTypeEnum(String type, String fileExtension) {
        value = type;
        extension = fileExtension;
    }

    private CompressionTypeEnum() {
    }

    @Override
    public String toString() {
        return value;
    }

    public String getFileExtension() {
        return extension;
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
        if (pValue.equals(ZIP.value)) {
            returnValue = ZIP;
        } else if (pValue.equals(GZIP.value)) {
            returnValue = GZIP;
        } else if (pValue.equals(TAR.value)) {
            returnValue = TAR;
        } else if (pValue.equals(Z.value)) {
            returnValue = Z;
        }
        return returnValue;
    }
}
