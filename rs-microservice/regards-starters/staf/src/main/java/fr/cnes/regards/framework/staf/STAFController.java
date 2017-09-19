/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.staf;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import fr.cnes.regards.framework.file.utils.ChecksumUtils;
import fr.cnes.regards.framework.file.utils.CutFileUtils;
import fr.cnes.regards.framework.staf.domain.AbstractPhysicalFile;
import fr.cnes.regards.framework.staf.domain.ArchiveAccessModeEnum;
import fr.cnes.regards.framework.staf.domain.PhysicalCutFile;
import fr.cnes.regards.framework.staf.domain.PhysicalCutPartFile;
import fr.cnes.regards.framework.staf.domain.PhysicalFileStatusEnum;
import fr.cnes.regards.framework.staf.domain.PhysicalNormalFile;
import fr.cnes.regards.framework.staf.domain.PhysicalTARFile;
import fr.cnes.regards.framework.staf.domain.STAFArchiveModeEnum;
import fr.cnes.regards.framework.staf.domain.STAFConfiguration;
import fr.cnes.regards.framework.staf.event.IClientCollectListener;
import fr.cnes.regards.framework.staf.exception.STAFException;
import fr.cnes.regards.framework.staf.exception.STAFTarException;
import fr.cnes.regards.framework.staf.protocol.STAFURLException;
import fr.cnes.regards.framework.staf.protocol.STAFURLFactory;
import fr.cnes.regards.framework.staf.protocol.STAFURLParameter;

/**
 * STAF Controller to handle STAF recommandations to store and retrieve files.<br/>
 * Recommandations are :
 * <ul>
 * <li>Do not store short files as single files into STAF Archive. Store short files into a single archive file (TAR File)</li>
 * <li>Do not store files that exceed Xoctets. Cut too big files into multiple files in STAF</li>
 * </ul>
 * <br/>
 * To archive files :
 * <ul>
 * <li> 1. prepare files with {@link #prepareFilesToArchive} method.</li>
 * <li> 2. do archive prepared files with {@link #archiveFiles} method.</li>
 * <li> 3. Retrieve link between raw files and archived files with the {@link #getRawFilesArchived} method.</li>
 * </ul>
 * To retrieve files :
 * <ul>
 * <li> 1. prepare files with {@link #prepareFilesToRestore} method. </li>
 * <li> 2. do restore prepared files with {@link #restoreFiles} method. All files are restored in the given directory.</li>
 * </ul>
 * To delete files :
 * <ul>
 * <li>1. prepare files with {@link #prepareFilesToDelete} method. </li>
 * <li>2. do delete files with {@link #deleteFiles} method. </li>
 * </ul>
 *
 * @author Sébastien Binda
 *
 */
public class STAFController {

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(STAFController.class);

    /**
     * URL STAF Protocole name
     */
    public static final String STAF_PROTOCOLE = "staf";

    /**
     * STAF temporary directory used to handle cut and tar files.
     */
    private static final String TMP_DIRECTORY = "tmp";

    /**
     * STAF global configuration
     */
    private final STAFConfiguration stafConfiguration;

    /**
     * Instance of STAFService for the configured STAFArchive
     */
    private final STAFService stafService;

    /**
     * Path to the local workspace of the STAF System. Use to process files before transfert to STAF System.
     */
    private final Path localWorkspace;

    /**
     * Controller to handle TAR files.
     */
    private final TARController tarController;

    /**
     * Constructor
     * @param pStafConfiguration {@link STAFConfiguration} Global STAF Configuration
     * @param pLocalWorkspace {@link Path} STAF local workspace
     * @param pSTAFService {@link STAFService} STAF Service to call STAF Commands.
     * @throws IOException Thrown if workspace is not available.
     */
    public STAFController(STAFConfiguration pStafConfiguration, Path pLocalWorkspace, STAFService pSTAFService)
            throws IOException {
        super();
        stafConfiguration = pStafConfiguration;
        localWorkspace = pLocalWorkspace;
        stafService = pSTAFService;
        // Initialize TAR Controller
        tarController = new TARController(stafConfiguration, localWorkspace);
    }

