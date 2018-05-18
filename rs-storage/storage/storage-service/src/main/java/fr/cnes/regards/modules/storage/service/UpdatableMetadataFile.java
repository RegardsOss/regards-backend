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
package fr.cnes.regards.modules.storage.service;

import fr.cnes.regards.modules.storage.domain.database.StorageDataFile;

/**
 * Association between old AIP metadata file {@link StorageDataFile} and the new one for update.<br/>
 * Update of an AIP metadata file means the deletion of the old one and the creation of the new one.
 * @author Sylvain VISSIERE-GUERINET
 * @author Sébastien Binda
 */
public class UpdatableMetadataFile {

    /**
     * Previous {@link StorageDataFile} metadata file to replace.
     */
    private StorageDataFile oldOne;

    /**
     * New {@link StorageDataFile} metadata file.
     */
    private StorageDataFile newOne;

    public UpdatableMetadataFile(StorageDataFile oldOne, StorageDataFile newOne) {
        this.oldOne = oldOne;
        this.newOne = newOne;
    }

    public StorageDataFile getOldOne() {
        return oldOne;
    }

    public void setOldOne(StorageDataFile oldOne) {
        this.oldOne = oldOne;
    }

    public StorageDataFile getNewOne() {
        return newOne;
    }

    public void setNewOne(StorageDataFile newOne) {
        this.newOne = newOne;
    }
}
