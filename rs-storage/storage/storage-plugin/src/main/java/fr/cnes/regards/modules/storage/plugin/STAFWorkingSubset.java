/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.storage.plugin;

import java.util.Set;

import fr.cnes.regards.modules.storage.domain.database.DataFile;

/**
 * STAF set of files to handle for next store action.
 *
 * @author sbinda
 *
 */
public class STAFWorkingSubset implements IWorkingSubset {

    private final Set<DataFile> datafiles;

    public STAFWorkingSubset(Set<DataFile> pDatafiles) {
        super();
        datafiles = pDatafiles;
    }

    @Override
    public Set<DataFile> getDataFiles() {
        return datafiles;
    }

}