    /**
     * Store the given {@link AbstractPhysicalFile} into STAF.<br/>
     * @param {@link Set}<{@link AbstractPhysicalFile}> files to archive/
     * @param {@link Path} into STAF where to archive prepared files.
     * @param pReplaceMode replace file in STAF if already exists ?
     * @return Set of successfuly stored {@link AbstractPhysicalFile}.
     * @throws STAFException STAF store error
     */
    public Set<AbstractPhysicalFile> archiveFiles(Set<AbstractPhysicalFile> pFilesToArchive, boolean pReplaceMode) {
        Set<String> archivedFiles;
        // Create map bewteen localFile to archive and destination file path into STAF
        Map<Path, Path> localFileToArchiveMap = Maps.newHashMap();
        pFilesToArchive.forEach(stafFile -> {
            if (PhysicalFileStatusEnum.TO_STORE.equals(stafFile.getStatus())) {
                if ((stafFile.getLocalFilePath() != null) && (stafFile.getSTAFFilePath() != null)) {
                    localFileToArchiveMap.put(stafFile.getLocalFilePath(), stafFile.getSTAFFilePath());
                } else {
                    stafFile.setStatus(PhysicalFileStatusEnum.ERROR);
                    LOG.warn("[STAF] Undefined file to archive for origine(local)={} and destination(STAF)={}",
                             stafFile.getLocalFilePath(), stafFile.getSTAFFilePath());
                }
            }
        });

        // If there is files to store. Run STAF archive.
        try {
            if (!localFileToArchiveMap.isEmpty()) {
                stafService.connectArchiveSystem(ArchiveAccessModeEnum.ARCHIVE_MODE);
                try {
                    archivedFiles = stafService.archiveFiles(localFileToArchiveMap, Paths.get("/"), pReplaceMode);
                    archivedFiles.forEach(archivedFile ->
                    // For each file to store, check if the file has really been stored and set the status to STORED.
                // @formatter:off
                pFilesToArchive
                    .stream()
                    .filter(f -> PhysicalFileStatusEnum.TO_STORE.equals(f.getStatus()))
                    .filter(f -> archivedFile.equals(f.getLocalFilePath().toString()))
                    .forEach(f -> {
                        // Set status to STORED
                        f.setStatus(PhysicalFileStatusEnum.STORED);
                        // Delete local temporary files
                        deleteTemporaryFiles(f);
                    }));
                // @formatter:on
                } finally {
                    stafService.disconnectArchiveSystem(ArchiveAccessModeEnum.ARCHIVE_MODE);
                }
            }
        } catch (STAFException e) {
            LOG.error(e.getMessage(), e);
        } finally {
            // delete temporary files created.
            pFilesToArchive.stream().forEach(this::deleteTemporaryFiles);

            // handle special case of TAR PENDING. Status is not STORED but LOCALY_STORED.
            // @formatter:off
            pFilesToArchive
                .stream()
                .filter(physicalFile -> STAFArchiveModeEnum.TAR.equals(physicalFile.getArchiveMode()))
                .filter(tar -> PhysicalFileStatusEnum.PENDING.equals(tar.getStatus()))
                .forEach(tar -> {
                    LOG.info("[STAF] Current TAR {} not big enought for transfert to STAF System. This TAR is localy stored waiting for new files.",
                             ((PhysicalTARFile)tar).getLocalTarDirectory());
                    tar.setStatus(PhysicalFileStatusEnum.LOCALY_STORED);
                });
            // @formatter:on
        }

        // Return all Physical file stored
        // @formatter:off
        return pFilesToArchive.stream()
                .filter(file -> PhysicalFileStatusEnum.STORED.equals(file.getStatus()))
                .collect(Collectors.toSet());
        // @formatter:on
    }

