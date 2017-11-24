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
 * {@link ScanDirectory} builder
 *
 * @author Christophe Mertz
 *
 */
public final class ScanDirectoryBuilder {

    /**
     * Current {@link ScanDirectory}
     */
    private final ScanDirectory scanDirectory;

    private ScanDirectoryBuilder(ScanDirectory scanDir) {
        this.scanDirectory = scanDir;
    }

    /**
     * Create a {@link ScanDirectory}
     * @param scan directory name 
     * @return
     */
    public static ScanDirectoryBuilder build(String directory) {
        final ScanDirectory sd = new ScanDirectory();
        sd.setScanDir(directory);
        return new ScanDirectoryBuilder(sd);
    }

    //    public ScanDirectoryBuilder withDateAcquisition(OffsetDateTime odt) {
    //        scanDirectory.setLastAcqDate(odt);
    //        return this;
    //    }

    public ScanDirectory get() {
        return scanDirectory;
    }

}
