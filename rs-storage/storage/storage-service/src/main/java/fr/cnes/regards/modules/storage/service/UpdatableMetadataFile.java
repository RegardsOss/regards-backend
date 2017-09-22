/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.storage.service;

import fr.cnes.regards.modules.storage.domain.database.DataFile;

/**
 * Association between old AIP metadata file {@link DataFile} and the new one for update.
 * @author Sylvain VISSIERE-GUERINET
 * @author Sébastien Binda
 */
public class UpdatableMetadataFile {

    /**
     * Previous {@link DataFile} metadata file to replace.
     */
    private DataFile oldOne;

    /**
     * New {@link DataFile} metadata file.
     */
    private DataFile newOne;

    public UpdatableMetadataFile(DataFile oldOne, DataFile newOne) {
        this.oldOne = oldOne;
        this.newOne = newOne;
    }

    public DataFile getOldOne() {
        return oldOne;
    }

    public void setOldOne(DataFile oldOne) {
        this.oldOne = oldOne;
    }

    public DataFile getNewOne() {
        return newOne;
    }

    public void setNewOne(DataFile newOne) {
        this.newOne = newOne;
    }
}