    /**
     * Delete all {@link AbstractPhysicalFile} from STAF System.
     * After each deletion success or error, a notification is sent
     * @param pPhysicalFilesToDelete
     * @return {@link Set}<{@link URL}> STAF URLs of successfully deleted files
     */
    public Set<URL> deleteFiles(Set<AbstractPhysicalFile> pPhysicalFilesToDelete) {

        Map<Path, Path> filesToReplace = Maps.newHashMap();
        Map<Path, AbstractPhysicalFile> stafFilePathsToDelete = Maps.newHashMap();
        Set<PhysicalTARFile> stafTARToRetrieve = Sets.newHashSet();

        // 1. For each file to delete, calculate staf file paths to delete.
        // If file are in a TAR, check if the full TAR is to delete or if files have to be remove from the TAR.
        for (AbstractPhysicalFile physicalFileToDelete : pPhysicalFilesToDelete) {
            if (PhysicalFileStatusEnum.TO_DELETE.equals(physicalFileToDelete.getStatus())) {
                switch (physicalFileToDelete.getArchiveMode()) {
                    case CUT:
                        // Add each part to delete
                        PhysicalCutFile cutFile = (PhysicalCutFile) physicalFileToDelete;
                        cutFile.getCutedFileParts().forEach(part -> {
                            part.setStatus(PhysicalFileStatusEnum.TO_DELETE);
                            stafFilePathsToDelete.put(part.getSTAFFilePath(), part);
                        });
                        break;
                    case CUT_PART:
                    case NORMAL:
                        // Add file to delete
                        stafFilePathsToDelete.put(physicalFileToDelete.getSTAFFilePath(), physicalFileToDelete);
                        break;
                    case TAR:
                        // If TAR is locally store, delete files in local dir
                        PhysicalTARFile tar = (PhysicalTARFile) physicalFileToDelete;
                        if (tar.getLocalTarDirectory() != null) {
                            tarController.deleteFilesFromLocalTAR(tar);
                        } else {
                            stafTARToRetrieve.add(tar);
                        }
                        break;
                    default:
                        break;
                }
            }
        }

        if (!stafTARToRetrieve.isEmpty() || !stafFilePathsToDelete.isEmpty()) {
            try {
                stafService.connectArchiveSystem(ArchiveAccessModeEnum.ARCHIVE_MODE);
            } catch (STAFException e) {
                LOG.error("[STAF] Error connecting to STAF Archive", e);
                return Sets.newHashSet();
            }

            // 2. Prepare TAR Files for delation or replacement (replacement if only a part of the files in the TAR are to delete)
            for (PhysicalTARFile tar : stafTARToRetrieve) {
                try {
                    retrieveFileFromSTAF(tar);
                    if (tarController.deleteFilesFromTAR(tar)) {
                        stafFilePathsToDelete.put(tar.getSTAFFilePath(), tar);
                    } else {
                        filesToReplace.put(tar.getLocalFilePath(), tar.getSTAFFilePath());
                    }
                } catch (STAFException e) {
                    LOG.error("[STAF] Error deleting files from local TAR {}", tar.getLocalFilePath(), e);
                    tar.setStatus(PhysicalFileStatusEnum.ERROR);
                }
            }

            // 3. run files deletions
            try {
                Set<Path> notDeletedPaths = stafService.deleteFiles(stafFilePathsToDelete.keySet());
                for (Entry<Path, AbstractPhysicalFile> stafFileToDelete : stafFilePathsToDelete.entrySet()) {
                    if (!notDeletedPaths.contains(stafFileToDelete.getKey())) {
                        LOG.info("[STAF] {} - FILE deleted from STAF {}", stafFileToDelete.getKey(),
                                 stafService.getStafArchive().getArchiveName());
                        // Retrieve original file associted to this deleted path
                        stafFileToDelete.getValue().setStatus(PhysicalFileStatusEnum.DELETED);
                    }
                }
            } catch (STAFException e) {
                LOG.error("[STAF] Error during STAF Deletion", e.getMessage(), e);
            }

            // 4. run TAR replacements
            try {
                Set<String> replacedFiles = stafService.archiveFiles(filesToReplace, Paths.get("/"), true);
                for (PhysicalTARFile tar : stafTARToRetrieve) {
                    // If tar is not in error status and is present in the replaced files so the files in TAR are well DELETED.
                    if (!PhysicalFileStatusEnum.ERROR.equals(tar.getStatus())
                            && replacedFiles.contains(tar.getLocalFilePath().toString())) {
                        LOG.info("[STAF] TAR File replaced {}", tar.getSTAFFilePath());
                        tar.setStatus(PhysicalFileStatusEnum.DELETED);
                    }
                }
            } catch (STAFException e) {
                LOG.error("[STAF] Error during STAF Deletion", e.getMessage(), e);
            }

            try {
                stafService.disconnectArchiveSystem(ArchiveAccessModeEnum.ARCHIVE_MODE);
            } catch (STAFException e) {
                LOG.error("Error during STAF deconnection.", e);
            }
        }

        // 5. return URLs of STAF Files deleted.
        return getDeletedFileUrls(pPhysicalFilesToDelete);
    }

    /**
     * Calculate all STAF URLs for each {@link AbstractPhysicalFile} in DELETED status.
     * @param pPhysicalFilesToDelete {@link Set}<{@link AbstractPhysicalFile}>
     * @return {@link Set}<{@link URL}> of each deleted file in STAF.
     */
    private Set<URL> getDeletedFileUrls(Set<AbstractPhysicalFile> pPhysicalFilesToDelete) {
        Set<URL> stafDeleteFileURLs = Sets.newHashSet();
        // @formatter:off
        pPhysicalFilesToDelete
                .stream()
                .filter(f -> PhysicalFileStatusEnum.DELETED.equals(f.getStatus()))
                .forEach(fileToDelete -> {
                 // @formatter:on
                    try {
                        switch (fileToDelete.getArchiveMode()) {
                            case CUT_PART:
                                PhysicalCutFile cutFile = ((PhysicalCutPartFile) fileToDelete).getIncludingCutFile();
                                stafDeleteFileURLs.add(STAFURLFactory.getCutFileSTAFUrl(cutFile));
                                break;
                            case TAR:
                            case CUT:
                            case NORMAL:
                            default:
                                stafDeleteFileURLs.addAll(STAFURLFactory.getSTAFURLs(fileToDelete));
                                break;
                        }
                    } catch (STAFURLException e) {
                        LOG.error(e.getMessage(), e);
                    }
                });
        return stafDeleteFileURLs;
    }

