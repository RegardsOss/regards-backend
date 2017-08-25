/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.storage.plugin;

import java.util.Set;

import fr.cnes.regards.modules.storage.domain.database.DataFile;
import fr.cnes.regards.modules.storage.domain.plugin.IWorkingSubset;

/**
 * STAF set of files to handle for next store action.
 *
 * @author sbinda
 *
 */
public class STAFWorkingSubset implements IWorkingSubset {

    private final Set<DataFile> datafiles;

    private String productTye;

    public STAFWorkingSubset(Set<DataFile> pDatafiles) {
        super();
        datafiles = pDatafiles;
    }

    @Override
    public Set<DataFile> getDataFiles() {
        return datafiles;
    }

}
