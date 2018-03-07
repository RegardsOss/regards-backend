package fr.cnes.regards.modules.storage.plugin;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Set;

import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;
import fr.cnes.regards.modules.storage.domain.database.StorageDataFile;
import fr.cnes.regards.modules.storage.domain.plugin.DataStorageAccessModeEnum;
import fr.cnes.regards.modules.storage.domain.plugin.IOnlineDataStorage;
import fr.cnes.regards.modules.storage.domain.plugin.IProgressManager;
import fr.cnes.regards.modules.storage.domain.plugin.IWorkingSubset;
import fr.cnes.regards.modules.storage.domain.plugin.WorkingSubsetWrapper;
import fr.cnes.regards.modules.storage.plugin.datastorage.local.LocalDataStorage;
import fr.cnes.regards.modules.storage.plugin.datastorage.local.LocalWorkingSubset;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
@Plugin(author = "REGARDS Team", description = "Fake plugin to test retrieval priority", id = "OnlineNoRetrieveDataStorage",
        version = "1.0", contact = "regards@c-s.fr", licence = "GPLv3", owner = "CNES",
        url = "https://regardsoss.github.io/")
public class OnlineNoRetrieveDataStorage  implements IOnlineDataStorage<LocalWorkingSubset> {

    @Override
    public InputStream retrieve(StorageDataFile data) throws IOException {
        throw new IllegalStateException("This plugin should be less prioritized than the \"real\" online data storage.");
    }

    @Override
    public WorkingSubsetWrapper<LocalWorkingSubset> prepare(Collection<StorageDataFile> dataFiles, DataStorageAccessModeEnum mode) {
        return null;
    }

    @Override
    public boolean canDelete() {
        return false;
    }

    @Override
    public void delete(LocalWorkingSubset workingSubset, IProgressManager progressManager) {

    }

    @Override
    public void store(LocalWorkingSubset workingSubset, Boolean replaceMode, IProgressManager progressManager) {

    }

    @Override
    public Long getTotalSpace() {
        return null;
    }
}