    /**
     * Retrieve a given {@link AbstractPhysicalFile} from STAF System.<br/>
     * Requirement : A connection to the stafService must be open.
     * @param pTarToRetrieve {@link AbstractPhysicalFile} to retrieve.
     * @return {@link Path} of the local retrieved file.
     * @throws STAFException Error retrieving file.
     */
    private Path retrieveFileFromSTAF(AbstractPhysicalFile pFileToRetrieve) throws STAFException {
        stafService.restoreFile(pFileToRetrieve.getSTAFFilePath(), getWorkspaceTmpDirectory());
        // Check file is restored
        Path localTARFilePath = Paths.get(getWorkspaceTmpDirectory().toString(),
                                          pFileToRetrieve.getStafFileName().toString());
        if (localTARFilePath.toFile().exists()) {
            LOG.debug("[STAF] TAR File {} retrieved to {}.", pFileToRetrieve.getStafFileName(), localTARFilePath);
            pFileToRetrieve.setLocalFilePath(localTARFilePath);
        } else {
            throw new STAFException(
                    String.format("[STAF] TAR File is not retrieved from STAF %s", pFileToRetrieve.getSTAFFilePath()));
        }
        return localTARFilePath;
    }

    /**
     * Return a mapping between raw files and STAF URLs.
     * @param pFiles {@link AbstractPhysicalFile} files to calculate the mapping.
     * @return {@link Map}<{@link Path}, {@link URL}> (key : Path of the raw file to archive, value : URL of STAF file)
     */
    public Map<Path, URL> getRawFilesArchived(Set<AbstractPhysicalFile> pFiles) {
        Map<Path, URL> rawFilesArchived = Maps.newHashMap();
        // @formatter:off
        pFiles
            .stream()
            .filter(f ->
                PhysicalFileStatusEnum.STORED.equals(f.getStatus()) ||
                PhysicalFileStatusEnum.LOCALY_STORED.equals(f.getStatus()))
            .forEach(file -> {
                // Create urls for the given stored file.
                // NORMAL : 1 File stored -> 1 URL
                // TAR : 1 File stored -> X URL (one per file in TAR)
                // CUT : X Files stored -> 1 URL
                try {
                    Map<Path, URL> urls = STAFURLFactory.getSTAFURLsPerRAWFileToArchive(file);
                    rawFilesArchived.putAll(urls);
                } catch (STAFException e) {
                    // Error creating file URL
                    LOG.error("Error during STAF URL creation for staf file {}",file.getLocalFilePath(),e);
                }
            });
        // @formatter:on
        return rawFilesArchived;
    }

    /**
     * Retreive the temporary directory from the workspace for the current STAF Archive.
     * @return {@link Path} of the STAF TMP workspace directory for the current STAF Archive.
     */
    public Path getWorkspaceTmpDirectory() {
        return Paths.get(localWorkspace.toString(), stafService.getStafArchive().getArchiveName(), TMP_DIRECTORY);
    }

    /**
     * The archive mode to store file in STAF is calculated with the file size.
     * The modes are {@link STAFArchiveModeEnum}
     * @param pFileSize int
     * @return {@link STAFArchiveModeEnum}
     */
    private STAFArchiveModeEnum getFileArchiveMode(Long pFileSize) {
        if (pFileSize < stafConfiguration.getMinFileSize()) {
            return STAFArchiveModeEnum.TAR;
        }
        if (pFileSize > stafConfiguration.getMaxFileSize()) {
            return STAFArchiveModeEnum.CUT;
        }
        return STAFArchiveModeEnum.NORMAL;
    }

    /**
     * Initialize needed directories fot the current controller.
     * @throws IOException Directories are not accessible.
     */
    public void initializeWorkspaceDirectories() throws IOException {
        if (!Files.exists(localWorkspace)) {
            Files.createDirectories(localWorkspace);
        }
        if (!Files.isReadable(localWorkspace)) {
            throw new IOException(
                    String.format("[STAF] Local workspace %s is not readable", localWorkspace.toString()));
        }
        if (!Files.isWritable(localWorkspace)) {
            throw new IOException(
                    String.format("[STAF] Local workspace %s is not writable", localWorkspace.toString()));
        }
        // Initialize TMP Directory
        Path tmpWorkspaceDir = getWorkspaceTmpDirectory();
        if (!Files.exists(tmpWorkspaceDir)) {
            Files.createDirectories(tmpWorkspaceDir);
        }
        if (!Files.isReadable(tmpWorkspaceDir)) {
            throw new IOException(
                    String.format("[STAF] workspace TMP directory %s is not readable", tmpWorkspaceDir.toString()));
        }
        if (!Files.isWritable(tmpWorkspaceDir)) {
            throw new IOException(
                    String.format("[STAF] workspace TMP directory %s is not writable", tmpWorkspaceDir.toString()));
        }
    }

