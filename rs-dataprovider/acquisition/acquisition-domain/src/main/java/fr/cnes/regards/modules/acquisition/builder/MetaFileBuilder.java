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
package fr.cnes.regards.modules.acquisition.builder;

import fr.cnes.regards.modules.acquisition.domain.metadata.MetaFile;
import fr.cnes.regards.modules.acquisition.domain.metadata.ScanDirectory;

/**
 *
 * {@link MetaFile} builder
 *
 * @author Christophe Mertz
 *
 */
public final class MetaFileBuilder {

    /**
     * Current {@link MetaFile}
     */
    private final MetaFile metaFile;

    private MetaFileBuilder(MetaFile file) {
        this.metaFile = file;
    }

    /**
     * Create a {@link MetaFile} 
     * @return
     */
    public static MetaFileBuilder build() {
        final MetaFile mf = new MetaFile();
        return new MetaFileBuilder(mf);
    }

    /**
     * Get the current {@link MetaFile}
     * @return the current {@link MetaFileBuilder}
     */
    public MetaFile get() {
        return metaFile;
    }

    /**
     * Set the mandatory property to the current {@link MetaFile}
     * @return the current {@link MetaFileBuilder}
     */
    public MetaFileBuilder isMandatory() {
        metaFile.setMandatory(Boolean.TRUE);
        return this;
    }

    /**
     * Set the file name pattern property to the current {@link MetaFile}
     * @param pattern the file name pattern
     * @return the current {@link MetaFileBuilder}
     */
    public MetaFileBuilder withFilePattern(String pattern) {
        metaFile.setFileNamePattern(pattern);
        return this;
    }

    /**
     * Set the {@link ScanDirectory} property to the current {@link MetaFile}
     * @param scanDirectory the {@link ScanDirectory}
     * @return the current {@link MetaFileBuilder}
     */
    public MetaFileBuilder addScanDirectory(ScanDirectory scanDirectory) {
        metaFile.addScanDirectory(scanDirectory);
        return this;
    }

    /**
     * Set the invalid folder property to the current {@link MetaFile}
     * @param folder the invalid folder
     * @return the current {@link MetaFileBuilder}
     */
    public MetaFileBuilder withInvalidFolder(String folder) {
        metaFile.setInvalidFolder(folder);
        return this;
    }

    /**
     * Set the mandatory property to the current {@link MetaFile}
     * @param type the file type {@link String} value
     * @return the current {@link MetaFileBuilder}
     */
    public MetaFileBuilder withFileType(String type) {
        metaFile.setFileType(type);
        return this;
    }

    /**
     * Set the comment property to the current {@link MetaFile}
     * @param comment the comment {@link String} value
     * @return the current {@link MetaFileBuilder}
     */
    public MetaFileBuilder comment(String comment) {
        metaFile.setComment(comment);
        return this;
    }

}