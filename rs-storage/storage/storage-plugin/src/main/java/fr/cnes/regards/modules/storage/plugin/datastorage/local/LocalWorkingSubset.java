/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.storage.plugin.datastorage.local;

import java.util.Set;

import fr.cnes.regards.modules.storage.domain.database.StorageDataFile;
import fr.cnes.regards.modules.storage.domain.plugin.IWorkingSubset;

/**
 * {@link IWorkingSubset} implementation for {@link LocalDataStorage}
 * @author svissier
 */
public class LocalWorkingSubset implements IWorkingSubset {

    /**
     * Data files from this working subset
     */
    private Set<StorageDataFile> dataFiles;

    /**
     * Default constructor
     */
    public LocalWorkingSubset() {
    }

    /**
     * Constructor setting the parameter as attribute
     * @param dataFiles
     */
    public LocalWorkingSubset(Set<StorageDataFile> dataFiles) {
        this.dataFiles = dataFiles;
    }

    @Override
    public Set<StorageDataFile> getDataFiles() {
        return dataFiles;
    }

    /**
     * Set the data files
     * @param dataFiles
     */
    public void setDataFiles(Set<StorageDataFile> dataFiles) {
        this.dataFiles = dataFiles;
    }
}
