/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.storage.plugin.staf;

import java.util.Set;

import com.google.common.collect.Sets;

import fr.cnes.regards.framework.staf.STAFArchiveModeEnum;
import fr.cnes.regards.modules.storage.domain.database.DataFile;
import fr.cnes.regards.modules.storage.plugin.IWorkingSubset;

/**
 * STAF set of files to handle for next store action.
 *
 * @author sbinda
 *
 */
public class STAFWorkingSubset implements IWorkingSubset {

    private Set<DataFile> datafiles = Sets.newHashSet();

    private STAFArchiveModeEnum mode = STAFArchiveModeEnum.NORMAL;

    private boolean filesAleadyStored = false;

    public STAFWorkingSubset(Set<DataFile> pDatafiles, STAFArchiveModeEnum pMode) {
        super();
        datafiles = pDatafiles;
        mode = pMode;
    }

    public STAFWorkingSubset(Set<DataFile> pDatafiles, boolean pFilesAlreadyStored) {
        super();
        datafiles = pDatafiles;
        filesAleadyStored = pFilesAlreadyStored;
    }

    @Override
    public Set<DataFile> getDataFiles() {
        return datafiles;
    }

    public STAFArchiveModeEnum getMode() {
        return mode;
    }

    public boolean isFilesAleadyStored() {
        return filesAleadyStored;
    }

}
