package fr.cnes.regards.modules.storage.plugin.staf.domain;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
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
    private final Set<STAFPhysicalFile> filesToArchive = Sets.newHashSet();

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
                        String.format("STAF Local workspace %s is not readable", localWorkspace.toString()));
            }
            if (!Files.isWritable(localWorkspace)) {
                throw new IOException(
                        String.format("STAF Local workspace %s is not writable", localWorkspace.toString()));
            }
        }
        tarController = new TARController(pStafConfiguration, pLocalWorkspace);
    }

    /**
     * Prepare the list of file to archive into the STAF for the given files
     * @param pFileToArchivePerStafNode
     * @param pMode
     * @return
     * @throws STAFException
     */
    public Set<STAFPhysicalFile> prepareFilesToArchive(Map<String, Set<Path>> pFileToArchivePerStafNode,
            STAFArchiveModeEnum pMode) throws STAFException {

        this.clear();
        for (String stafNode : pFileToArchivePerStafNode.keySet()) {
            for (Path fileToArchive : pFileToArchivePerStafNode.get(stafNode)) {
                this.prepareFileToArchive(fileToArchive, stafNode, pMode);
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
    public void prepareFileToArchive(Path pFileToArchivePerStafNode, String pSTAFNode, STAFArchiveModeEnum pMode)
            throws STAFException {
        try {

            // 2. Manage file transformation if needed before staf storage
            switch (pMode) {
                case CUT:
                    // 1. Cut file in part
                    filesToArchive.add(cutFile(pFileToArchivePerStafNode, pSTAFNode));
                    // STAF Location : staf://<ARCHIVE>/<NODE>/<full_file_name>?cut=3
                    break;
                case TAR:
                    // Add file to TAR
                    tarController.addFileToTar(pFileToArchivePerStafNode, pSTAFNode, tarsToArchive);
                    // STAF Location : staf://<ARCHIVE>/<NODE>/<tar_file>?file=<file_name>
                    break;
                case NORMAL:
                default:
                    // 1. Create simple file
                    PhysicalFile simpleFile = new PhysicalFile(pSTAFNode, pFileToArchivePerStafNode,
                            getFileNameForSTAF(pFileToArchivePerStafNode));
                    filesToArchive.add(simpleFile);
                    LOG.info("New file prepared {} to staf : {}", simpleFile.getLocalFile().toString(),
                             simpleFile.getStafFileName());
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

        // 4. Create cut Physical file object to return
        Set<PhysicalFile> cutedFiles = Sets.newHashSet();
        for (File cutedFile : cutedLocalFiles) {
            Path cutedFilePath = Paths.get(cutedFile.getPath());
            cutedFiles.add(new PhysicalFile(pSTAFNode, cutedFilePath, getFileNameForSTAF(cutedFilePath)));
        }
        return new PhysicalCutFile(pSTAFNode, pPhysicalFileToArchive, cutedFiles);
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
    public List<String> archiveFiles(Map<String, String> pFilesToArchiveMap, boolean pReplaceMode)
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

    public Set<STAFPhysicalFile> getAllFilesToArchive() {
        Set<STAFPhysicalFile> allFilesToArchive = Sets.newHashSet();
        allFilesToArchive.addAll(filesToArchive);
        allFilesToArchive.addAll(tarsToArchive);
        return allFilesToArchive;
    }

    private String getFileNameForSTAF(Path pPhysicalFile) {

        // Name of the archive in STAF
        String result = null;

        if (stafConfiguration.isConvertInvalidCaracters()) {
            String fileName;
            fileName = pPhysicalFile.getFileName().toString();
            result = fileName.replace('-', '_');

            // If the archive file name is changed, add a warning in the log
            if (!result.equals(fileName)) {
                LOG.warn("File name changed to be archive to staf from <" + fileName + "> to <" + result + ">.");
            }
        } else {
            // Standard case, STAF Archive File Name is the Physical file name
            if (pPhysicalFile != null) {
                result = pPhysicalFile.getFileName().toString();
            }
        }
        return result;
    }

    public void clear() {
        // Clear already calculated files to archive
        filesToArchive.clear();
        tarsToArchive.clear();
    }

    public void storePreparedFiles() {
        // TODO Auto-generated method stub

    }

}
