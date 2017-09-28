package fr.cnes.regards.modules.storage.plugin;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Set;

import com.google.common.collect.Sets;

import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;
import fr.cnes.regards.modules.storage.domain.database.DataFile;
import fr.cnes.regards.modules.storage.plugin.local.LocalWorkingSubset;

@Plugin(author = "REGARDS Team", description = "SImple test plugin.", id = "SimpleTestNearLineStoragePlugin",
        version = "1.0", contact = "regards@c-s.fr", licence = "GPLv3", owner = "CNES",
        url = "https://regardsoss.github.io/")
public class SimpleNearLineStoragePlugin implements INearlineDataStorage<LocalWorkingSubset> {

    @Override
    public Set<LocalWorkingSubset> prepare(Collection<DataFile> pDataFiles, DataStorageAccessModeEnum pMode) {
        LocalWorkingSubset ws = new LocalWorkingSubset();
        Set<DataFile> dataFiles = Sets.newHashSet();
        dataFiles.addAll(pDataFiles);
        ws.setDataFiles(dataFiles);
        return Sets.newHashSet(ws);
    }

    @Override
    public void delete(Set<DataFile> pDataFiles, IProgressManager pProgressManager) {
        // TODO Auto-generated method stub

    }

    @Override
    public void store(LocalWorkingSubset pWorkingSubset, Boolean pReplaceMode, IProgressManager pProgressManager) {
        // TODO Auto-generated method stub

    }

    @Override
    public Set<DataStorageInfo> getMonitoringInfos() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void retrieve(LocalWorkingSubset pWorkingSubset, Path pDestinationPath, IProgressManager pProgressManager) {
        // TODO Auto-generated method stub
    }
}
