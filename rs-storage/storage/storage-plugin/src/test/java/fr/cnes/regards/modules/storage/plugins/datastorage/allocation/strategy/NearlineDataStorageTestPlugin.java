/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.storage.plugins.datastorage.allocation.strategy;

import java.nio.file.Path;
import java.util.Collection;

import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;
import fr.cnes.regards.modules.storage.domain.database.StorageDataFile;
import fr.cnes.regards.modules.storage.domain.plugin.DataStorageAccessModeEnum;
import fr.cnes.regards.modules.storage.domain.plugin.INearlineDataStorage;
import fr.cnes.regards.modules.storage.domain.plugin.IProgressManager;
import fr.cnes.regards.modules.storage.domain.plugin.WorkingSubsetWrapper;
import fr.cnes.regards.modules.storage.plugin.datastorage.local.LocalWorkingSubset;

/**
 * Nearline datastorage plugin for tests
 *
 * @author sbinda
 *
 */
@Plugin(author = "REGARDS Team", description = "NEarline test plugin.", id = NearlineDataStorageTestPlugin.PLUGIN_ID,
        version = "1.0", contact = "regards@c-s.fr", licence = "GPLv3", owner = "CNES",
        url = "https://regardsoss.github.io/")
public class NearlineDataStorageTestPlugin implements INearlineDataStorage<LocalWorkingSubset> {

    public final static String PLUGIN_ID = "NearlineTest";

    @Override
    public WorkingSubsetWrapper<LocalWorkingSubset> prepare(Collection<StorageDataFile> dataFiles,
            DataStorageAccessModeEnum mode) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean canDelete() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void delete(LocalWorkingSubset workingSubset, IProgressManager progressManager) {
        // TODO Auto-generated method stub

    }

    @Override
    public void store(LocalWorkingSubset workingSubset, Boolean replaceMode, IProgressManager progressManager) {
        // TODO Auto-generated method stub

    }

    @Override
    public Long getTotalSpace() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void retrieve(LocalWorkingSubset workingSubset, Path destinationPath, IProgressManager progressManager) {
        // TODO Auto-generated method stub

    }

}