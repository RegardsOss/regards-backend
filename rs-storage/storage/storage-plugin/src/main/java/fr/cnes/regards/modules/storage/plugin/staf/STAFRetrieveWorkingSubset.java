/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.storage.plugin.staf;

import java.util.Set;

import com.google.common.collect.Sets;

import fr.cnes.regards.modules.storage.domain.database.DataFile;
import fr.cnes.regards.modules.storage.plugin.staf.domain.AbstractPhysicalFile;

/**
 * STAF set of files to handle for next store action.
 *
 * @author sbinda
 *
 */
public class STAFRetrieveWorkingSubset extends STAFWorkingSubset {

    /**
     * Prepared {@link AbstractPhysicalFile}s STAF Files to retrieved.
     */
    private Set<AbstractPhysicalFile> filesToRestore = Sets.newHashSet();

    public STAFRetrieveWorkingSubset(Set<DataFile> pDatafiles, Set<AbstractPhysicalFile> pFilesToRestore) {
        super(pDatafiles);
        filesToRestore = pFilesToRestore;
    }

    public Set<AbstractPhysicalFile> getFilesToRestore() {
        return filesToRestore;
    }

}
