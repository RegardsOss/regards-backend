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

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;

/**
 * Enumeration for compression modes
 * @author Sébastien Binda
 */
public enum CompressionTypeEnum {

    ZIP("zip", "zip"),
    GZIP("gz", "gz", "z"),
    TAR("tar", "tar");

    /**
     * Attribut permettant la journalisation.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(CompressionTypeEnum.class);

    /**
     * Main extension for the compression type
     */
    private final String fileExtension;

    /**
     * List of extensions handled by the compression type
     */
    private final Set<String> handledExtensions = Sets.newHashSet();

    CompressionTypeEnum(String fileExtension, String... handledExtensions) {
        this.fileExtension = fileExtension;
        if (handledExtensions != null) {
            for (String ext : handledExtensions) {
                this.handledExtensions.add(ext);
            }
        }
    }

    public String getFileExtension() {
        return fileExtension;
    }

    public Set<String> getHandledExtensions() {
        return this.handledExtensions;
    }

    public static CompressionTypeEnum parse(String name) {
        if (name != null) {
            for (CompressionTypeEnum type : CompressionTypeEnum.values()) {
                if (type.getHandledExtensions().contains(name.toLowerCase())) {
                    return type;
                }
            }
        }
        LOGGER.error("Unhandled extension \"{}\" for compression tools", name);
        return null;
    }
}
