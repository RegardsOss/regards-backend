/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.staf;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

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
import fr.cnes.regards.framework.staf.protocol.STAFUrlFactory;
import fr.cnes.regards.framework.staf.protocol.STAFUrlParameter;

/**
 * STAF Controller to handle STAF recommandations before storing files.<br/>
 * Recommandations are :
 * <ul>
 * <li>Do not store short files as single files into STAF Archive. Store short files into a single archive file (TAR File)</li>
 * <li>Do not store files that exceed Xoctets. Cut too big files into multiple files in STAF</li>
 * </ul>
 * <br/>
 * To do so, use the prepareFilesToArchive method.<br/>
 * Then you can use the doArchivePreparedFiles to store the prepared files.<br/>
 * After the store process is over, you can retrieve the associations between <br/>
 * raw files and actually stored files with the getRawFilesArchived method.
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
    public Set<AbstractPhysicalFile> archiveFiles(Set<AbstractPhysicalFile> pFilesToArchive, Path pSTAFNode,
            boolean pReplaceMode) throws STAFException {
        Set<String> archivedFiles;
        // Create map bewteen localFile to archive and destination file path into STAF
        Map<Path, Path> localFileToArchiveMap = Maps.newHashMap();
        pFilesToArchive.forEach(stafFile -> {
            if (PhysicalFileStatusEnum.TO_STORE.equals(stafFile.getStatus())) {
                try {
                    if ((stafFile.getLocalFilePath() != null) && (stafFile.calculateSTAFFilePath() != null)) {
                        localFileToArchiveMap.put(stafFile.getLocalFilePath(), stafFile.calculateSTAFFilePath());
                    } else {
                        stafFile.setStatus(PhysicalFileStatusEnum.ERROR);
                        LOG.warn("Undefined file to archive for origine(local)={} and destination(STAF)={}",
                                 stafFile.getLocalFilePath(), stafFile.calculateSTAFFilePath());
                    }
                } catch (STAFException e) {
                    LOG.error(e.getMessage(), e);
                }
            }
        });

        // If there is files to store. Run STAF archive.
        if (!localFileToArchiveMap.isEmpty()) {
            stafService.connectArchiveSystem(ArchiveAccessModeEnum.ARCHIVE_MODE);
            try {
                archivedFiles = stafService.archiveFiles(localFileToArchiveMap, pSTAFNode, pReplaceMode);
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
     */
    public void deleteFiles(Set<AbstractPhysicalFile> pPhysicalFilesToDelete) {

        // List of staf file path (key : local file to replace with, value : staf file path to replace)
        // to replace per staf node.
        Map<Path, Map<Path, Path>> filesToReplace = Maps.newHashMap();

        // List of STAF file path to delete
        Set<Path> stafFilePathsToDelete = Sets.newHashSet();

        for (AbstractPhysicalFile physicalFileToDelete : pPhysicalFilesToDelete) {
            try {
                switch (physicalFileToDelete.getArchiveMode()) {
                    case CUT:
                        // Add each part to delete
                        PhysicalCutFile cutFile = (PhysicalCutFile) physicalFileToDelete;
                        for (PhysicalCutPartFile part : cutFile.getCutedFileParts()) {
                            stafFilePathsToDelete.add(part.getSTAFFilePath());
                        }
                        break;
                    case CUT_PART:
                    case NORMAL:
                        // Add file to delete
                        stafFilePathsToDelete.add(physicalFileToDelete.getSTAFFilePath());
                        break;
                    case TAR:
                        // TODO : Case of file deletion into TAR :
                        // 1. Retrieve TAR from STAF
                        // 2. extract TAR
                        // 3. Reconstruct TAR without the delete file
                        // 4. Archive file
                        break;
                    default:
                        break;
                }
            } catch (STAFException e) {
                LOG.error("[STAF] Unable to delete file from STAF", physicalFileToDelete.getStafFileName());
                // TODO : Notify listener
            }
        }

        try {
            stafService.connectArchiveSystem(ArchiveAccessModeEnum.ARCHIVE_MODE);

            // run files deletion
            stafService.deleteFiles(stafFilePathsToDelete);

            // run file replace
            for (Entry<Path, Map<Path, Path>> entry : filesToReplace.entrySet()) {
                stafService.archiveFiles(entry.getValue(), entry.getKey(), true);
            }
        } catch (STAFException e) {
            LOG.error("[STAF] Error during STAF Deletion", e.getMessage(), e);
        } finally {
            try {
                stafService.disconnectArchiveSystem(ArchiveAccessModeEnum.ARCHIVE_MODE);
            } catch (STAFException e) {
                LOG.error("Error during STAF deconnection.", e);
            }
        }
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
                    Map<Path, URL> urls = STAFUrlFactory.getSTAFURLsPerRAWFileToArchive(file);
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
    public STAFArchiveModeEnum getFileArchiveMode(Long pFileSize) {
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
     * Create the list of {@link AbstractPhysicalFile} associated to the given list of {@link Path} file to archive.
     * @param pFileToArchivePerStafNode {@link Map}<{@link String},{@link Set}<{@link Path}>> <br/>
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

    public Set<AbstractPhysicalFile> prepareFilesToRestore(Set<URL> pSTAFFilesToRestore) {
        Set<AbstractPhysicalFile> physicalFiles = Sets.newHashSet();
        //1. Create STAF File from given urls
        for (URL stafURL : pSTAFFilesToRestore) {
            try {
                AbstractPhysicalFile physicalFile = getSTAFPhysicalFile(stafURL, physicalFiles,
                                                                        PhysicalFileStatusEnum.TO_RETRIEVE);

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

    /**
     * Restore the files from the given STAF {@link URL}
     * @param pSTAFFilesToRestore {@link Set}<{@link URL}> STAF URL of files to retrieve.
     * @param pDestinationPath {@link Path} Directory where to put restored files.
     */
    public void restoreFiles(Set<AbstractPhysicalFile> pPhysicalFiles, Path pDestinationPath,
            IClientCollectListener pListener) {

        stafService.setCollectListener(new STAFCollectListener(pPhysicalFiles, pDestinationPath, pListener));

        //2. Retrieve staf files path to retrieve
        Set<Path> stafFilePathsToRetrieive = Sets.newHashSet();
        for (AbstractPhysicalFile physicalFile : pPhysicalFiles) {
            try {
                stafFilePathsToRetrieive.addAll(getSTAFFilePaths(physicalFile));
            } catch (STAFException e) {
                physicalFile.setStatus(PhysicalFileStatusEnum.ERROR);
                LOG.error("[STAF] Error creating STAF file path for file {}", physicalFile.getLocalFilePath(), e);
            }
        }
        //2. Do the STAF Restitution for each physicalFile
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
    }

    /**
     * Cut a file which is too big to be archive in one part into STAF System.
     * @param pPhysicalFileToArchive {@link Path} file to cut in parts
     * @param pStafNode {@link String} STAF Node where to archive the given File
     * @return {@link PhysicalCutFile} containing {@link PhysicalCutPartFile}s for each part of the cuted file
     * @throws IOException Error during file cut.
     */
    private PhysicalCutFile cutFile(Path pPhysicalFileToArchive, Path pSTAFNode) throws IOException {
        // 1. Create cut temporary directory into workspace
        Path tmpCutDirectory = Paths.get(getWorkspaceTmpDirectory().toString(),
                                         pPhysicalFileToArchive.getFileName().toString());
        if (!tmpCutDirectory.toFile().exists()) {
            tmpCutDirectory.toFile().mkdirs();
        }
        // 2. Do cut files
        Set<File> cutedLocalFiles = CutFileUtils.cutFile(pPhysicalFileToArchive.toFile(), tmpCutDirectory.toString(),
                                                         stafConfiguration.getMaxFileSize());
        LOG.info("[STAF] Number of cuted files : {} for file {}", cutedLocalFiles.size(),
                 pPhysicalFileToArchive.toString());

        // 3. Create cut Physical file object to return
        PhysicalCutFile physicalCutFile = new PhysicalCutFile(pPhysicalFileToArchive,
                stafService.getStafArchive().getArchiveName(), pSTAFNode);
        physicalCutFile.addRawAssociatedFile(pPhysicalFileToArchive);
        int partIndex = 0;
        for (File cutedFile : cutedLocalFiles) {
            Path cutedFilePath = Paths.get(cutedFile.getPath());
            PhysicalCutPartFile cutFilePart = new PhysicalCutPartFile(cutedFilePath, physicalCutFile, partIndex,
                    stafService.getStafArchive().getArchiveName(), pSTAFNode);
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
                // Add path to the file.
                // CUT PART : 1 file in STAF = 1 file to retrieve
                // NORMAL : 1 file in STAF = 1 file to retrieve
                // TAR : 1 file in STAF = 1 file to retrieve (containing many files stored).
                stafFilePaths.add(pPhysicalFile.getSTAFFilePath());
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

        String stafArchive = STAFUrlFactory.getSTAFArchiveFromURL(pUrl);
        Path stafNode = STAFUrlFactory.getSTAFNodeFromURL(pUrl);
        String stafFileName = STAFUrlFactory.getSTAFFileNameFromURL(pUrl);
        STAFArchiveModeEnum mode = STAFUrlFactory.getSTAFArchiveModeFromURL(pUrl);
        Map<STAFUrlParameter, String> parameters = STAFUrlFactory.getSTAFURLParameters(pUrl);

        switch (mode) {
            case CUT:
                PhysicalCutFile cutFile = new PhysicalCutFile(null, stafArchive, stafNode);
                cutFile.setStafFileName(stafFileName);
                if (parameters.get(STAFUrlParameter.CUT_PARTS_PARAMETER) != null) {
                    Integer numberOfParts = Integer.parseInt(parameters.get(STAFUrlParameter.CUT_PARTS_PARAMETER));
                    for (int i = 0; i < numberOfParts; i++) {
                        PhysicalCutPartFile partFile = new PhysicalCutPartFile(null, cutFile, i, stafArchive, stafNode);
                        partFile.setStatus(pStatus);
                        cutFile.addCutedPartFile(partFile);
                    }
                }

                return cutFile;
            case NORMAL:
                PhysicalNormalFile physicalFile = new PhysicalNormalFile(null, null, stafArchive, stafNode);
                physicalFile.setStatus(pStatus);
                physicalFile.setStafFileName(stafFileName);
                return physicalFile;
            case TAR:
                // Check if tar is already prepared
                Optional<AbstractPhysicalFile> existingTar = pAlreadyPreparedPhysicalFiles.stream()
                        .filter(f -> STAFArchiveModeEnum.TAR.equals(f.getArchiveMode())).filter(f -> {
                            PhysicalTARFile t = (PhysicalTARFile) f;
                            return stafFileName.equals(t.getStafFileName()) && stafNode.equals(t.getStafNode())
                                    && stafArchive.equals(t.getStafArchiveName());
                        }).findFirst();
                PhysicalTARFile tar;
                if (existingTar.isPresent()) {
                    tar = (PhysicalTARFile) existingTar.get();
                } else {
                    tar = new PhysicalTARFile(stafArchive, stafNode);
                    tar.setStafFileName(stafFileName);
                }
                tar.setStatus(pStatus);
                Path fileName = Paths.get(parameters.get(STAFUrlParameter.TAR_FILENAME_PARAMETER));
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
            // 3. Manage file transformation if needed before staf storage
            switch (mode) {
                case CUT:
                    pAlreadyPreparedFiles.addAll(cutFile(pFileToArchive, pSTAFNode).getCutedFileParts());
                    break;
                case TAR:
                    pAlreadyPreparedFiles
                            .add(tarController.addFileToTar(pFileToArchive,
                                                            getPhysicalTARFilesFromAbstractPhysicalFiles(pAlreadyPreparedFiles),
                                                            stafService.getStafArchive().getArchiveName(), pSTAFNode));
                    break;
                case NORMAL:
                    pAlreadyPreparedFiles.add(new PhysicalNormalFile(pFileToArchive, pFileToArchive,
                            stafService.getStafArchive().getArchiveName(), pSTAFNode));
                    break;
                default:
                    throw new STAFException(String.format("Unhandle Archive mode %s", mode.toString()));
            }
        } catch (IOException | STAFTarException e) {
            LOG.error("[STAF] Error preparing file {}", pFileToArchive, e);
        }
        return pAlreadyPreparedFiles;
    }
}
