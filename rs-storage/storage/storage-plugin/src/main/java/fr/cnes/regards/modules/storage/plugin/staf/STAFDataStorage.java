/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.storage.plugin.staf;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.stream.Collectors;

import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import fr.cnes.regards.framework.file.utils.CutFileUtils;
import fr.cnes.regards.framework.file.utils.DownloadUtils;
import fr.cnes.regards.framework.file.utils.compression.CompressManager;
import fr.cnes.regards.framework.file.utils.compression.CompressionException;
import fr.cnes.regards.framework.file.utils.compression.CompressionFacade;
import fr.cnes.regards.framework.file.utils.compression.CompressionTypeEnum;
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
import fr.cnes.regards.modules.storage.plugin.DataStorageInfo;
import fr.cnes.regards.modules.storage.plugin.INearlineDataStorage;
import fr.cnes.regards.modules.storage.plugin.ProgressManager;
import fr.cnes.regards.modules.storage.plugin.staf.domain.CutPhysicalFile;
import fr.cnes.regards.modules.storage.plugin.staf.domain.PhysicalFile;
import fr.cnes.regards.modules.storage.plugin.staf.domain.STAFPhysicalFile;
import fr.cnes.regards.modules.storage.plugin.staf.domain.TarPhysicalFile;

@Plugin(author = "REGARDS Team", description = "Plugin handling the storage on local file system", id = "STAF",
        version = "1.0", contact = "regards@c-s.fr", licence = "GPLv3", owner = "CNES",
        url = "https://regardsoss.github.io/")
public class STAFDataStorage implements INearlineDataStorage<STAFWorkingSubset> {

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(STAFDataStorage.class);

    private static final String TMP_DIRECTORY = "tmp";

    private static final String TAR_DIRECTORY = "tar";

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
     * Instance of STAFService for the configured STAFArchive
     */
    private STAFService stafService;

    /**
     * STAF Plugin working directory.
     */
    @PluginParameter(name = "workspaceDirectory")
    private String workspaceDirectory;

    @PluginInit
    public void init() {
        // Initialize STAF Service
        stafService = stafManager.getNewArchiveAccessService(stafArchive);
    }

    /**
     * URL STAF Protocole name
     */
    private static final String STAF_PROTOCOLE = "staf";

    /**
     * URL FILE Protocole name
     */
    private static final String FILE_PROTOCOLE = "file";

    /**
     * Generate three working subsets divided by STAF archiving mode {@link STAFArchiveModeEnum}
     * @param pDataFiles {@link Collection} of {@link DataFile} to dispatch
     */
    @Override
    public Set<STAFWorkingSubset> prepare(Collection<DataFile> dataFiles) {
        Set<STAFWorkingSubset> workingSubsets = new HashSet<>();
        // Create workingSubset for file to stored dispatching by archive mode
        dispatchFilesToArchiveByArchiveMode(dataFiles).forEach((mode, files) -> {
            LOG.info("[STAF PLUGIN] {} - Prepare - Number of files to archive in mode {} : {}",
                     stafArchive.getArchiveName(), mode.toString(), dataFiles.size());
            workingSubsets.add(new STAFWorkingSubset(files, mode));
        });
        return workingSubsets;
    }

