/*
 * LICENSE_PLACEHOLDER
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
