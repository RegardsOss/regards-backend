package fr.cnes.regards.modules.storage.plugin.datastorage.staf;

import java.util.Set;

import com.google.common.collect.Sets;

import fr.cnes.regards.modules.storage.domain.database.DataFile;
import fr.cnes.regards.modules.storage.domain.plugin.IWorkingSubset;

public class STAFWorkingSubset implements IWorkingSubset {

    /**
     * Raw {@link DataFile}s associate
     */
    private Set<DataFile> datafiles = Sets.newHashSet();

    public STAFWorkingSubset(Set<DataFile> pDatafiles) {
        super();
        datafiles = pDatafiles;
    }

    @Override
    public Set<DataFile> getDataFiles() {
        return datafiles;
    }

}
