/*
 * Copyright 2017-2018 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