    @Override
    public void store(STAFWorkingSubset pSubset, Boolean replaceMode, ProgressManager progressManager) {
        LOG.info("[STAF PLUGIN] {} - Store - Start store action for mode : {}", stafArchive.getArchiveName(),
                 pSubset.getMode());
        Set<DataFile> alreadyStoredFiles = Sets.newHashSet();
        Set<DataFile> filesToStore = Sets.newHashSet();
        // Check if files are already stored
        dispatchAlreadyStoredFiles(pSubset.getDataFiles(), alreadyStoredFiles, filesToStore);
        // Files already stored in STAF. Only send stored event to listeners
        alreadyStoredFiles.forEach(file -> progressManager.storageSucceed(file, file.getOriginUrl()));
        // Files need to be stored
        doStore(filesToStore, pSubset.getMode(), replaceMode, progressManager);
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

        // 0. Create temporal directory into workspace
        // TODO

        // Prepare informations needed to archvive for each file
        final Set<STAFPhysicalFile> filesToArchive = Sets.newHashSet();
        pFilesToStore.forEach(file -> {
            try {
                // 1. First we have to check if datafile to store are available. If not, first transfer files
                // into a workspace directory
                File physicalFileToArchive = getPhysicalFile(file);

                // 2. Manage file transformation if needed before staf storage
                switch (pMode) {
                    case CUT:
                        // 1. Cut file in part
                        Set<CutPhysicalFile> cutFiles = cutFile(physicalFileToArchive, file);
                        // 2. Add cut files : 1 -> X to archive
                        filesToArchive.addAll(cutFiles);
                        // STAF Location : staf://<ARCHIVE>/<NODE>/<full_file_name>?cut=3
                        break;
                    case TAR:
                        // 1. Check if TAR current exists in working dir
                        TarPhysicalFile tarFile = addFileToTar(physicalFileToArchive, file);
                        // 2. Add tar file to archive : 1 -> 1 to archive
                        filesToArchive.add(tarFile);
                        // STAF Location : staf://<ARCHIVE>/<NODE>/<tar_file>?file=<file_name>
                        break;
                    case NORMAL:
                        // 1. Create simple file
                        PhysicalFile simpleFile = new PhysicalFile();
                        simpleFile.setLocalLocation(physicalFileToArchive.getPath());
                        // 2. Add file to archive : 1 -> 1 to archive
                        filesToArchive.add(simpleFile);
                        // STAF Location : staf://<ARCHIVE>/<NODE>/<file_name>
                    default:
                        break;
                }

            } catch (IOException e) {
                LOG.error("[STAF PLUGIN] {} - {}", stafArchive.getArchiveName(), e.getMessage(), e);
                pProgressManager.storageFailed(file, e.getMessage());
            }
        });

        final List<String> archivedFiles = Lists.newArrayList();
        try {
            Map<String, String> filestoArchiveMap = filesToArchive.stream()
                    .collect(Collectors.toMap(STAFFileToStore::getLocalFilePath, STAFFileToStore::getStafFilePath));
            archivedFiles.addAll(doStoreFiles(filestoArchiveMap, pReplaceMode));
        } catch (STAFException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } finally {
            // Inform plugin manager for each file stored or in error
            filesToArchive.forEach(stafFileToArchive -> notifyFileStorage(stafFileToArchive, archivedFiles,
                                                                          pProgressManager));
        }

        // X. Delete temporal directory into workspace for cutFiles and downloaded files
        // TODO

    }

