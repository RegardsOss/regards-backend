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
package fr.cnes.regards.modules.acquisition.domain.metadata;

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

    public MetaFile get() {
        return metaFile;
    }

    public MetaFileBuilder isMandatory() {
        metaFile.setMandatory(Boolean.TRUE);
        return this;
    }

    public MetaFileBuilder withFilePattern(String pattern) {
        metaFile.setFileNamePattern(pattern);
        return this;
    }

    public MetaFileBuilder addScanDirectory(ScanDirectory scanDirectory) {
        metaFile.addScanDirectory(scanDirectory);
        return this;
    }

    public MetaFileBuilder withInvalidFolder(String folder) {
        metaFile.setInvalidFolder(folder);
        return this;
    }

    public MetaFileBuilder withFileType(String type) {
        metaFile.setFileType(type);
        return this;
    }

    public MetaFileBuilder comment(String comment) {
        metaFile.setComment(comment);
        return this;
    }

}
