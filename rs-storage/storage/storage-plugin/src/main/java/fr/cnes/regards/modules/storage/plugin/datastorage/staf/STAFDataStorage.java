/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.storage.plugin.datastorage.staf;

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
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;
import fr.cnes.regards.framework.modules.plugins.annotations.PluginInit;
import fr.cnes.regards.framework.modules.plugins.annotations.PluginParameter;
import fr.cnes.regards.framework.staf.STAFController;
import fr.cnes.regards.framework.staf.STAFService;
import fr.cnes.regards.framework.staf.STAFSessionManager;
import fr.cnes.regards.framework.staf.domain.AbstractPhysicalFile;
import fr.cnes.regards.framework.staf.domain.STAFArchive;
import fr.cnes.regards.framework.staf.domain.STAFArchiveModeEnum;
import fr.cnes.regards.framework.staf.exception.STAFException;
import fr.cnes.regards.framework.utils.file.DownloadUtils;
import fr.cnes.regards.modules.storage.domain.database.DataFile;
import fr.cnes.regards.modules.storage.domain.plugin.DataStorageAccessModeEnum;
import fr.cnes.regards.modules.storage.domain.plugin.INearlineDataStorage;
import fr.cnes.regards.modules.storage.domain.plugin.IProgressManager;

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
     * URL FILE Protocole name
     */
    public static final String FILE_PROTOCOLE = "file";

    public static final String STAF_ARCHIVE_PARAMETER_NAME = "archiveParameters";

    public static final String STAF_WORKSPACE_PATH = "workspaceDirectory";

    /**
     * Plugin parameter name of the deletion option
     */
    public static final String STAF_STORAGE_DELETE_OPTION = "stafDeleteOption";

    /**
     * Plugin parameter name of the total space allocated to this data storage
     */
    public static final String STAF_STORAGE_TOTAL_SPACE = "stafTotalSpace";

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(STAFDataStorage.class);

    /**
     * STAF connections manager
     */
    @Autowired
    private STAFSessionManager stafManager;

    /**
     * Plugin parameter containing STAF archive connection informations
     */
    @PluginParameter(name = STAF_ARCHIVE_PARAMETER_NAME, label = "Archive parameters")
    private STAFArchive stafArchive;

    /**
     * Can this data storage delete files or not?
     */
    @PluginParameter(name = STAF_STORAGE_DELETE_OPTION, defaultValue = "true", label = "Deletion option")
    private Boolean canDelete;

    /**
     * Total space, in byte, this data storage is allowed to use
     */
    @PluginParameter(name = STAF_STORAGE_TOTAL_SPACE, description = "total space, in byte, this data storage is allowed to use", label = "Total allocated space")
    private Long totalSpace;

    /**
     * STAF Controller to handle file preparation
     */
    private STAFController stafController;

    /**
     * STAF Plugin working directory.
     */
    @PluginParameter(name = STAF_WORKSPACE_PATH, label = "Workspace directory")
    private String workspaceDirectory;

    public static Path getStafNode(DataFile pFile) {
        // TODO : How to calculate staf node from DataFile or AIP ?
        return Paths.get("common/default");
    }

    @PluginInit
    public void init() {
        // Initialize STAF Service
        STAFService stafService = stafManager.getNewArchiveAccessService(stafArchive);
        try {
            stafController = new STAFController(stafManager.getConfiguration(),
                                                Paths.get(workspaceDirectory),
                                                stafService);
            stafController.initializeWorkspaceDirectories();
        } catch (IOException e) {
            LOG.error("[STAFDataStorage Plugin] Error during plugin initialization", e);
        }
    }

    /**
     * Generate three working subsets divided by STAF archiving mode {@link STAFArchiveModeEnum}
     * @param dataFiles {@link Collection} of {@link DataFile} to dispatch
     */
    @Override
    public Set<STAFWorkingSubset> prepare(Collection<DataFile> dataFiles, DataStorageAccessModeEnum pMode) {
        switch (pMode) {
            case RETRIEVE_MODE:
                return prepareRetrieveWorkingsubsets(dataFiles);
            case STORE_MODE:
                return prepareStoreWorkingsubsets(dataFiles);
            default:
                LOG.error("[STAFDataStorage Plugin] Unknown preparation mode {}", pMode.toString());
                return Sets.newHashSet();
        }
    }

    @Override
    public boolean canDelete() {
        return canDelete;
    }

    public Set<STAFWorkingSubset> prepareStoreWorkingsubsets(Collection<DataFile> dataFiles) {
        LOG.info("[STAFDataStorage Plugin] {} - Prepare STORE action - Start", stafArchive.getArchiveName());
        Set<STAFWorkingSubset> workingSubsets = new HashSet<>();
        // Create workingSubset for file to stored dispatching by archive mode
        dispatchFilesToArchiveBySTAFNode(dataFiles).forEach((path, files) -> {
            LOG.info(
                    "[STAFDataStorage Plugin] {} - Prepare STORE action - Working subset created for archiving STAF node {} with {} files to store.",
                    stafArchive.getArchiveName(),
                    path.toString(),
                    files.size());
            workingSubsets.add(new STAFStoreWorkingSubset(files, path));
        });
        LOG.info("[STAFDataStorage Plugin] {} - Prepare STORE action - End, {} working sets to store",
                 stafArchive.getArchiveName(),
                 workingSubsets.size());
        return workingSubsets;
    }

    public Set<STAFWorkingSubset> prepareRetrieveWorkingsubsets(Collection<DataFile> dataFiles) {
        LOG.info("[STAFDataStorage Plugin] {} - Prepare RETRIEVE action - Start", stafArchive.getArchiveName());
        Set<STAFWorkingSubset> workingSubsets = new HashSet<>();
        Set<URL> urls = dataFiles.stream().map(df -> df.getUrl()).collect(Collectors.toSet());
        Set<AbstractPhysicalFile> preparedFiles = stafController.prepareFilesToRestore(urls);
        workingSubsets
                .add(new STAFRetrieveWorkingSubset(dataFiles.stream().collect(Collectors.toSet()), preparedFiles));
        LOG.info("[STAFDataStorage Plugin] {} - Prepare RETRIEVE action - End, {} working sets to retrieve",
                 stafArchive.getArchiveName(),
                 workingSubsets.size());
        return workingSubsets;
    }

    @Override
    public void store(STAFWorkingSubset pSubset, Boolean replaceMode, IProgressManager progressManager) {
        STAFStoreWorkingSubset ws = (STAFStoreWorkingSubset) pSubset;
        if (ws != null) {
            LOG.info("[STAFDataStorage Plugin] {} - Store action - Start with Working subset for STAF Node : {}",
                     stafArchive.getArchiveName(),
                     ws.getStafNode());
            Set<DataFile> alreadyStoredFiles = Sets.newHashSet();
            Set<DataFile> filesToStore = Sets.newHashSet();
            // Check if files are already stored
            dispatchAlreadyStoredFiles(pSubset.getDataFiles(), alreadyStoredFiles, filesToStore);
            // Files already stored in STAF. Only send stored event to listeners
            alreadyStoredFiles.forEach(file -> progressManager.storageSucceed(file, file.getUrl(), file.getFileSize()));
            // Files need to be stored
            doStore(filesToStore, ws.getStafNode(), replaceMode, progressManager);
            LOG.info("[STAFDataStorage Plugin] {} - Store action - End.", stafArchive.getArchiveName());
        } else {
            LOG.error("[STAFDataStorage Plugin] {} - Invalid workingsubset of Retrieve type used for store action.",
                      stafArchive.getArchiveName());
        }
    }

    @Override
    public Long getTotalSpace() {
        return totalSpace;
    }

    @Override
    public void retrieve(STAFWorkingSubset pWorkingSubset, Path pDestinationPath, IProgressManager pProgressManager) {
        STAFRetrieveWorkingSubset ws = (STAFRetrieveWorkingSubset) pWorkingSubset;
        if (ws != null) {
            stafController.restoreFiles(ws.getFilesToRestore(),
                                        pDestinationPath,
                                        new STAFRetrieveListener(pProgressManager, ws));
        } else {
            LOG.error("[STAFDataStorage Plugin] {} - Invalid workingsubset of Store type used for retrieve action.",
                      stafArchive.getArchiveName());
        }
    }

    @Override
    public void delete(Set<DataFile> pDataFiles, IProgressManager pProgressManager) {
        // 1. Prepare files
        Map<URL, DataFile> urls = Maps.newHashMap();
        pDataFiles.stream().forEach(f -> urls.put(f.getUrl(), f));
        Set<AbstractPhysicalFile> filesToDelete = stafController.prepareFilesToDelete(urls.keySet());
        // 2. Delete prepared files
        Set<URL> deletedSTAFFiles = stafController.deleteFiles(filesToDelete);
        urls.forEach((url, dataFile) -> {
            if (deletedSTAFFiles.contains(url)) {
                pProgressManager.deletionSucceed(dataFile);
            } else {
                pProgressManager.deletionFailed(dataFile, "STAF Error");
            }
        });
    }

    /**
     * Do the store action for the given {@link DataFile}s
     * @param pFilesToStore Set of {@link DataFile} of file to store.
     * @param pReplaceMode {@link Path} of the STAF Node where to store files.
     * @param pProgressManager{@link Boolean} replace if files exists into STAF ?
     */
    private void doStore(Set<DataFile> pFilesToStore, Path pSTAFNode, Boolean pReplaceMode,
            IProgressManager pProgressManager) {

        // 1. Dispatch files to store by stafNode
        Map<Path, Set<Path>> filesToPrepare = Maps.newHashMap();
        for (DataFile file : pFilesToStore) {
            Path filePath;
            try {
                filePath = Paths.get(getPhysicalFile(file).getPath());
                Set<Path> filePaths;
                if (filesToPrepare.get(pSTAFNode) != null) {
                    filePaths = filesToPrepare.get(pSTAFNode);
                } else {
                    filePaths = Sets.newHashSet();
                }
                filePaths.add(filePath);
                filesToPrepare.put(pSTAFNode, filePaths);
            } catch (IOException e) {
                LOG.error("[STAFDataStorage Plugin] Error preparing file {}",
                          file.getUrl().toString(),
                          e.getMessage(),
                          e);
            }
        }

        // 2. Prepare files to store
        Set<AbstractPhysicalFile> preparedFiles = stafController.prepareFilesToArchive(filesToPrepare);

        // 3. Do store all prepared files
        stafController.archiveFiles(preparedFiles, pReplaceMode);

        Map<Path, URL> rawArchivedFiles = stafController.getRawFilesArchived(preparedFiles);

        // 4. Log files stored.
        rawArchivedFiles.forEach((rawPath, storedUrl) -> LOG.info(
                "[STAFDataStorage Plugin] File {} stored into STAF at {}",
                rawPath.toString(),
                storedUrl.toString()));
        // 5. Inform progress manager for each file stored and each file not stored
        pFilesToStore.stream().forEach(fileToStore -> {
            boolean fileArchived = false;
            for (Entry<Path, URL> rawFile : rawArchivedFiles.entrySet()) {
                if ((rawFile.getKey() != null) && fileToStore.getUrl().getPath().equals(rawFile.getKey().toString())) {
                    fileArchived = true;
                    // Raw file successfully stored
                    pProgressManager
                            .storageSucceed(fileToStore, rawFile.getValue(), rawFile.getKey().toFile().length());
                    break;
                }
            }
            if (!fileArchived) {
                // Raw file not stored
                LOG.error("[STAFDataStorage Plugin] File {} has not been stored into STAF System.",
                          fileToStore.getUrl().toString());
                pProgressManager.storageFailed(fileToStore, "Error during file archive");
            }
        });

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
    private Map<Path, Set<DataFile>> dispatchFilesToArchiveBySTAFNode(Collection<DataFile> pFiles) {
        Map<Path, Set<DataFile>> dispatchedFiles = Maps.newHashMap();
        pFiles.forEach(file -> {
            Path stafNode;
            stafNode = getStafNode(file);
            dispatchedFiles.merge(stafNode, new HashSet<>(Arrays.asList(file)), (olds, news) -> {
                olds.addAll(news);
                return olds;
            });

        });
        return dispatchedFiles;
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
            Path destinationFilePath = Paths
                    .get(stafController.getWorkspaceTmpDirectory().toString(), file.getChecksum());
            if (!destinationFilePath.toFile().exists()) {
                try {
                    LOG.info("[STAFDataStorage Plugin] {} - Store - Retrieving file from {} to {}",
                             stafArchive.getArchiveName(),
                             file.getUrl().toString(),
                             destinationFilePath.toFile().getPath());
                    DownloadUtils.downloadAndCheckChecksum(file.getUrl(),
                                                           destinationFilePath,
                                                           file.getAlgorithm(),
                                                           file.getChecksum(),
                                                           100);
                    // File is now in our workspace, so change origine url
                } catch (IOException | NoSuchAlgorithmException e) {
                    String errorMsg = String.format("Error retrieving file from %s to %s",
                                                    file.getUrl().getPath(),
                                                    destinationFilePath.toString());
                    LOG.error(errorMsg, e);
                    throw new IOException(e);
                }
            }
            physicalFile = destinationFilePath.toFile();
            if (!physicalFile.exists()) {
                String errorMsg = String.format("Error retrieving file from %s to %s",
                                                file.getUrl().getPath(),
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

}
