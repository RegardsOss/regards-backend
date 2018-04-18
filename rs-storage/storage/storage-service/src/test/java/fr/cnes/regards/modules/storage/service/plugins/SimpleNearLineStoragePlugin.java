/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.storage.service.plugins;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;
import com.google.common.io.Files;

import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;
import fr.cnes.regards.modules.storage.domain.database.StorageDataFile;
import fr.cnes.regards.modules.storage.domain.plugin.DataStorageAccessModeEnum;
import fr.cnes.regards.modules.storage.domain.plugin.INearlineDataStorage;
import fr.cnes.regards.modules.storage.domain.plugin.IProgressManager;
import fr.cnes.regards.modules.storage.domain.plugin.WorkingSubsetWrapper;
import fr.cnes.regards.modules.storage.plugin.datastorage.local.LocalWorkingSubset;

@Plugin(author = "REGARDS Team", description = "SImple test plugin.", id = "SimpleTestNearLineStoragePlugin",
        version = "1.0", contact = "regards@c-s.fr", licence = "GPLv3", owner = "CNES",
        url = "https://regardsoss.github.io/")
public class SimpleNearLineStoragePlugin implements INearlineDataStorage<LocalWorkingSubset> {

    private static final Logger LOG = LoggerFactory.getLogger(SimpleNearLineStoragePlugin.class);

    @Override
    public WorkingSubsetWrapper<LocalWorkingSubset> prepare(Collection<StorageDataFile> pDataFiles,
            DataStorageAccessModeEnum pMode) {
        // Return only one workingSubset
        LOG.info("SimpleNearLineStoragePlugin preparing {} files for restoration", pDataFiles.size());
        LocalWorkingSubset ws = new LocalWorkingSubset();
        Set<StorageDataFile> dataFiles = Sets.newHashSet();
        dataFiles.addAll(pDataFiles);
        ws.setDataFiles(dataFiles);
        WorkingSubsetWrapper<LocalWorkingSubset> wrapper = new WorkingSubsetWrapper<>();
        wrapper.getWorkingSubSets().add(ws);
        return wrapper;
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

    @Override
    public void retrieve(LocalWorkingSubset pWorkingSubset, Path pDestinationPath, IProgressManager pProgressManager) {
        for (StorageDataFile file : pWorkingSubset.getDataFiles()) {
            URL fileUrl = file.getUrls().stream().findFirst().get();
            LOG.info("FILE REstored id : {} cs : {} path: {}", file.getId(), file.getChecksum(), fileUrl.getPath());
            try {
                File sourceFile = new File(fileUrl.getPath());
                File destinationFile = Paths.get(pDestinationPath.toString(), sourceFile.getName()).toFile();
                Files.copy(sourceFile, destinationFile);
                pProgressManager.restoreSucceed(file, fileUrl, Paths.get(destinationFile.getPath()));
            } catch (IOException e) {
                LOG.error(e.getMessage(), e);
                pProgressManager.restoreFailed(file, Optional.of(fileUrl), e.getMessage());
            }

        }
    }
}
