/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.storage.plugin;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;

import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;
import fr.cnes.regards.framework.modules.plugins.annotations.PluginInit;
import fr.cnes.regards.framework.modules.plugins.annotations.PluginParameter;
import fr.cnes.regards.framework.staf.ArchiveAccessModeEnum;
import fr.cnes.regards.framework.staf.STAFArchive;
import fr.cnes.regards.framework.staf.STAFArchiveModeEnum;
import fr.cnes.regards.framework.staf.STAFException;
import fr.cnes.regards.framework.staf.STAFManager;
import fr.cnes.regards.framework.staf.STAFService;
import fr.cnes.regards.modules.storage.domain.database.DataFile;

@Plugin(author = "REGARDS Team", description = "Plugin handling the storage on local file system", id = "STAF",
        version = "1.0", contact = "regards@c-s.fr", licence = "GPLv3", owner = "CNES",
        url = "https://regardsoss.github.io/")
public class STAFDataStorage implements INearlineDataStorage<STAFWorkingSubset> {

    @Autowired
    private STAFManager stafManager;

    @PluginParameter(name = "archiveParameters")
    private STAFArchive stafArchive;

    private STAFService stafService;

    @PluginParameter(name = "workspaceDirectory")
    private String workspaceDirectory;

    @PluginInit
    public void init() {
        // Initialize STAF Service
        stafService = stafManager.getNewArchiveAccessService(stafArchive);
    }

    @Override
    public void store(STAFWorkingSubset pSubset, Boolean replaceMode, ProgressManager progressManager) {

        // 1. First we have to check if datafile to store are available. If not, first transfer files
        // into a workspace directory
        Path workspace = Paths.get(workspaceDirectory);

        // Process each AIP to regroup by staf archive
        try {
            stafService.connectArchiveSystem(ArchiveAccessModeEnum.ARCHIVE_MODE);
        } catch (STAFException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    @Override
    public Set<STAFWorkingSubset> prepare(Collection<DataFile> dataFiles) {
        return null;
    }

    private Map<STAFArchiveModeEnum, Set<DataFile>> dispatchFilesToArchive(Set<DataFile> pFiles) {
        Map<STAFArchiveModeEnum, Set<DataFile>> dispatchedFiles = new EnumMap<>(STAFArchiveModeEnum.class);
        pFiles.forEach(file -> {
            STAFArchiveModeEnum mode = getFileArchiveMode(file.getFileSize());
            dispatchedFiles.merge(mode, new HashSet<>(Arrays.asList(file)), (olds, news) -> {
                olds.addAll(news);
                return olds;
            });
        });
        return dispatchedFiles;
    }

    private STAFArchiveModeEnum getFileArchiveMode(Double pFileSize) {

        if (pFileSize < stafManager.getConfiguration().getMinFileSize()) {
            return STAFArchiveModeEnum.TAR;
        }

        if (pFileSize > stafManager.getConfiguration().getMaxFileSize()) {
            return STAFArchiveModeEnum.CUT;
        }

        return STAFArchiveModeEnum.NORMAL;

    }

    @Override
    public void retrieve(STAFWorkingSubset pWorkingSubset, ProgressManager pProgressManager) {
        // TODO Auto-generated method stub

    }

    @Override
    public void delete(STAFWorkingSubset pWorkingSubset, ProgressManager pProgressManager) {
        // TODO Auto-generated method stub

    }

    @Override
    public Set<DataStorageInfo> getMonitoringInfos() {
        // TODO Auto-generated method stub
        return null;
    }

}