    /**
     * Create the list of {@link AbstractPhysicalFile} associated to the given list of {@link Path} file to archive per STAF node.
     * @param pFileToArchivePerStafNode {@link Map}<{@link Path},{@link Set}<{@link Path}>> <br/>
     * <ul>
     * <li>key : STAF Node where to store the {@link Path}s in map value</li>
     * <li>value : {@link Path} Files to store for the given STAF Node.</li>
     * </ul>
     * @param pMode {@link STAFArchiveModeEnum} Archiving mode.
     * @return {@link Set}<{@link AbstractPhysicalFile}> prepared files to archive with STAF recomandations.
     */
    public Set<AbstractPhysicalFile> prepareFilesToArchive(Map<Path, Set<Path>> pFileToArchivePerStafNode) {
        Set<AbstractPhysicalFile> preparedFiles = Sets.newHashSet();
        for (Entry<Path, Set<Path>> stafNode : pFileToArchivePerStafNode.entrySet()) {
            for (Path fileToArchive : stafNode.getValue()) {
                try {
                    preparedFiles.addAll(prepareFileToArchive(fileToArchive, stafNode.getKey(), preparedFiles));
                } catch (STAFException e) {
                    LOG.error("[STAF] Error preparing file for STAF transfer. " + e.getMessage());
                    LOG.debug(e.getMessage(), e);
                }
            }
        }
        // After all files added, check if TAR files are in TO_STORE state in order to create associated TAR.
        try {
            tarController.createPreparedTAR(getPhysicalTARFilesFromAbstractPhysicalFiles(preparedFiles));
        } catch (STAFTarException e) {
            LOG.error("[STAF] Error creating TAR File", e);
        }
        return preparedFiles;
    }

    public Set<AbstractPhysicalFile> prepareFilesToDelete(Set<URL> pSTAFFilesToDelete) {
        Set<AbstractPhysicalFile> physicalFiles = Sets.newHashSet();
        //1. Create STAF File from given urls
        for (URL stafURL : pSTAFFilesToDelete) {
            try {
                AbstractPhysicalFile physicalFile = getSTAFPhysicalFile(stafURL, physicalFiles,
                                                                        PhysicalFileStatusEnum.TO_DELETE);

                switch (physicalFile.getArchiveMode()) {
                    case CUT:
                        PhysicalCutFile cutFile = (PhysicalCutFile) physicalFile;
                        physicalFiles.addAll(cutFile.getCutedFileParts());
                        break;
                    case CUT_PART:
                    case NORMAL:
                    case TAR:
                    default:
                        physicalFiles.add(physicalFile);
                        break;
                }
            } catch (STAFException e) {
                LOG.error("[STAF] Error retreiving file {}", stafURL.toString(), e);
            }
        }
        return physicalFiles;
    }

    public Set<AbstractPhysicalFile> prepareFilesToRestore(Set<URL> pSTAFFilesToRestore) {
        Set<AbstractPhysicalFile> physicalFiles = Sets.newHashSet();
        //1. Create STAF File from given urls
        for (URL stafURL : pSTAFFilesToRestore) {
            try {
                AbstractPhysicalFile physicalFile = getSTAFPhysicalFile(stafURL, physicalFiles,
                                                                        PhysicalFileStatusEnum.TO_RETRIEVE);
                if (stafService.getStafArchive().getArchiveName().equals(physicalFile.getStafArchiveName())) {
                    switch (physicalFile.getArchiveMode()) {
                        case CUT:
                            PhysicalCutFile cutFile = (PhysicalCutFile) physicalFile;
                            physicalFiles.addAll(cutFile.getCutedFileParts());
                            break;
                        case CUT_PART:
                        case NORMAL:
                        case TAR:
                        default:
                            physicalFiles.add(physicalFile);
                            break;
                    }
                } else {
                    LOG.error("[STAF] Unable to retrieve a file from archive {}. The current configured archive is {}",
                              physicalFile.getStafArchiveName(), stafService.getStafArchive().getArchiveName());
                }
            } catch (STAFException e) {
                LOG.error("[STAF] Error retreiving file {}", stafURL.toString(), e);
            }
        }
        return physicalFiles;
    }

