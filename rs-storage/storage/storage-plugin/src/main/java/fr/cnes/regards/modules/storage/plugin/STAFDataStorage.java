/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.storage.plugin;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Multimap;

import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;
import fr.cnes.regards.framework.modules.plugins.annotations.PluginInit;
import fr.cnes.regards.framework.modules.plugins.annotations.PluginParameter;
import fr.cnes.regards.modules.storage.domain.AIP;
import fr.cnes.regards.modules.storage.domain.database.DataFile;
import fr.cnes.regards.modules.storage.plugin.staf.STAFArchiveModeEnum;
import fr.cnes.regards.modules.storage.plugin.staf.STAFConfiguration;
import fr.cnes.regards.modules.storage.plugin.staf.STAFService;

@Plugin(author = "REGARDS Team", description = "Plugin handling the storage on local file system", id = "STAF",
        version = "1.0", contact = "regards@c-s.fr", licence = "GPLv3", owner = "CNES",
        url = "https://regardsoss.github.io/")
public class STAFDataStorage implements IDataStorage<STAFWorkingSubset> {

    @PluginParameter(name = "STAFParameters")
    private STAFConfiguration configuration;

    private STAFService stafService;

    @PluginInit
    public void init() {
        // Initialize STAF Service
        stafService = new STAFService(configuration);
    }

    @Override
    public void store(STAFWorkingSubset pSubset, Boolean replaceMode, ProgressManager progressManager) {
        // Process each AIP to regroup by staf archive
        // stafService.connectArchiveSystem(pSTAFArchive, ArchiveAccessModeEnum.ARCHIVE_MODE);
    }

    @Override
    public Set<STAFWorkingSubset> prepare(Multimap<AIP, List<DataFile>> pAips) {
        return null;
    }

    private Map<STAFArchiveModeEnum, List<File>> handleAIP(AIP aip) {

        // Files list to add like that in archive : "standard" archive
        final List<File> pFileListStandard = new ArrayList<>();
        // Files list to cut before archive its (files too big): "cut" archive
        final List<File> pFileListCut = new ArrayList<>();
        // Files list to regroup before archive its (files too small): "tar" archive
        final List<File> pFileListTar = new ArrayList<>();

        // 1. Find from the AIP the product type
        String productType = getProductType(aip);

        // 2. get Files to archive from AIP
        List<File> filesToArchive = getFilesToArchive(aip);

        // 3. Dispatch files to archive by archiving mode
        return dispatchFilesToArchive(filesToArchive);
    }

    private String getProductType(AIP aip) {
        // TODO : Comment connaître le nom du produit depuis l'AIP ?
        return "TODO";
    }

    private List<File> getFilesToArchive(AIP aip) {
        // TODO : Comment récupérer les fichiers depuis l'AIP ?
        return new ArrayList<>();
    }

    private Map<STAFArchiveModeEnum, List<File>> dispatchFilesToArchive(List<File> pFiles) {
        Map<STAFArchiveModeEnum, List<File>> dispatchedFiles = new EnumMap<>(STAFArchiveModeEnum.class);
        pFiles.forEach(file -> {
            STAFArchiveModeEnum mode = getFileArchiveMode(file);
            dispatchedFiles.merge(mode, Arrays.asList(file), (olds, news) -> {
                olds.addAll(news);
                return olds;
            });
        });
        return dispatchedFiles;
    }

    private STAFArchiveModeEnum getFileArchiveMode(File pFile) {

        final long fileSize = pFile.length();

        if (fileSize < configuration.getMinFileSize()) {
            return STAFArchiveModeEnum.TAR;
        }

        if (fileSize > configuration.getMaxFileSize()) {
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

    @Override
    public DataStorageType getType() {
        return DataStorageType.NEARLINE;
    }

}
