package fr.cnes.regards.modules.storage.plugin;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;
import fr.cnes.regards.modules.storage.domain.database.StorageDataFile;
import fr.cnes.regards.modules.storage.domain.plugin.DataStorageAccessModeEnum;
import fr.cnes.regards.modules.storage.domain.plugin.INearlineDataStorage;
import fr.cnes.regards.modules.storage.domain.plugin.IProgressManager;
import fr.cnes.regards.modules.storage.domain.plugin.WorkingSubsetWrapper;
import fr.cnes.regards.modules.storage.plugin.datastorage.local.LocalWorkingSubset;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
@Plugin(author = "REGARDS Team", description = "Fake plugin to test retrieval priority", id = "NearlineNoRetrieveDataStorage",
        version = "1.0", contact = "regards@c-s.fr", licence = "GPLv3", owner = "CNES",
        url = "https://regardsoss.github.io/")
public class NearlineNoRetrieveDataStorage implements INearlineDataStorage<LocalWorkingSubset> {

    @Override
    public void retrieve(LocalWorkingSubset workingSubset, Path destinationPath, IProgressManager progressManager) {
        throw new IllegalStateException("This plugin should be less prioritized than the \"real\" nearline data storage.");
    }

    @Override
    public WorkingSubsetWrapper<LocalWorkingSubset> prepare(Collection<StorageDataFile> pDataFiles, DataStorageAccessModeEnum pMode) {
        throw new IllegalStateException("This plugin should be less prioritized than the \"real\" nearline data storage.");
    }

    @Override
    public boolean canDelete() {
        return true;
    }

    @Override
    public void delete(LocalWorkingSubset workingSubset, IProgressManager progressManager) {

    }

    @Override
    public void store(LocalWorkingSubset pWorkingSubset, Boolean pReplaceMode, IProgressManager pProgressManager) {

    }

    @Override
    public Long getTotalSpace() {
        return 900000000000L;
    }
}