    /**
     * Restore the files from the given STAF {@link URL}
     * @param pSTAFFilesToRestore {@link Set}<{@link URL}> STAF URL of files to retrieve.
     * @param pDestinationPath {@link Path} Directory where to put restored files.
     */
    public void restoreFiles(Set<AbstractPhysicalFile> pPhysicalFiles, Path pDestinationPath,
            IClientCollectListener pListener) {

        // 1. Set the collector listener for notification when a file is restored from STAF system.
        STAFCollectListener listener = new STAFCollectListener(pPhysicalFiles, pDestinationPath, pListener);
        stafService.setCollectListener(listener);

        // 2. generate staf file paths
        Set<Path> stafFilePathsToRetrieive = Sets.newHashSet();
        for (AbstractPhysicalFile physicalFile : pPhysicalFiles) {
            try {
                boolean isLocallyStored = false;
                // Try to retrieve from local temporary storage (TAR file case)
                if (STAFArchiveModeEnum.TAR.equals(physicalFile.getArchiveMode())) {
                    isLocallyStored = tarController.retrieveLocallyStoredTARFiles((PhysicalTARFile) physicalFile,
                                                                                  listener);
                }
                // If no local retrieve, then generate remote staf paths to retrieve.
                if (!isLocallyStored) {
                    stafFilePathsToRetrieive.addAll(getSTAFFilePaths(physicalFile));
                }
            } catch (STAFException e) {
                physicalFile.setStatus(PhysicalFileStatusEnum.ERROR);
                LOG.error("[STAF] Error creating STAF file path for file {}", physicalFile.getLocalFilePath(), e);
            }
        }

        // 3. Do the STAF Restitution for each physicalFile
        if (!stafFilePathsToRetrieive.isEmpty()) {
            try {
                stafService.connectArchiveSystem(ArchiveAccessModeEnum.RESTITUTION_MODE);
                stafService.restoreAllFiles(stafFilePathsToRetrieive, pDestinationPath.toString());
            } catch (STAFException e) {
                LOG.error("[STAF] Error during STAF Restoration", e.getMessage(), e);
            } finally {
                try {
                    stafService.disconnectArchiveSystem(ArchiveAccessModeEnum.RESTITUTION_MODE);
                } catch (STAFException e) {
                    LOG.error("Error during STAF deconnection.", e);
                }
            }
        } else {
            // Send fail error for all files needed to be restored.
        }
    }

    /**
     * Cut a file which is too big to be archive in one part into STAF System.
     * @param pPhysicalFileToArchive {@link Path} file to cut in parts
     * @param pStafNode {@link String} STAF Node where to archive the given File
     * @return {@link PhysicalCutFile} containing {@link PhysicalCutPartFile}s for each part of the cuted file
     * @throws IOException Error during file cut.
     */
    private PhysicalCutFile cutFile(Path pPhysicalFileToArchive, Path pSTAFNode, String pFileMd5) throws IOException {
        // 1. Create cut temporary directory into workspace
        Path tmpCutDirectory = Paths.get(getWorkspaceTmpDirectory().toString(), pFileMd5);
        if (!tmpCutDirectory.toFile().exists()) {
            tmpCutDirectory.toFile().mkdirs();
        }
        // 2. Do cut files
        Set<File> cutedLocalFiles = CutFileUtils.cutFile(pPhysicalFileToArchive.toFile(), tmpCutDirectory.toString(),
                                                         pFileMd5, stafConfiguration.getMaxFileSize());
        LOG.info("[STAF] Number of cuted files : {} for file {}", cutedLocalFiles.size(),
                 pPhysicalFileToArchive.toString());

        // 3. Create cut Physical file object to return
        PhysicalCutFile physicalCutFile = new PhysicalCutFile(pPhysicalFileToArchive,
                stafService.getStafArchive().getArchiveName(), pSTAFNode, pFileMd5);
        physicalCutFile.addRawAssociatedFile(pPhysicalFileToArchive);
        int partIndex = 0;
        for (File cutedFile : cutedLocalFiles) {
            Path cutedFilePath = Paths.get(cutedFile.getPath());
            PhysicalCutPartFile cutFilePart = new PhysicalCutPartFile(cutedFilePath, physicalCutFile, partIndex,
                    stafService.getStafArchive().getArchiveName(), pSTAFNode, cutedFilePath.getFileName().toString());
            physicalCutFile.addCutedPartFile(cutFilePart);
            partIndex++;
        }
        return physicalCutFile;
    }

    /**
     * Delete temporary files associated to the given {@link AbstractPhysicalFile}
     * @param pFile {@link AbstractPhysicalFile}
     */
    private void deleteTemporaryFiles(AbstractPhysicalFile pFile) {
        switch (pFile.getArchiveMode()) {
            case CUT_PART:
                deleteTemporaryFile((PhysicalCutPartFile) pFile);
                break;
            case NORMAL:
                deleteTemporaryFile((PhysicalNormalFile) pFile);
                break;
            case TAR:
                deleteTemporaryFile((PhysicalTARFile) pFile);
                break;
            case CUT:
            default:
                deleteTemporaryFile((PhysicalCutFile) pFile);
                break;
        }
    }

    /**
     * Delete temporary files for the given {@link PhysicalCutPartFile}
     * @param pFile {@link PhysicalCutPartFile}
     */
    private void deleteTemporaryFile(PhysicalCutPartFile pCutPartFile) {
        if (pCutPartFile.getLocalFilePath().getParent().toFile().exists()) {
            try {
                Files.walk(pCutPartFile.getLocalFilePath().getParent()).map(Path::toFile)
                        .sorted((o1, o2) -> -o1.compareTo(o2)).forEach(File::delete);
            } catch (IOException e) {
                LOG.error("[STAF] Error deleting file (CUT MODE", e);
            }
        }
    }

