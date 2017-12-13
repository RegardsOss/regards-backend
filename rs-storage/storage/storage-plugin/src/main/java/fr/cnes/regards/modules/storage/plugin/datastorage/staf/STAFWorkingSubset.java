package fr.cnes.regards.modules.storage.plugin.datastorage.staf;

import java.util.Set;

import com.google.common.collect.Sets;

import fr.cnes.regards.modules.storage.domain.database.StorageDataFile;
import fr.cnes.regards.modules.storage.domain.plugin.IWorkingSubset;

public class STAFWorkingSubset implements IWorkingSubset {

    /**
     * Raw {@link StorageDataFile}s associate
     */
    private Set<StorageDataFile> datafiles = Sets.newHashSet();

    public STAFWorkingSubset(Set<StorageDataFile> pDatafiles) {
        super();
        datafiles = pDatafiles;
    }

    @Override
    public Set<StorageDataFile> getDataFiles() {
        return datafiles;
    }

}
