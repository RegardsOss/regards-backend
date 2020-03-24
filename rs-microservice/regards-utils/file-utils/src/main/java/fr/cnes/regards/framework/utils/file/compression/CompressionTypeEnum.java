/*
 * Copyright 2017-2020 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Enumeration for compression modes
 * @author Sébastien Binda
 */
public enum CompressionTypeEnum {

    ZIP("zip"),
    GZIP("gz"),
    TAR("tar"),
    UNKNOWN(""),
    Z("z");

    /**
     * Attribut permettant la journalisation.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(CompressionTypeEnum.class);

    /**
     * Main extension for the compression type
     */
    private final String fileExtension;

    CompressionTypeEnum(String fileExtension) {
        this.fileExtension = fileExtension;
    }

    public String getFileExtension() {
        return fileExtension;
    }

    public static CompressionTypeEnum parse(String name) {
        try {
            return CompressionTypeEnum.valueOf(name);
        } catch (IllegalArgumentException e) {
            LOGGER.error("unknown compression tools type : \"{}\"", name);
            LOGGER.error(e.getMessage(), e);
            return CompressionTypeEnum.UNKNOWN;
        }
    }
}