    /**
     * Delete temporary file for the given {@link PhysicalNormalFile}
     * @param pFile {@link PhysicalNormalFile}
     */
    private void deleteTemporaryFile(PhysicalNormalFile pFile) {
        // Only delete local file if the file is in the STAF workspace.
        if (pFile.getLocalFilePath().toFile().exists() && pFile.getLocalFilePath().startsWith(localWorkspace)) {
            try {
                Files.delete(pFile.getLocalFilePath());
            } catch (IOException e) {
                LOG.error("[STAF] Error deleting file (NORMAL MODE", e);
            }
        }
    }

    /**
     * Delete temporary file for the given {@link PhysicalTARFile}
     * @param pFile {@link PhysicalTARFile}
     */
    private void deleteTemporaryFile(PhysicalTARFile pFile) {
        // Only delete local file if the file is in the STAF workspace.
        if (pFile.getLocalFilePath().toFile().exists() && pFile.getLocalFilePath().startsWith(localWorkspace)) {
            try {
                Files.delete(pFile.getLocalFilePath());
            } catch (IOException e) {
                LOG.error("[STAF] Error deleting file (TAR MODE", e);
            }
        }
    }

    /**
     * Delete temporary file for the given {@link PhysicalCutFile}
     * @param pFile {@link PhysicalCutFile}
     */
    private void deleteTemporaryFile(PhysicalCutFile pFile) {
        // Only delete local file if the file is in the STAF workspace.
        if (pFile.getLocalFilePath().toFile().exists() && pFile.getLocalFilePath().startsWith(localWorkspace)) {
            try {
                Files.delete(pFile.getLocalFilePath());
            } catch (IOException e) {
                LOG.error("[STAF] Error deleting file (NORMAL MODE", e);
            }
        }
    }

    /**
     * Return all file STAF {@link Path} associted to the given {@link AbstractPhysicalFile}
     * @param pPhysicalFile {@link AbstractPhysicalFile} Prepared file (CUT|NORMAL|TAR) to retreive STAF files paths.
     * @return {@link Set}<{@link Path}> for each physical file in STAF for the given {@link AbstractPhysicalFile}
     * @throws STAFException
     */
    private Set<Path> getSTAFFilePaths(AbstractPhysicalFile pPhysicalFile) throws STAFException {
        Set<Path> stafFilePaths = Sets.newHashSet();
        switch (pPhysicalFile.getArchiveMode()) {
            case CUT:
                // Add path to all parts of the cuted file in STAF.
                PhysicalCutFile cutFile = (PhysicalCutFile) pPhysicalFile;
                for (PhysicalCutPartFile cutedFilePart : cutFile.getCutedFileParts()) {
                    stafFilePaths.add(cutedFilePart.getSTAFFilePath());
                }
                break;
            case CUT_PART:
            case NORMAL:
            case TAR:
                // CUT PART : 1 file in STAF = 1 file to retrieve
                // NORMAL : 1 file in STAF = 1 file to retrieve
                // TAR : 1 file in STAF = 1 file to retrieve (containing many files stored).
                stafFilePaths.add(pPhysicalFile.getSTAFFilePath());
                break;
            default:
                break;
        }
        return stafFilePaths;
    }

    private Set<PhysicalTARFile> getPhysicalTARFilesFromAbstractPhysicalFiles(Set<AbstractPhysicalFile> pFiles) {
        return pFiles.stream().filter(physicalFile -> STAFArchiveModeEnum.TAR.equals(physicalFile.getArchiveMode()))
                .map(physicalFile -> (PhysicalTARFile) physicalFile).collect(Collectors.toSet());
    }

