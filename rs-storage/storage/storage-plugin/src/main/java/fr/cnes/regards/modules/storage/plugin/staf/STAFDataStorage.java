/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.storage.plugin.staf;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import fr.cnes.regards.framework.file.utils.DownloadUtils;
import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;
import fr.cnes.regards.framework.modules.plugins.annotations.PluginInit;
import fr.cnes.regards.framework.modules.plugins.annotations.PluginParameter;
import fr.cnes.regards.framework.staf.STAFArchive;
import fr.cnes.regards.framework.staf.STAFArchiveModeEnum;
import fr.cnes.regards.framework.staf.STAFException;
import fr.cnes.regards.framework.staf.STAFManager;
import fr.cnes.regards.framework.staf.STAFService;
import fr.cnes.regards.modules.storage.domain.database.DataFile;
import fr.cnes.regards.modules.storage.plugin.DataStorageInfo;
import fr.cnes.regards.modules.storage.plugin.INearlineDataStorage;
import fr.cnes.regards.modules.storage.plugin.ProgressManager;
import fr.cnes.regards.modules.storage.plugin.staf.domain.STAFController;
import fr.cnes.regards.modules.storage.plugin.staf.domain.protocol.STAFUrlFactory;

/**
 * Storage plugin to store plugin in CNES STAF System.<br/>
 * All files stored by an instance of this plugin are stored in the same STAF Archive.<br/>
 * The STAF Archive is configured with REGARDS generic plugin configuration system.<br/>
 * The storage manager, with the AllocationStrategy plugin must ensure that all the files to store
 * by this plugin has to be stored in the same STAF Archive.<br/>
 * <br/>
 * The global STAF System configuration is a static configuration read from property file of the staf-starter module.<br/>
 * The staf-starter module is use to ensure the unicity of the STAFManager object used to retreive STAF connections
 * from the available connection-pool.<br/>
 * <br/>
 * The plugin parameter <worksapceDirectory> is used to :
 * <ul>
 * <li>Handle file cut (if files are too big to be stored in the STAF System)</li>
 * <li>Temporary store files before transfer to the STAF archive.</li>
 * <li>Temporary create and store TAR files before sending to the STAF System (regrouping too small files to be archive alone in the STAF System)</li>
 * </ul>
 *
 * @author sbinda
 *
 */
@Plugin(author = "REGARDS Team",
        description = "Plugin to handle files storage for a specific archive of the CNES STAF System.",
        id = "STAFDataStorage", version = "1.0", contact = "regards@c-s.fr", licence = "GPLv3", owner = "CNES",
        url = "https://regardsoss.github.io/")
public class STAFDataStorage implements INearlineDataStorage<STAFWorkingSubset> {

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(STAFDataStorage.class);

    /**
     * URL FILE Protocole name
     */
    public static final String FILE_PROTOCOLE = "file";

    /**
     * STAF connections manager
     */
    @Autowired
    private STAFManager stafManager;

    /**
     * Plugin parameter containing STAF archive connection informations
     */
    @PluginParameter(name = "archiveParameters")
    private STAFArchive stafArchive;

    /**
     * STAF Controller to handle file preparation
     */
    private STAFController stafController;

    /**
     * STAF Plugin working directory.
     */
    @PluginParameter(name = "workspaceDirectory")
    private String workspaceDirectory;

    @PluginInit
    public void init() {
        // Initialize STAF Service
        STAFService stafService = stafManager.getNewArchiveAccessService(stafArchive);
        try {
            stafController = new STAFController(stafManager.getConfiguration(), Paths.get(workspaceDirectory),
                    stafService);
            stafController.initializeWorkspaceDirectories();
        } catch (IOException e) {
            LOG.error("[STAF Plugin] Error during plugin initialization", e);
        }
    }

    /**
     * Generate three working subsets divided by STAF archiving mode {@link STAFArchiveModeEnum}
     * @param pDataFiles {@link Collection} of {@link DataFile} to dispatch
     */
    @Override
    public Set<STAFWorkingSubset> prepare(Collection<DataFile> dataFiles) {
        LOG.info("[STAF] {} - Prepare action - Start", stafArchive.getArchiveName());
        Set<STAFWorkingSubset> workingSubsets = new HashSet<>();
        // Create workingSubset for file to stored dispatching by archive mode
        dispatchFilesToArchiveByArchiveMode(dataFiles).forEach((mode, files) -> {
            LOG.info("[STAF] {} - Prepare - Working subset created for archiving mode {} with {} files to store.",
                     stafArchive.getArchiveName(), mode.toString(), files.size());
            workingSubsets.add(new STAFWorkingSubset(files, mode));
        });
        LOG.info("[STAF] {} - Prepare action - End, {} working sets to store", stafArchive.getArchiveName(),
                 workingSubsets.size());
        return workingSubsets;
    }

