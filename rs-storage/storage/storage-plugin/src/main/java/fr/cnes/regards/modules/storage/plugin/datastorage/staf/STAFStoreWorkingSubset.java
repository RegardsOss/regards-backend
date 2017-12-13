/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.storage.plugin.datastorage.staf;

import java.nio.file.Path;
import java.util.Set;

import fr.cnes.regards.modules.storage.domain.database.StorageDataFile;

/**
 * STAF set of files to handle for next store action.
 *
 * @author sbinda
 *
 */
public class STAFStoreWorkingSubset extends STAFWorkingSubset {

    private boolean filesAleadyStored = false;

    private final Path stafNode;

    public STAFStoreWorkingSubset(Set<StorageDataFile> pDatafiles, Path pSTAFNode) {
        super(pDatafiles);
        stafNode = pSTAFNode;
    }

    public STAFStoreWorkingSubset(Set<StorageDataFile> pDatafiles, Path pSTAFNode, boolean pFilesAlreadyStored) {
        super(pDatafiles);
        filesAleadyStored = pFilesAlreadyStored;
        stafNode = pSTAFNode;
    }

    public boolean isFilesAleadyStored() {
        return filesAleadyStored;
    }

    public Path getStafNode() {
        return stafNode;
    }

}
