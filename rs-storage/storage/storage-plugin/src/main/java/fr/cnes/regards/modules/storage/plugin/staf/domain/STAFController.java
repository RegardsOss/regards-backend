package fr.cnes.regards.modules.storage.plugin.staf.domain;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import fr.cnes.regards.framework.file.utils.CutFileUtils;
import fr.cnes.regards.framework.staf.ArchiveAccessModeEnum;
import fr.cnes.regards.framework.staf.STAFArchiveModeEnum;
import fr.cnes.regards.framework.staf.STAFConfiguration;
import fr.cnes.regards.framework.staf.STAFException;
import fr.cnes.regards.framework.staf.STAFService;

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
     * List of prepared files to store into STAF System.
     */
    private final Set<AbstractPhysicalFile> filesToArchive = Sets.newHashSet();

    /**
     * List of prepared tar to store into STAF System.
     */
    private final Set<PhysicalTARFile> tarsToArchive = Sets.newHashSet();

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
        // Check if workspace exists and is writable/readable
        if (!Files.exists(localWorkspace)) {
            Files.createDirectories(localWorkspace);
            if (!Files.isReadable(localWorkspace)) {
                throw new IOException(
                        String.format("[STAF] Local workspace %s is not readable", localWorkspace.toString()));
            }
            if (!Files.isWritable(localWorkspace)) {
                throw new IOException(
                        String.format("[STAF] Local workspace %s is not writable", localWorkspace.toString()));
            }
        }
        tarController = new TARController(pStafConfiguration, pLocalWorkspace);
    }

    /**
     * Prepare the list of file to archive into the STAF for the given files
     * @param pFileToArchivePerStafNode
     * @param pMode
     * @return
     */
    public Set<AbstractPhysicalFile> prepareFilesToArchive(Map<String, Set<Path>> pFileToArchivePerStafNode,
            STAFArchiveModeEnum pMode) {

        this.clear();
        for (String stafNode : pFileToArchivePerStafNode.keySet()) {
            for (Path fileToArchive : pFileToArchivePerStafNode.get(stafNode)) {
                try {
                    this.prepareFileToArchive(fileToArchive, stafNode, pMode);
                } catch (STAFException e) {
                    LOG.error("[STAF] Error preparing file for STAF transfer. " + e.getMessage());
                    LOG.debug(e.getMessage(), e);
                }
            }
        }
        return this.getAllFilesToArchive();
    }

    /**
     * Prepare the given file to archive into the staf.
     * @param pFileToArchivePerStafNode
     * @param pSTAFNode
     * @param pMode
     * @throws STAFException
     */
    private void prepareFileToArchive(Path pFileToArchive, String pSTAFNode, STAFArchiveModeEnum pMode)
            throws STAFException {
        try {

            // 1. Check file existance
            if (!Files.exists(pFileToArchive) || !Files.isReadable(pFileToArchive)) {
                String message = String.format("[STAF] File %s to archive, is not accessible",
                                               pFileToArchive.toString());
                LOG.error(message);
                throw new STAFException(message);
            }

            // 2. Manage file transformation if needed before staf storage
            switch (pMode) {
                case CUT:
                    // 1. Cut file in part
                    filesToArchive.addAll(cutFile(pFileToArchive, pSTAFNode).getCutedFiles());
                    // STAF Location : staf://<ARCHIVE>/<NODE>/<full_file_name>?cut=3
                    break;
                case TAR:
                    // Add file to TAR
                    tarController.addFileToTar(pFileToArchive, pSTAFNode, tarsToArchive);
                    // STAF Location : staf://<ARCHIVE>/<NODE>/<tar_file>?file=<file_name>
                    break;
                case NORMAL:
                default:
                    // 1. Create simple file
                    PhysicalNormalFile simpleFile = new PhysicalNormalFile(pFileToArchive, pFileToArchive, pSTAFNode);
                    filesToArchive.add(simpleFile);
                    LOG.info("[STAF] New file prepared {} to staf : {}/{}", simpleFile.getLocalFile().toString(),
                             pSTAFNode, simpleFile.getSTAFFilePath());
                    // STAF Location : staf://<ARCHIVE>/<NODE>/<file_name>
                    break;
            }

        } catch (IOException | STAFTarException e) {
            throw new STAFException(e);
        }
    }

    /**
     * Cur a file which is too big to be archive in one part into STAF System.
     * @param pPhysicalFileToArchive
     * @param pStafNode
     * @return
     * @throws IOException
     */
    private PhysicalCutFile cutFile(Path pPhysicalFileToArchive, String pSTAFNode) throws IOException {

        // 1. Create cut temporary directory into workspace
        Path tmpCutDirectory = Paths.get(localWorkspace.toString(), TMP_DIRECTORY,
                                         pPhysicalFileToArchive.getFileName().toString());
        if (!tmpCutDirectory.toFile().exists()) {
            tmpCutDirectory.toFile().mkdirs();
        }

        // 3. Do cut files
        Set<File> cutedLocalFiles = CutFileUtils.cutFile(pPhysicalFileToArchive.toFile(), tmpCutDirectory.toString(),
                                                         stafConfiguration.getMaxFileSize());
        LOG.info("[STAF] Number of cuted files : {} for file {}", cutedLocalFiles.size(),
                 pPhysicalFileToArchive.toString());

        // 4. Create cut Physical file object to return
        Set<PhysicalNormalFile> cutedFiles = Sets.newHashSet();
        for (File cutedFile : cutedLocalFiles) {
            Path cutedFilePath = Paths.get(cutedFile.getPath());
            PhysicalNormalFile cutedFilePart = new PhysicalNormalFile(cutedFilePath, pPhysicalFileToArchive, pSTAFNode);
            cutedFiles.add(cutedFilePart);
        }
        PhysicalCutFile physicalCutFile = new PhysicalCutFile(pSTAFNode, pPhysicalFileToArchive, cutedFiles);
        physicalCutFile.addRawAssociatedFile(pPhysicalFileToArchive);
        return physicalCutFile;
    }

    /**
     * The archive mode to store file in STAF is calculated with the file size.
     * The modes are {@link STAFArchiveModeEnum}
     * @param pFileSize int
     * @return
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
     * Store given pFilesToArchiveMap in STAF.<br/>
     * For each element of pFilesToArchiveMap :<br/>
     *  - key : is the local path of the file to archive<br/>
     *  - value : is the staf file path to store.<br/>
     * @param pFilesToArchiveMap files to store
     * @param pReplaceMode replace file in STAF if already exists ?
     * @return list of successfuly stored local file path.
     * @throws STAFException staf store error
     */
    public Set<AbstractPhysicalFile> doArchivePreparedFiles(boolean pReplaceMode) throws STAFException {
        Set<String> archivedFiles = Sets.newHashSet();

        // Create map bewteen localFile to archive and destination file path into STAF
        Map<String, String> localFileToArchiveMap = Maps.newHashMap();
        this.getAllFilesToArchive().forEach(stafFile -> {
            if (PhysicalFileStatusEnum.TO_STORE.equals(stafFile.getStatus())) {
                if ((stafFile.getLocalFilePath() != null) && (stafFile.getSTAFFilePath() != null)) {
                    localFileToArchiveMap.put(stafFile.getLocalFilePath().toString(),
                                              stafFile.getSTAFFilePath().toString());
                } else {
                    stafFile.setStatus(PhysicalFileStatusEnum.ERROR);
                    LOG.warn("Undefined file to archive for origine(local)={} and destination(STAF)={}",
                             stafFile.getLocalFilePath(), stafFile.getSTAFFilePath());
                }
            }
        });

        stafService.connectArchiveSystem(ArchiveAccessModeEnum.ARCHIVE_MODE);
        try {
            archivedFiles = stafService.archiveFiles(localFileToArchiveMap, "/", pReplaceMode);

            archivedFiles.forEach(archivedFile ->
            // For each file to store, check if the file has really been stored and set the status to STORED.
            // @formatter:off
            this.getAllFilesToArchive()
                .stream()
                .filter(f -> PhysicalFileStatusEnum.TO_STORE.equals(f.getStatus()))
                .filter(f -> archivedFile.equals(f.getLocalFilePath().toString()))
                .forEach(f -> f.setStatus(PhysicalFileStatusEnum.STORED)));
            // @formatter:on

        } finally {
            stafService.disconnectArchiveSystem(ArchiveAccessModeEnum.ARCHIVE_MODE);
        }
        // Return all Physical file stored
        // @formatter:off
        return this.getAllFilesToArchive().stream()
                .filter(file -> PhysicalFileStatusEnum.STORED.equals(file.getStatus()))
                .collect(Collectors.toSet());
        // @formatter:on
    }

    public Set<Path> getRawFilesArchived() {
        // Get all raw associated files to each file in STORED status
        Set<Path> rawFilesStored = this.getAllFilesToArchive().stream()
                .filter(file -> PhysicalFileStatusEnum.STORED.equals(file.getStatus()))
                .flatMap(file -> file.getRawAssociatedFiles().stream()).distinct().collect(Collectors.toSet());

        // Add all files stored temporarly in PENDING tar
        // Files are concidered as STORED if there are in a current pending TAR. This TAR should be sent to
        // STAF during a future archive process if new files make the TAR file big enought to be send to STAF
        // system.
        rawFilesStored
                .addAll(tarsToArchive.stream().filter(tar -> PhysicalFileStatusEnum.PENDING.equals(tar.getStatus()))
                        .flatMap(file -> file.getRawAssociatedFiles().stream()).distinct().collect(Collectors.toSet()));

        return rawFilesStored;
    }

    public Set<AbstractPhysicalFile> getAllFilesToArchive() {
        Set<AbstractPhysicalFile> allFilesToArchive = Sets.newHashSet();
        allFilesToArchive.addAll(filesToArchive);
        allFilesToArchive.addAll(tarsToArchive);
        return allFilesToArchive;
    }

    public void clear() {
        // Clear already calculated files to archive
        filesToArchive.clear();
        tarsToArchive.clear();
    }

}
