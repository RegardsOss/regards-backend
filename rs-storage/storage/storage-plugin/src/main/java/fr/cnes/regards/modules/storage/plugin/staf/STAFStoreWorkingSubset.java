/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.storage.plugin.staf;

import java.util.Set;

import fr.cnes.regards.framework.staf.STAFArchiveModeEnum;
import fr.cnes.regards.modules.storage.domain.database.DataFile;

/**
 * STAF set of files to handle for next store action.
 *
 * @author sbinda
 *
 */
public class STAFStoreWorkingSubset extends STAFWorkingSubset {

    private STAFArchiveModeEnum mode = STAFArchiveModeEnum.NORMAL;

    private boolean filesAleadyStored = false;

    public STAFStoreWorkingSubset(Set<DataFile> pDatafiles, STAFArchiveModeEnum pMode) {
        super(pDatafiles);
        mode = pMode;
    }

    public STAFStoreWorkingSubset(Set<DataFile> pDatafiles, boolean pFilesAlreadyStored) {
        super(pDatafiles);
        filesAleadyStored = pFilesAlreadyStored;
    }

    public STAFArchiveModeEnum getMode() {
        return mode;
    }

    public boolean isFilesAleadyStored() {
        return filesAleadyStored;
    }

}