    private TarPhysicalFile addFileToTar(File pPhysicalFileToArchive, DataFile pFile) throws IOException {

        TarPhysicalFile physicalFile = new TarPhysicalFile();

        // 1. Calculate staf informations for given file
        String stafNode = getStafNode(pFile);

        // 2. Get lock on TAR directory for the node
        Path localTarDirectory = Paths.get(workspaceDirectory, TAR_DIRECTORY, stafNode);
        Path localCurrentTarDirectory = Paths.get(workspaceDirectory, TAR_DIRECTORY, stafNode, "current");
        if (!localCurrentTarDirectory.toFile().exists()) {
            Files.createDirectories(localCurrentTarDirectory);
        }
        // 3. Get lock on directory to avoid an other process to add file into.
        try (FileChannel fileChannel = FileChannel.open(localCurrentTarDirectory, StandardOpenOption.READ,
                                                        StandardOpenOption.CREATE);) {
            FileLock lock = fileChannel.lock();

            // 4. Move file in the tar directory
            Path sourceFile = Paths.get(pPhysicalFileToArchive.getPath());
            Path destinationFile = Paths.get(localCurrentTarDirectory.toString(), pPhysicalFileToArchive.getName());
            Files.move(sourceFile, destinationFile);

            // 5. Check TAR size
            long size = Files.walk(localTarDirectory).mapToLong(p -> p.toFile().length()).sum();
            if (size > stafManager.getConfiguration().getMaxTarSize()) {
                // No other files can be added in tar.
                // Create TAR
                String tarName = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmm"));
                CompressionFacade facade = new CompressionFacade();
                Vector<CompressManager> manager = facade
                        .compress(CompressionTypeEnum.TAR, localCurrentTarDirectory.toFile(), null,
                                  Paths.get(localTarDirectory.toString(), tarName).toFile(),
                                  localCurrentTarDirectory.toFile(), true, false);
                // New URL for the given dataFile need to be the STAF URL of the new TAR
                File tarFile = manager.firstElement().getCompressedFile();
                // Set the staf location for new file to archive (TAR FILE)
                physicalFile.setSTAFLocation(getTarSTAFUrl(stafArchive.getArchiveName(), stafNode, tarFile,
                                                           pPhysicalFileToArchive));
                // Set the origne DataFile associated to the new STAF File
                physicalFile.setDataFile(pFile);

            } else {
                // TAR does not need to be added to STAF yet
                // Set local TAR URL for current file
                physicalFile.setSTAFLocation(getTarLocalUrl(stafArchive.getArchiveName(), stafNode, destinationFile));
                // Set the origine DataFile associated to the new local TAR File
                physicalFile.setDataFile(pFile);
                // TODO : How to update this STAFLocation if after all files added to tar the tar has to be stored in STAF ?
            }

            lock.release();
            // TODO : Need to release lock if the Channel is closed by the Cloasble interface ?
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (CompressionException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return physicalFile;

    }

    private Set<CutPhysicalFile> cutFile(File pPhysicalFileToArchive, DataFile pFile) throws IOException {

        Set<CutPhysicalFile> cutFiles = Sets.newHashSet();

        // 1. Create cut temporary directory into workspace
        Path tmpCutDirectory = Paths.get(workspaceDirectory, TMP_DIRECTORY, pFile.getChecksum());
        if (!tmpCutDirectory.toFile().exists()) {
            tmpCutDirectory.toFile().mkdirs();
        }

        // 2. Calculate staf informations for given file
        String stafNode = getStafNode(pFile);

        // 3. Do cut files
        Set<File> cutedLocalFiles = CutFileUtils.cutFile(pPhysicalFileToArchive, tmpCutDirectory.toString(),
                                                         stafManager.getConfiguration().getMaxFileSize());

        // 4. Create for each cuted file the staf destination path
        cutedLocalFiles.forEach(cutFile -> cutFiles.add(new CutPhysicalFile(pPhysicalFileToArchive.getPath(),
                Paths.get(stafNode, cutFile.getName()).toString())));

        return cutFiles;

    }

    private void notifyFileStorage(DataFile pDataFile, List<STAFFileToStore> pFilesToArchive,
            List<String> pArchivedFiles, ProgressManager pProgressManager) {

        String localFilePath = pDataFile.getOriginUrl().getPath();
        if (pArchivedFiles.contains(localFilePath)) {
            pProgressManager.storageSucceed(pDataFile, getStafFilePathUrl(pFilesToArchive.get(localFilePath)));
        } else {
            pProgressManager.storageFailed(pDataFile, "STAF error");
        }
    }

    private String getStafNode(DataFile pFile) {
        // TODO : How to calculate staf node from DataFile or AIP ?
        return "common/default";
    }

    /**
     * Store given pFilesToArchiveMap in STAF.<br/>
     * For each element of pFilesToArchiveMap :<br/>
     *  - key : is the local path of the file to archive<br/>
     *  - value : is the staf file path to store.<br/>
     * @param pFilesToArchiveMap files to store
     * @param pReplaceMode replace file in STAF if already exists ?
     * @return list of successfuly stored local file path.
     * @throws STAFException staf store error
     */
    private List<String> doStoreFiles(Map<String, String> pFilesToArchiveMap, boolean pReplaceMode)
            throws STAFException {
        List<String> archivedFiles = Lists.newArrayList();
        stafService.connectArchiveSystem(ArchiveAccessModeEnum.ARCHIVE_MODE);
        try {
            archivedFiles = stafService.archiveFiles(pFilesToArchiveMap, "/", pReplaceMode);
        } finally {
            stafService.disconnectArchiveSystem(ArchiveAccessModeEnum.ARCHIVE_MODE);
        }
        return archivedFiles;
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
            if (STAF_PROTOCOLE.equals(file.getOriginUrl().getProtocol())) {
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
                mode = getFileArchiveMode(getDataFileSize(file));
                dispatchedFiles.merge(mode, new HashSet<>(Arrays.asList(file)), (olds, news) -> {
                    olds.addAll(news);
                    return olds;
                });
            } catch (IOException e) {
                LOG.error("STAF PLUGIN] {} - Prepare - Error getting size for file %s", file.getOriginUrl().getPath(),
                          e);
                // TODO : Inform the upper plugin manager thaht the file is not storable.
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
        Long fileSize;
        Integer contentLenght = file.getOriginUrl().openConnection().getContentLength();
        if (contentLenght == -1) {
            LOG.info("[STAF PLUGIN] {} - Prepare - Unknown length for file {}. Retrieving file ...",
                     file.getOriginUrl().getPath());
            // Size undefined, we have to donwload file to know his size
            File pysicalFile = getPhysicalFile(file);
            fileSize = pysicalFile.length();
            LOG.info("[STAF PLUGIN] {} - Prepare - Unknown length for file {}. File retrieved {}.",
                     file.getOriginUrl().getPath(), pysicalFile.getPath());
            if (contentLenght == -1) {
                LOG.error("[STAF PLUGIN] {} - Prepare - Error retrieving file {}", file.getOriginUrl().getPath());
            }
        } else {
            fileSize = contentLenght.longValue();
        }
        return fileSize;
    }

    /**
     * Check that the physical file is accessible. If not, the file is retrieve from his origine url.
     * @param file {@link DataFile} to retrieve file
     * @throws STAFException If the file is not accessible
     */
    private File getPhysicalFile(DataFile file) throws IOException {
        File physicalFile;
        if (!FILE_PROTOCOLE.equals(file.getOriginUrl().getProtocol())) {
            Path destinationFilePath = Paths.get(workspaceDirectory,
                                                 FilenameUtils.getName(file.getOriginUrl().getPath()));
            try {
                LOG.info("[STAF PLUGIN] {} - Store - Retrieving file from {} to {}", stafArchive.getArchiveName(),
                         file.getOriginUrl().getPath(), destinationFilePath.toFile().getPath());
                DownloadUtils.download(file.getOriginUrl(), destinationFilePath, null, file.getAlgorithm());
                // File is now in our workspace, so change origine url
                // TODO : Can I change the origine URL ?
                physicalFile = destinationFilePath.toFile();
                if (!physicalFile.exists()) {
                    String errorMsg = String.format("Error retrieving file from %s to %s",
                                                    file.getOriginUrl().getPath(), destinationFilePath.toString());
                    throw new IOException(errorMsg);
                }
                file.setOriginUrl(new URL(FILE_PROTOCOLE, null, destinationFilePath.toString()));
            } catch (IOException | NoSuchAlgorithmException e) {
                String errorMsg = String.format("Error retrieving file from %s to %s", file.getOriginUrl().getPath(),
                                                destinationFilePath.toString());
                LOG.error(errorMsg, e);
                throw new IOException(e);
            }
        } else {
            try {
                physicalFile = new File(file.getOriginUrl().toURI());
            } catch (URISyntaxException e) {
                physicalFile = new File(file.getOriginUrl().getPath());
            }
        }
        return physicalFile;
    }

    /**
     * The archive mode to store file in STAF is calculated with the file size.
     * The modes are {@link STAFArchiveModeEnum}
     * @param pFileSize int
     * @return
     */
    private STAFArchiveModeEnum getFileArchiveMode(Long pFileSize) {

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