    @Override
    public void store(STAFWorkingSubset pSubset, Boolean replaceMode, ProgressManager progressManager) {
        LOG.info("[STAF] {} - Store action - Start with Working subset mode : {}", stafArchive.getArchiveName(),
                 pSubset.getMode());
        Set<DataFile> alreadyStoredFiles = Sets.newHashSet();
        Set<DataFile> filesToStore = Sets.newHashSet();
        // Check if files are already stored
        dispatchAlreadyStoredFiles(pSubset.getDataFiles(), alreadyStoredFiles, filesToStore);
        // Files already stored in STAF. Only send stored event to listeners
        alreadyStoredFiles.forEach(file -> progressManager.storageSucceed(file, file.getUrl()));
        // Files need to be stored
        doStore(filesToStore, pSubset.getMode(), replaceMode, progressManager);
        LOG.info("[STAF] {} - Store action - End.", stafArchive.getArchiveName());
    }

    /**
     * Do the store action for the given {@link DataFile}s
     * @param pFilesToStore
     * @param pMode
     * @param pReplaceMode
     * @param pProgressManager
     */
    private void doStore(Set<DataFile> pFilesToStore, STAFArchiveModeEnum pMode, Boolean pReplaceMode,
            ProgressManager pProgressManager) {

        // 1. Dispatch files to store by stafNode
        Map<String, Set<Path>> filesToPrepare = Maps.newHashMap();
        for (DataFile file : pFilesToStore) {
            String stafNode = getStafNode(file);
            Path filePath;
            try {
                filePath = Paths.get(getPhysicalFile(file).getPath());
                Set<Path> filePaths;
                if (filesToPrepare.get(stafNode) != null) {
                    filePaths = filesToPrepare.get(stafNode);
                } else {
                    filePaths = Sets.newHashSet();
                }
                filePaths.add(filePath);
                filesToPrepare.put(stafNode, filePaths);
            } catch (IOException e) {
                LOG.error("[STAF] Error preparing file {}", file.getUrl().toString(), e.getMessage(), e);
            }
        }

        // 2. Perpare files to store
        stafController.prepareFilesToArchive(filesToPrepare, pMode);

        try {
            // 3. Do store all prepared files
            stafController.doArchivePreparedFiles(pReplaceMode);
        } catch (STAFException e) {
            LOG.error("[STAF] Error during file preparation", e);
        }

        Map<Path, URL> rawArchivedFiles = stafController.getRawFilesArchived();

        // 4. Log files stored.
        rawArchivedFiles.forEach((rawPath, storedUrl) -> LOG.info("[STAF] File {} stored into STAF at {}",
                                                                  rawPath.toString(), storedUrl.toString()));
        // 5. Inform progress manager for each file stored and each file not stored
        pFilesToStore.stream().forEach(fileToStore -> {
            boolean fileArchived = false;
            for (Entry<Path, URL> rawFile : rawArchivedFiles.entrySet()) {
                if ((rawFile.getKey() != null) && fileToStore.getUrl().getPath().equals(rawFile.getKey().toString())) {
                    fileArchived = true;
                    // Raw file successfully stored
                    pProgressManager.storageSucceed(fileToStore, rawFile.getValue());
                    break;
                }
            }
            if (!fileArchived) {
                // Raw file not stored
                LOG.error("[STAF] File {} has not been stored into STAF System.", fileToStore.getUrl().toString());
                pProgressManager.storageFailed(fileToStore, "Error during file archive");
            }
        });

    }

    private String getStafNode(DataFile pFile) {
        // TODO : How to calculate staf node from DataFile or AIP ?
        return "common/default";
    }

    /**
     * Dispatch the given files into two categories : Already stored files and files to store.
     * A {@link DataFile} is identified as stored if his url use the staf protocol.
     * @param pDataFiles {@link Collection} of {@link DataFile} to dispatch
     * @param pAlreadyStoredFiles {@link Set} of already stored {@link DataFile}
     * @param pFilesToStore {@link Set} of {@link DataFile} to store
     */
    private void dispatchAlreadyStoredFiles(Collection<DataFile> pDataFiles, Set<DataFile> pAlreadyStoredFiles,
            Set<DataFile> pFilesToStore) {
        pDataFiles.forEach(file -> {
            if (STAFController.STAF_PROTOCOLE.equals(file.getUrl().getProtocol())) {
                pAlreadyStoredFiles.add(file);
            } else {
                pFilesToStore.add(file);
            }
        });
    }

    /**
     * Dispatch the given {@link Set} of {@link DataFile} into STAF archive mode {@link STAFArchiveModeEnum}
     * @param pFiles {@link Collection}<{@link DataFile}>
     * @return {@link Map}<{@link STAFArchiveModeEnum}, {@link Set}<{@link DataFile}>
     */
    private Map<STAFArchiveModeEnum, Set<DataFile>> dispatchFilesToArchiveByArchiveMode(Collection<DataFile> pFiles) {
        Map<STAFArchiveModeEnum, Set<DataFile>> dispatchedFiles = new EnumMap<>(STAFArchiveModeEnum.class);
        pFiles.forEach(file -> {
            STAFArchiveModeEnum mode;
            try {
                mode = stafController.getFileArchiveMode(getDataFileSize(file));
                dispatchedFiles.merge(mode, new HashSet<>(Arrays.asList(file)), (olds, news) -> {
                    olds.addAll(news);
                    return olds;
                });
            } catch (IOException e) {
                LOG.error("STAF PLUGIN] {} - Prepare - Error getting size for file %s", file.getUrl().getPath(), e);
            }

        });
        return dispatchedFiles;
    }

