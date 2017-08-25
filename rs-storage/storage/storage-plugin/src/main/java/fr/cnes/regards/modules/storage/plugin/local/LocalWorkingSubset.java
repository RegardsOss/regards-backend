/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.storage.plugin.local;

import java.util.Set;

import fr.cnes.regards.modules.storage.domain.database.DataFile;
import fr.cnes.regards.modules.storage.plugin.IWorkingSubset;

public class LocalWorkingSubset implements IWorkingSubset {

    private Set<DataFile> dataFiles;

    public LocalWorkingSubset() {
    }

    public LocalWorkingSubset(Set<DataFile> dataFiles) {
        this.dataFiles = dataFiles;
    }

    @Override
    public Set<DataFile> getDataFiles() {
        return dataFiles;
    }

    public void setDataFiles(Set<DataFile> dataFiles) {
        this.dataFiles = dataFiles;
    }
}
