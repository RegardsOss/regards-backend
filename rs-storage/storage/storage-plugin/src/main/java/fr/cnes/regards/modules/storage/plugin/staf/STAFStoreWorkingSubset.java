/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.storage.plugin.staf;

import java.nio.file.Path;
import java.util.Set;

import fr.cnes.regards.modules.storage.domain.database.DataFile;

/**
 * STAF set of files to handle for next store action.
 *
 * @author sbinda
 *
 */
public class STAFStoreWorkingSubset extends STAFWorkingSubset {

    private boolean filesAleadyStored = false;

    private final Path stafNode;

    public STAFStoreWorkingSubset(Set<DataFile> pDatafiles, Path pSTAFNode) {
        super(pDatafiles);
        stafNode = pSTAFNode;
    }

    public STAFStoreWorkingSubset(Set<DataFile> pDatafiles, Path pSTAFNode, boolean pFilesAlreadyStored) {
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