    /**
     * Create an object {@link AbstractPhysicalFile} from a given STAF {@link URL}
     * @param pUrl STAF {@link URL}
     * @return created {@link AbstractPhysicalFile}
     * @throws STAFException
     */
    private AbstractPhysicalFile getSTAFPhysicalFile(URL pUrl, Set<AbstractPhysicalFile> pAlreadyPreparedPhysicalFiles,
            PhysicalFileStatusEnum pStatus) throws STAFException {

        String stafArchive = STAFURLFactory.getSTAFArchiveFromURL(pUrl);
        Path stafNode = STAFURLFactory.getSTAFNodeFromURL(pUrl);
        String stafFileName = STAFURLFactory.getSTAFFileNameFromURL(pUrl);
        STAFArchiveModeEnum mode = STAFURLFactory.getSTAFArchiveModeFromURL(pUrl);
        Map<STAFURLParameter, String> parameters = STAFURLFactory.getSTAFURLParameters(pUrl);

        switch (mode) {
            case CUT:
                PhysicalCutFile cutFile = new PhysicalCutFile(null, stafArchive, stafNode, stafFileName);
                if (parameters.get(STAFURLParameter.CUT_PARTS_PARAMETER) != null) {
                    Integer numberOfParts = Integer.parseInt(parameters.get(STAFURLParameter.CUT_PARTS_PARAMETER));
                    for (int i = 0; i < numberOfParts; i++) {
                        String partFileName = PhysicalCutPartFile.calculatePartFileName(stafFileName, i);
                        PhysicalCutPartFile partFile = new PhysicalCutPartFile(null, cutFile, i, stafArchive, stafNode,
                                partFileName);
                        partFile.setStatus(pStatus);
                        cutFile.addCutedPartFile(partFile);
                    }
                }

                return cutFile;
            case NORMAL:
                PhysicalNormalFile physicalFile = new PhysicalNormalFile(null, null, stafArchive, stafNode,
                        stafFileName);
                physicalFile.setStatus(pStatus);
                return physicalFile;
            case TAR:
                // Check if tar is already prepared
                // @formatter:off
                Optional<AbstractPhysicalFile> existingTar = pAlreadyPreparedPhysicalFiles
                    .stream()
                    .filter(f -> STAFArchiveModeEnum.TAR.equals(f.getArchiveMode()))
                    .filter(f -> {
                            PhysicalTARFile t = (PhysicalTARFile) f;
                            return stafFileName.equals(t.getStafFileName()) && stafNode.equals(t.getStafNode())
                                    && stafArchive.equals(t.getStafArchiveName());
                        })
                    .findFirst();
                // @formatter:on
                PhysicalTARFile tar;
                if (existingTar.isPresent()) {
                    tar = (PhysicalTARFile) existingTar.get();
                } else {
                    tar = new PhysicalTARFile(stafArchive, stafNode, stafFileName);
                    tarController.getLocalInformations(tar);
                }
                tar.setStatus(pStatus);
                Path fileName = Paths.get(parameters.get(STAFURLParameter.TAR_FILENAME_PARAMETER));
                if (fileName != null) {
                    tar.addFileInTar(fileName, fileName);
                }
                return tar;
            default:
                throw new STAFException(String.format("STAF Archive mode invalid for restituion %s", mode));
        }
    }

    /**
     * Create all {@link AbstractPhysicalFile} associated to the given local file to archive.
     * @param pFileToArchivePerStafNode {@lnik Path} File to prepare.
     * @param pSTAFNode {@link String} STAF Node where to store file.
     * @param pAlreadyPreparedFiles {@link Set}<{@link AbstractPhysicalFile}> All files already prepared to archive.
     * @throws STAFException Error during file preparation. File is not available for store.
     */
    private Set<AbstractPhysicalFile> prepareFileToArchive(Path pFileToArchive, Path pSTAFNode,
            Set<AbstractPhysicalFile> pAlreadyPreparedFiles) throws STAFException {
        try {
            // 1. Check file existance
            if (!Files.exists(pFileToArchive) || !Files.isReadable(pFileToArchive)) {
                String message = String.format("[STAF] File %s to archive, is not accessible",
                                               pFileToArchive.toString());
                LOG.error(message);
                throw new STAFException(message);
            }
            // 2. Get file archiving mode
            STAFArchiveModeEnum mode = getFileArchiveMode(pFileToArchive.toFile().length());

            // 3. Calculate MD5 signature of file. This signature is used into STAF System for file names to ensure unicity.
            String stafFileName = calculateSTAFFileName(pFileToArchive);

            // 3. Manage file transformation if needed before staf storage
            switch (mode) {
                case CUT:
                    pAlreadyPreparedFiles.addAll(cutFile(pFileToArchive, pSTAFNode, stafFileName).getCutedFileParts());
                    break;
                case TAR:
                    pAlreadyPreparedFiles
                            .add(tarController.addFileToTar(pFileToArchive,
                                                            getPhysicalTARFilesFromAbstractPhysicalFiles(pAlreadyPreparedFiles),
                                                            stafService.getStafArchive().getArchiveName(), pSTAFNode));
                    break;
                case NORMAL:
                    pAlreadyPreparedFiles.add(new PhysicalNormalFile(pFileToArchive, pFileToArchive,
                            stafService.getStafArchive().getArchiveName(), pSTAFNode, stafFileName));
                    break;
                default:
                    throw new STAFException(String.format("Unhandle Archive mode %s", mode.toString()));
            }
        } catch (IOException | STAFTarException e) {
            LOG.error("[STAF] Error preparing file {}", pFileToArchive, e);
        }
        return pAlreadyPreparedFiles;
    }

    /**
     * Calculate name of files stored into STAF System for the given {@link Path} of the local file to archive.
     * @param pLocalFilePathToArchive {@link Path} of the local file to archive.
     * @return {@link String} STAF file name.
     * @throws STAFException
     */
    private String calculateSTAFFileName(Path pLocalFilePathToArchive) throws STAFException {
        try (FileInputStream is = new FileInputStream(pLocalFilePathToArchive.toFile())) {
            return ChecksumUtils.computeHexChecksum(is, "md5");
        } catch (IOException | NoSuchAlgorithmException e) {
            throw new STAFException(String.format("Error calculating STAF File name for file %s to archive",
                                                  pLocalFilePathToArchive.toString()),
                    e);
        }
    }
}
