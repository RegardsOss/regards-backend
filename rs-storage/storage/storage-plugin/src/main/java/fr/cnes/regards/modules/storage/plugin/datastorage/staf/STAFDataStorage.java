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
import java.util.Optional;
import java.util.Set;
import java.util.StringJoiner;
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
import fr.cnes.regards.modules.storage.domain.StorageDataFileUtils;
import fr.cnes.regards.modules.storage.domain.database.StorageDataFile;
import fr.cnes.regards.modules.storage.domain.plugin.DataStorageAccessModeEnum;
import fr.cnes.regards.modules.storage.domain.plugin.INearlineDataStorage;
import fr.cnes.regards.modules.storage.domain.plugin.IProgressManager;
import fr.cnes.regards.modules.storage.domain.plugin.WorkingSubsetWrapper;

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
        description = "Plugin to handle files storage for a specific archive of the CNES STAF System.", id = "STAF",
        version = "1.0", contact = "regards@c-s.fr", licence = "GPLv3", owner = "CNES",
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
    @PluginParameter(name = STAF_STORAGE_TOTAL_SPACE,
            description = "total space, in byte, this data storage is allowed to use", label = "Total allocated space")
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

    @PluginInit
    public void init() {
        // Initialize STAF Service
        STAFService stafService = stafManager.getNewArchiveAccessService(stafArchive);
        try {
            stafController = new STAFController(stafManager.getConfiguration(), Paths.get(workspaceDirectory),
                    stafService);
            stafController.initializeWorkspaceDirectories();
        } catch (IOException e) {
            LOG.error("[STAFDataStorage Plugin] Error during plugin initialization", e);
        }
    }

    /**
     * Generate three working subsets divided by STAF archiving mode {@link STAFArchiveModeEnum}
     * @param dataFiles {@link Collection} of {@link StorageDataFile} to dispatch
     */
    @Override
    public WorkingSubsetWrapper<STAFWorkingSubset> prepare(Collection<StorageDataFile> dataFiles,
            DataStorageAccessModeEnum pMode) {
        WorkingSubsetWrapper<STAFWorkingSubset> wrapper = new WorkingSubsetWrapper<>();
        switch (pMode) {
            case RETRIEVE_MODE:
            case DELETION_MODE:
                prepareRetrieveWorkingsubsets(dataFiles, wrapper.getWorkingSubSets(), wrapper.getRejectedDataFiles());
                break;
            case STORE_MODE:
                prepareStoreWorkingsubsets(dataFiles, wrapper.getWorkingSubSets(), wrapper.getRejectedDataFiles());
                break;
            default:
                LOG.error("[STAFDataStorage Plugin] Unknown preparation mode {}", pMode.toString());
                break;
        }
        return wrapper;
    }

    @Override
    public boolean canDelete() {
        return canDelete;
    }

    public void prepareStoreWorkingsubsets(Collection<StorageDataFile> dataFiles, Set<STAFWorkingSubset> workingSubsets,
            Map<StorageDataFile, String> rejectedFiles) {
        LOG.info("[STAFDataStorage Plugin] {} - Prepare STORE action - Start", stafArchive.getArchiveName());
        // Create workingSubset for file to stored dispatching by archive mode
        dispatchFilesToArchiveBySTAFNode(dataFiles, rejectedFiles).forEach((path, files) -> {
            LOG.info("[STAFDataStorage Plugin] {} - Prepare STORE action - Working subset created for archiving STAF node {} with {} files to store.",
                     stafArchive.getArchiveName(), path.toString(), files.size());
            workingSubsets.add(new STAFStoreWorkingSubset(files, path));
        });
        LOG.info("[STAFDataStorage Plugin] {} - Prepare STORE action - End, {} working sets to store",
                 stafArchive.getArchiveName(), workingSubsets.size());
    }

    public void prepareRetrieveWorkingsubsets(Collection<StorageDataFile> dataFiles,
            Set<STAFWorkingSubset> workingSubsets, Map<StorageDataFile, String> rejectedFiles) {
        LOG.info("[STAFDataStorage Plugin] {} - Prepare RETRIEVE action - Start", stafArchive.getArchiveName());
        Set<URL> urls = dataFiles.stream().map(df -> extractThisStafUrl(df).get()).collect(Collectors.toSet());
        Set<AbstractPhysicalFile> preparedFiles = stafController.prepareFilesToRestore(urls);
        workingSubsets
                .add(new STAFRetrieveWorkingSubset(dataFiles.stream().collect(Collectors.toSet()), preparedFiles));
        LOG.info("[STAFDataStorage Plugin] {} - Prepare RETRIEVE action - End, {} working sets to retrieve",
                 stafArchive.getArchiveName(), workingSubsets.size());
    }

    @Override
    public void store(STAFWorkingSubset pSubset, Boolean replaceMode, IProgressManager progressManager) {
        STAFStoreWorkingSubset ws = (STAFStoreWorkingSubset) pSubset;
        if (ws != null) {
            LOG.info("[STAFDataStorage Plugin] {} - Store action - Start with Working subset for STAF Node : {}",
                     stafArchive.getArchiveName(), ws.getStafNode());
            Set<StorageDataFile> filesToStore = Sets.newHashSet();
            // Check if files are already stored
            dispatchAlreadyStoredFiles(pSubset.getDataFiles(), progressManager, filesToStore);
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
            stafController.restoreFiles(ws.getFilesToRestore(), pDestinationPath,
                                        new STAFRetrieveListener(pProgressManager, ws));
        } else {
            LOG.error("[STAFDataStorage Plugin] {} - Invalid workingsubset of Store type used for retrieve action.",
                      stafArchive.getArchiveName());
        }
    }

    @Override
    public void delete(STAFWorkingSubset pWorkingSubset, IProgressManager pProgressManager) {
        // 1. Prepare files
        Map<URL, StorageDataFile> urls = Maps.newHashMap();
        pWorkingSubset.getDataFiles().stream().forEach(f -> urls.put(extractThisStafUrl(f).get(), f));
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
     * Do the store action for the given {@link StorageDataFile}s
     * @param filesToStore Set of {@link StorageDataFile} of file to store.
     * @param sTAFNode STAF Node where to store file
     * @param replaceMode {@link Path} of the STAF Node where to store files.
     * @param progressManager{@link Boolean} replace if files exists into STAF ?
     */
    private void doStore(Set<StorageDataFile> filesToStore, Path sTAFNode, Boolean replaceMode,
            IProgressManager progressManager) {

        // 1. Dispatch files to store by stafNode
        Map<Path, Set<Path>> filesToPrepare = Maps.newHashMap();
        for (StorageDataFile file : filesToStore) {
            Path filePath;
            try {
                filePath = Paths.get(getPhysicalFile(file).getPath());
                Set<Path> filePaths;
                if (filesToPrepare.get(sTAFNode) != null) {
                    filePaths = filesToPrepare.get(sTAFNode);
                } else {
                    filePaths = Sets.newHashSet();
                }
                filePaths.add(filePath);
                filesToPrepare.put(sTAFNode, filePaths);
            } catch (IOException e) {
                LOG.error("[STAFDataStorage Plugin] Error preparing file {}", file.getUrls().toString(), e.getMessage(),
                          e);
            }
        }

        // 2. Prepare files to store
        Set<AbstractPhysicalFile> preparedFiles = stafController.prepareFilesToArchive(filesToPrepare);

        // 3. Do store all prepared files
        stafController.archiveFiles(preparedFiles, replaceMode);

        Map<Path, URL> rawFilesArchivedMap = stafController.getRawFilesArchived(preparedFiles);

        // 4. Log files stored.
        rawFilesArchivedMap
                .forEach((rawPath, storedUrl) -> LOG.info("[STAFDataStorage Plugin] File {} stored into STAF at {}",
                                                          rawPath.toString(), storedUrl.toString()));
        // 5. Inform progress manager for each file stored and each file not stored
        filesToStore.stream()
                .forEach(fileToStore -> notifyProgressManager(fileToStore, rawFilesArchivedMap, progressManager));
    }

    /**
     * Inform progress manager for file storage success or failure.
     * @param file {@link StorageDataFile} file to be stored
     * @param rawFilesArchivedMap map between files to store and really stored files.
     * @param progressManager {@link IProgressManager}
     */
    private void notifyProgressManager(StorageDataFile file, Map<Path, URL> rawFilesArchivedMap,
            IProgressManager progressManager) {
        boolean fileArchived = false;
        // For each archived file check if the originale file url match the file to check.
        for (Entry<Path, URL> rawFile : rawFilesArchivedMap.entrySet()) {

            // Compare original file urls with archived ones. If no one is found then the file has not been stored
            String archivedFileOrigineUrl = rawFile.getKey().toString();
            Set<URL> fileToStoreOrigineUrls = file.getUrls();

            Optional<URL> formerUrlOpt = fileToStoreOrigineUrls.stream()
                    .filter(url -> url.getPath().equals(archivedFileOrigineUrl)).findFirst();
            if (formerUrlOpt.isPresent()) {
                // we found a file that has been stored
                fileArchived = true;
                // Raw file successfully stored
                progressManager.storageSucceed(file, rawFile.getValue(), rawFile.getKey().toFile().length());
                break;
            }
        }
        if (!fileArchived) {
            // Raw file not stored
            LOG.error("[STAFDataStorage Plugin] File {} has not been stored into STAF System.",
                      file.getUrls().toString());
            progressManager.storageFailed(file, "Error during file archive");
        }
    }

    /**
     * Dispatch the given files into two categories : Already stored files and files to store.
     * A {@link StorageDataFile} is identified as stored if his url use the staf protocol.
     * @param pDataFiles {@link Collection} of {@link StorageDataFile} to dispatch
     * @param progressManager {@link IProgressManager} to use to notify that a file is already stored to the staf
     * @param pFilesToStore {@link Set} of {@link StorageDataFile} to store
     */
    private void dispatchAlreadyStoredFiles(Collection<StorageDataFile> pDataFiles, IProgressManager progressManager,
            Set<StorageDataFile> pFilesToStore) {
        pDataFiles.forEach(file -> {
            Optional<URL> thisStafUrlOpt = extractThisStafUrl(file);
            if (thisStafUrlOpt.isPresent()) {
                // Files already stored in STAF. Only send stored event to listeners
                progressManager.storageSucceed(file, thisStafUrlOpt.get(), file.getFileSize());
            } else {
                pFilesToStore.add(file);
            }
        });
    }

    private Optional<URL> extractThisStafUrl(StorageDataFile file) {
        return file.getUrls().stream().filter(url -> url.getHost().equals(stafArchive.getArchiveName())).findFirst();
    }

    /**
     * Dispatch the given {@link StorageDataFile}s by archive directory
     * @param pFiles {@link Collection}<{@link StorageDataFile}>
     * @param rejectedFiles List of rejected {@link StorageDataFile} associated to a {@link String} rejection cause
     * @return {@link Map}<{@link STAFArchiveModeEnum}, {@link Set}<{@link StorageDataFile}>
     */

    private Map<Path, Set<StorageDataFile>> dispatchFilesToArchiveBySTAFNode(Collection<StorageDataFile> pFiles,
            Map<StorageDataFile, String> rejectedFiles) {
        Map<Path, Set<StorageDataFile>> dispatchedFiles = Maps.newHashMap();
        for (StorageDataFile file : pFiles) {
            String stafNode = file.getStorageDirectory();
            if ((stafNode != null) && !stafNode.isEmpty() && STAFController.isValidSTAFNode(stafNode)) {
                Path path = Paths.get(file.getStorageDirectory());
                if (dispatchedFiles.containsKey(path)) {
                    dispatchedFiles.get(path).add(file);
                } else {
                    dispatchedFiles.put(path, new HashSet<>(Arrays.asList(file)));
                }
            } else {
                String rejectedCause;
                if ((stafNode == null) || stafNode.isEmpty()) {
                    rejectedCause = String.format(
                                                  "[STAFDataStorage Plugin] File \"%s\" ignored because it is not associated"
                                                          + " to any archive directory. See your allocation strategy configuration.",
                                                  file.getName());
                } else {
                    rejectedCause = String.format(
                                                  "[STAFDataStorage Plugin] File \"%s\" ignored because the given STAF Node \"{}\"is not valid"
                                                          + " to any archive directory. See your allocation strategy configuration.",
                                                  file.getName(), stafNode);
                }
                LOG.error(rejectedCause);
                rejectedFiles.put(file, rejectedCause);
            }
        }
        return dispatchedFiles;
    }

    /**
     * Check that the physical file is accessible. If not, the file is retrieve from his origine url.
     * @param file {@link StorageDataFile} to retrieve file
     * @throws STAFException If the file is not accessible
     */
    private File getPhysicalFile(StorageDataFile file) throws IOException {
        File physicalFile;
        // lets select one of the file implementation:
        URL accessibleUrl = StorageDataFileUtils.getAccessibleUrl(file);
        if (accessibleUrl == null) {
            StringJoiner stringifiedUrls = new StringJoiner(",");
            file.getUrls().forEach(url -> stringifiedUrls.add(url.toExternalForm()));
            String errorMsg = String.format(
                                            "Error trying to retrieve file(checksum: %s). We could not find any accessible url(Actual urls: %s)",
                                            file.getChecksum(), stringifiedUrls.toString());
            IOException ioe = new IOException(errorMsg);
            LOG.error(ioe.getMessage(), ioe);
            throw ioe;
        }
        if (!FILE_PROTOCOLE.equals(accessibleUrl.getProtocol())) {
            // File to transfert locally is temporarelly named with the file checksum to ensure unicity
            Path destinationFilePath = Paths.get(stafController.getWorkspaceTmpDirectory().toString(),
                                                 file.getChecksum());
            if (!destinationFilePath.toFile().exists()) {
                try {
                    LOG.info("[STAFDataStorage Plugin] {} - Store - Retrieving file from {} to {}",
                             stafArchive.getArchiveName(), file.getUrls().toString(),
                             destinationFilePath.toFile().getPath());
                    DownloadUtils.downloadAndCheckChecksum(accessibleUrl, destinationFilePath, file.getAlgorithm(),
                                                           file.getChecksum(), 100);
                    // File is now in our workspace, so change origine url
                } catch (IOException | NoSuchAlgorithmException e) {
                    String errorMsg = String.format("Error retrieving file from %s to %s",
                                                    accessibleUrl.toExternalForm(), destinationFilePath.toString());
                    LOG.error(errorMsg, e);
                    throw new IOException(e);
                }
            }
            physicalFile = destinationFilePath.toFile();
            if (!physicalFile.exists()) {
                String errorMsg = String.format("Error retrieving file from %s to %s", accessibleUrl.toExternalForm(),
                                                destinationFilePath.toString());
                throw new IOException(errorMsg);
            }
            file.getUrls().add(new URL(FILE_PROTOCOLE, null, destinationFilePath.toString()));
        } else {
            try {
                URI uri = accessibleUrl.toURI();
                physicalFile = new File(uri.getPath());
            } catch (URISyntaxException e) {
                LOG.debug(e.getMessage(), e);
                physicalFile = new File(accessibleUrl.getPath());
            }
        }
        return physicalFile;
    }

}