    /**
     * Calculate physical file size for file {@link DataFile}.
     * If the file is not accessible, this method retrieve the file from is origineUrl in order to calculate his size.
     * If the file is retrieved, then the {@link DataFile} is updated to set the new origineUrl to the transfered file.
     * @param file {@link DataFile}
     * @return length of the physical file.
     * @throws IOException If the origineUrl of the given {@link DataFile} is not available.
     */
    private Long getDataFileSize(DataFile file) throws IOException {
        Long contentLenght = DownloadUtils.getContentLength(file.getUrl(), 1000).longValue();
        if (contentLenght == -1) {
            LOG.info("[STAF PLUGIN] {} - Prepare - Unknown length for file {}. Retrieving file ...",
                     file.getUrl().getPath());
            // Size undefined, we have to donwload file to know his size
            File pysicalFile = getPhysicalFile(file);
            contentLenght = pysicalFile.length();
            LOG.info("[STAF PLUGIN] {} - Prepare - Unknown length for file {}. File retrieved {}.",
                     file.getUrl().getPath(), pysicalFile.getPath());
            if (contentLenght == -1) {
                LOG.error("[STAF PLUGIN] {} - Prepare - Error retrieving file {}", file.getUrl().getPath());
            }
        }
        return contentLenght;
    }

    /**
     * Check that the physical file is accessible. If not, the file is retrieve from his origine url.
     * @param file {@link DataFile} to retrieve file
     * @throws STAFException If the file is not accessible
     */
    private File getPhysicalFile(DataFile file) throws IOException {
        File physicalFile;
        if (!FILE_PROTOCOLE.equals(file.getUrl().getProtocol())) {
            // File to transfert locally is temporarelly named with the file checksum to ensure unicity
            Path destinationFilePath = Paths.get(stafController.getWorkspaceTmpDirectory().toString(),
                                                 file.getChecksum());
            if (!destinationFilePath.toFile().exists()) {
                try {
                    LOG.info("[STAF PLUGIN] {} - Store - Retrieving file from {} to {}", stafArchive.getArchiveName(),
                             file.getUrl().toString(), destinationFilePath.toFile().getPath());
                    DownloadUtils.downloadAndCheckChecksum(file.getUrl(), destinationFilePath, file.getAlgorithm(),
                                                           file.getChecksum(), 100);
                    // File is now in our workspace, so change origine url
                } catch (IOException | NoSuchAlgorithmException e) {
                    String errorMsg = String.format("Error retrieving file from %s to %s", file.getUrl().getPath(),
                                                    destinationFilePath.toString());
                    LOG.error(errorMsg, e);
                    throw new IOException(e);
                }
            }
            physicalFile = destinationFilePath.toFile();
            if (!physicalFile.exists()) {
                String errorMsg = String.format("Error retrieving file from %s to %s", file.getUrl().getPath(),
                                                destinationFilePath.toString());
                throw new IOException(errorMsg);
            }
            file.setUrl(new URL(FILE_PROTOCOLE, null, destinationFilePath.toString()));
        } else {
            try {
                URI uri = file.getUrl().toURI();
                physicalFile = new File(uri.getPath());
            } catch (URISyntaxException e) {
                LOG.debug(e.getMessage(), e);
                physicalFile = new File(file.getUrl().getPath());
            }
        }
        return physicalFile;
    }

    @Override
    public void retrieve(STAFWorkingSubset pWorkingSubset, Path pDestinationPath, ProgressManager pProgressManager) {
        Set<URL> stafUrlsToRetrieve = Sets.newHashSet();
        for (DataFile fileToRetrieve : pWorkingSubset.getDataFiles()) {
            URL stafUrl = fileToRetrieve.getUrl();
            if (STAFUrlFactory.STAF_URL_PROTOCOLE.equals(stafUrl.getProtocol())) {
                stafUrlsToRetrieve.add(stafUrl);
            }
        }

        stafController.restoreFiles(stafUrlsToRetrieve, pDestinationPath,
                                    new STAFRetrieveListener(pProgressManager, pWorkingSubset));
    }

    @Override
    public Set<DataStorageInfo> getMonitoringInfos() {
        // TODO Auto-generated method stub
        return Sets.newHashSet();
    }

    @Override
    public void delete(Set<DataFile> pDataFiles, ProgressManager pProgressManager) {
        // TODO Auto-generated method stub

    }

}
