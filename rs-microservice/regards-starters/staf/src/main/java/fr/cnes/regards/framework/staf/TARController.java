/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.staf;

import java.io.File;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.text.ParseException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.cnes.regards.framework.staf.domain.PhysicalFileStatusEnum;
import fr.cnes.regards.framework.staf.domain.PhysicalTARFile;
import fr.cnes.regards.framework.staf.domain.STAFConfiguration;
import fr.cnes.regards.framework.staf.exception.STAFTarException;
import fr.cnes.regards.framework.utils.file.compression.CompressionException;
import fr.cnes.regards.framework.utils.file.compression.CompressionFacade;
import fr.cnes.regards.framework.utils.file.compression.CompressionTypeEnum;

/**
 * STAF Controller to handle TAR files creation.
 * @author Sébastien Binda
 */
public class TARController {

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(TARController.class);

    /**
     * Global STAF Configuration
     */
    private final STAFConfiguration stafConfiguration;

    /**
     * STAF Workspace directory
     */
    private final Path workspaceDirectory;

    /**
     * Subdirectory containing tar creation workspace.
     */
    public static final String TAR_DIRECTORY = "tar";

    /**
     * Temporary current prefix for TAR working directories.
     */
    public static final String TAR_CURRENT_PREFIX = "_current";

    /**
     * Pattern of the directory containg the current pending TAR.
     */
    private static final Pattern TAR_CURRENT_DIRECTORY_PATTERN = Pattern
            .compile("^([0-9]{17})" + TAR_CURRENT_PREFIX + "$");

    /**
     * Name of the file used to create a lock on the pending TAR.
     */
    private static final String LOCK_FILE_NAME = ".staf_lock";

    /**
     * Date time format.
     */
    public static final String TAR_FILE_NAME_DATE_FORMAT = "yyyyMMddHHmmssSSS";

    /**
     * Pattern for well formed TAR archived using the staf library.
     */
    public static final Pattern TAR_FILE_NAME_DATE_PATTERN = Pattern.compile("^([0-9]{17}).tar$");

    /**
     * Constructor
     * @param pStafConfiguration Global STAF Configuration
     * @param pWorkspaceDirectory STAF Workspace directory
     */
    public TARController(STAFConfiguration pStafConfiguration, Path pWorkspaceDirectory) {
        super();
        stafConfiguration = pStafConfiguration;
        workspaceDirectory = pWorkspaceDirectory;
    }

    /**
     * Get a system physical lock on the workspace TAR directory associated to the given STAF Archive and STAF Node.
     * @param pSTAFArchiveName
     * @param pStafNode
     * @return {@link FileLock}. Need to be released with the
     *         {@link fr.cnes.regards.framework.staf.TARController#releaseLock} method
     * @throws STAFTarException
     */
    public FileLock getDirectoryLock(String pSTAFArchiveName, Path pStafNode) throws STAFTarException {
        // 1. Initialize lock on the tar current directory of the specified node
        Path localTarDirectory = getWorkingTarPath(pSTAFArchiveName, pStafNode);
        Path lockFile = Paths.get(localTarDirectory.toString(), LOCK_FILE_NAME);

        // If TAR directory for the given node doesn't exists, create it before lock
        try {
            Files.createDirectories(localTarDirectory);
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
            throw new STAFTarException(
                    String.format("[STAF] Error creating temporary tar directoty %s", localTarDirectory.toString()));
        }

        LOG.debug("[STAF] Getting lock for directory {}", lockFile.toString());
        FileLock lock = null;
        FileChannel fileChannel = null;
        try {
            fileChannel = FileChannel
                    .open(lockFile,
                          EnumSet.of(StandardOpenOption.CREATE, StandardOpenOption.READ, StandardOpenOption.WRITE));
            lock = fileChannel.lock();
        } catch (IOException e) {
            if (fileChannel != null) {
                try {
                    fileChannel.close();
                } catch (IOException e1) {
                    LOG.error(e1.getMessage(), e1);
                }
            }
            LOG.error(e.getMessage(), e);
            throw new STAFTarException(String.format("[STAF] Error getting lock on %s", lockFile.toString()));
        }
        LOG.debug("[STAF] Directory {} locked", lockFile.toString());

        return lock;
    }

    /**
     * Release a system physical lock
     * @param pLock
     */
    public void releaseLock(FileLock pLock) {
        if (pLock != null) {
            if (pLock.isValid()) {
                try {
                    pLock.release();
                } catch (IOException e) {
                    LOG.error("[STAF] Error releasing lock on file {}", pLock.toString(), e);
                }
            }
            if (pLock.channel() != null) {
                try {
                    pLock.channel().close();
                } catch (IOException e) {
                    LOG.error("[STAF] Error releasing lock on file {}", pLock.toString(), e);
                }
            }
        }
    }

    /**
     * Add a given file to the existing tar current file or if not to a new tar file.
     *
     * @param pPhysicalFileToArchive {@link File} to add into the TAR.
     * @param pFile {@link DataFile} associated to the {@link File} to add.
     * @param pStafNode Path into the staf archive where to store TAR.
     * @param pAlreadyPreparedTARFiles {@link Set} of {@link PhysicalTARFile} to archive.
     * @return {@link PhysicalTARFile} TAR file with the new added file in it.
     * @throws IOException : Unable to create new TAR current directory
     * @throws STAFTarException : Unable to add file to current TAR.
     */
    public PhysicalTARFile addFileToTar(Path pPhysicalFileToArchive, Set<PhysicalTARFile> pAlreadyPreparedTARFiles,
            String pSTAFArchiveName, Path pStafNode) throws STAFTarException {

        // 1. Get lock
        FileLock lock = getDirectoryLock(pSTAFArchiveName, pStafNode);
        try {
            // 2. Get the current creating tar file
            PhysicalTARFile workingTarPhysicalFile = getCurrentTarPhysicalFile(pAlreadyPreparedTARFiles,
                                                                               pSTAFArchiveName, pStafNode);
            // 3. Move file in the tar directory
            Path destinationFile = copyFileIntoTarCurrentDirectory(pPhysicalFileToArchive, workingTarPhysicalFile);
            // Add originale associated file
            workingTarPhysicalFile.addRawAssociatedFile(pPhysicalFileToArchive);
            LOG.debug("[STAF] File {} added to current TAR file (original file={})", destinationFile,
                      pPhysicalFileToArchive);
            if (workingTarPhysicalFile.getTarSize() > stafConfiguration.getMaxTarSize()) {
                // TAR size is over the maximum TAR file size, so do not add any more files and store it.
                LOG.debug("[STAF] Current TAR Size exceed limit ({}octets > {}octets), tar creation ... ",
                          workingTarPhysicalFile.getTarSize(), stafConfiguration.getMaxTarSize());
                createTAR(workingTarPhysicalFile);
            }
            return workingTarPhysicalFile;
        } catch (IOException e1) {
            throw new STAFTarException(e1);
        } finally {
            releaseLock(lock);
        }
    }

    /**
     * Update the given {@link PhysicalTARFile} with the local tar informations.<br/>
     * <ul>
     * <li>Calculate tar creation date.</li>
     * <li>Check if tar is locally stored.</li>
     * </ul>
     * @param pTARFile {@link PhysicalTARFile} to update
     */
    public void getLocalInformations(PhysicalTARFile pTARFile) {

        // 1. Check if STAF File name match the date format pattern
        Matcher matcher = TAR_FILE_NAME_DATE_PATTERN.matcher(pTARFile.getStafFileName());
        if (matcher.matches()) {
            // File name matches
            Date date;
            try {
                // Calculate associated local tar directory name
                date = DateUtils.parseDate(matcher.group(1), new String[] { TAR_FILE_NAME_DATE_FORMAT });
                LocalDateTime dateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(date.getTime()),
                                                                 ZoneId.systemDefault());
                // Update creation date information
                pTARFile.setLocalTarDirectoryCreationDate(dateTime);
                Path tarWorkingPath = getWorkingTarPath(pTARFile.getStafArchiveName(), pTARFile.getStafNode());
                String localName = getLocalTarCurrentDirName(dateTime);
                Path localTarPath = Paths.get(tarWorkingPath.toString(), localName);
                // Check if local dir exists
                if (localTarPath.toFile().exists() && localTarPath.toFile().isDirectory()) {
                    // Given TAR is locally stored. Update informations
                    pTARFile.setLocalTarDirectory(localTarPath);
                }
            } catch (ParseException e) {
                // Not a real error. The given TAR name does not match the library TAR names.
                // Maybe this TAR file was archived by an other system.
            }
        }
    }

    /**
     * Retrieve files from given {@link PhysicalTARFile} if the TAR is locally stored. Else do nothing.
     * @param pTARFileToRestore {@link PhysicalTARFile} from wich to restore files.
     * @return true if TAR files are locally restored.
     * @throws STAFTarException Error accessing locally stored files.
     */
    public boolean retrieveLocallyStoredTARFiles(PhysicalTARFile pTARFileToRestore, STAFCollectListener pListener)
            throws STAFTarException {
        boolean locallyRestored = false;
        // Check if TAR is locally stored
        Path localTarDir = pTARFileToRestore.getLocalTarDirectory();
        if ((localTarDir != null) && localTarDir.toFile().exists()) {
            // 1. Get lock on directory
            FileLock lock = null;
            try {
                lock = getDirectoryLock(pTARFileToRestore.getStafArchiveName(), pTARFileToRestore.getStafNode());
                LOG.info("[STAF] Retrieve from locally stored TAR file {}", pTARFileToRestore.getLocalTarDirectory());
                pListener.localTARCollectSucceed(pTARFileToRestore.getSTAFFilePath());
                locallyRestored = true;
            } finally {
                releaseLock(lock);
            }
        }
        return locallyRestored;
    }

    /**
     * Do copy the given {@link Path} file to the directory of the working given {@link PhysicalTARFile}
     * @param pFileToCopy {@link Path}
     * @param pTARFile {@link PhysicalTARFile}
     * @return {@link Path} of the file copied in the current TAR directory.
     * @throws STAFTarException Error during file copy.
     */
    private Path copyFileIntoTarCurrentDirectory(Path pFileToCopy, PhysicalTARFile pTARFile) throws STAFTarException {
        Path destinationFile = Paths.get(pTARFile.getLocalTarDirectory().toString(),
                                         pFileToCopy.getFileName().toString());
        try {
            // Copy file to current TAR directory and check new tar size.
            Files.copy(pFileToCopy, destinationFile, StandardCopyOption.REPLACE_EXISTING);
            Long size = Files.walk(pTARFile.getLocalTarDirectory()).filter(f -> f.toFile().isFile())
                    .mapToLong(p -> p.toFile().length()).sum();
            pTARFile.setTarSize(size);
            pTARFile.addFileInTar(destinationFile, pFileToCopy);
            return destinationFile;
        } catch (IOException e) {
            pTARFile.setStatus(PhysicalFileStatusEnum.ERROR);
            throw new STAFTarException(e);
        }
    }

    /**
     * Finish and create the current pending TAR if his size or creation date is over the limit configuration value.
     * @param pTarFiles
     * @throws STAFTarException
     */
    public void createPreparedTAR(Set<PhysicalTARFile> pTarFiles) throws STAFTarException {
        Optional<PhysicalTARFile> tar = pTarFiles.stream()
                .filter(t -> PhysicalFileStatusEnum.PENDING.equals(t.getStatus())).findFirst();
        if (tar.isPresent()) {
            PhysicalTARFile workingTarPhysicalFile = tar.get();
            Long tarSize = workingTarPhysicalFile.getTarSize();
            LocalDateTime tarCreationDate = workingTarPhysicalFile.getLocalTarDirectoryCreationDate();
            LocalDateTime dateLimit = tarCreationDate.plusHours(stafConfiguration.getMaxTarArchivingHours());
            if (LocalDateTime.now().isAfter(dateLimit) || (tarSize > stafConfiguration.getTarSizeThreshold())) {
                // If TAR size is over the minimal limit or if TAR date creation is over the limite date, store TAR.
                LOG.info("[STAF] Current TAR can be created (size={}, creation date={}). ", tarSize,
                         tarCreationDate.format(DateTimeFormatter.ofPattern(TAR_FILE_NAME_DATE_FORMAT)));
                createTAR(workingTarPhysicalFile);
            } else {
                LOG.info("[STAF] Current TAR {} not big enougth to be stored in staf ({}octets < {}octets && {} < {})",
                         workingTarPhysicalFile.getLocalTarDirectory(), workingTarPhysicalFile.getTarSize(),
                         stafConfiguration.getTarSizeThreshold(), LocalDateTime.now(), dateLimit);
            }
        }
    }

    /**
     * Do compress the files from the given {@link PhysicalTARFile}.
     * @param pTARToCreate {@link PhysicalTARFile} to create.
     * @throws STAFTarException Error during TAR Compression.
     */
    private void createTAR(PhysicalTARFile pTARToCreate) throws STAFTarException {
        LOG.debug("[STAF] Creating TAR file {}", pTARToCreate.getLocalFilePath());
        CompressionFacade facade = new CompressionFacade();
        try {
            List<File> filesToTar = Files.walk(pTARToCreate.getLocalTarDirectory()).filter(f -> f.toFile().isFile())
                    .filter(f -> !LOCK_FILE_NAME.equals(f.toFile().getName())).map(f -> f.toFile())
                    .collect(Collectors.toList());
            facade.compress(CompressionTypeEnum.TAR, pTARToCreate.getLocalTarDirectory().toFile(), filesToTar,
                            getFileWithoutExtension(pTARToCreate.getLocalFilePath()),
                            pTARToCreate.getLocalTarDirectory().toFile(), true, false);
            // Delete curent directory and all associated files
            FileUtils.deleteDirectory(pTARToCreate.getLocalTarDirectory().toFile());
        } catch (IOException | CompressionException e) {
            pTARToCreate.setStatus(PhysicalFileStatusEnum.ERROR);
            throw new STAFTarException(e);
        }
        // Set TAR Status to "TO_STORE"
        pTARToCreate.setStatus(PhysicalFileStatusEnum.TO_STORE);
        LOG.info("[STAF] TAR FILE created and ready to send to STAF System : {}", pTARToCreate.getLocalFilePath());
    }

    /**
     * Delete filesInTar from the given {@link PhysicalTARFile} into the local tar directory.
     * This method is used to delete files from a tar locally stored.
     * @param pTARFile {@link PhysicalTARFile}
     */
    public void deleteFilesFromLocalTAR(PhysicalTARFile pTARFile) {
        FileLock lock = null;
        try {
            // 1. Check local tar exists
            Path localTarDir = pTARFile.getLocalTarDirectory();
            if (pTARFile.getStatus().equals(PhysicalFileStatusEnum.TO_DELETE) && (localTarDir != null)
                    && localTarDir.toFile().exists()) {
                // 2. Get lock
                lock = getDirectoryLock(pTARFile.getStafArchiveName(), pTARFile.getStafNode());
                pTARFile.getFilesInTar().forEach((raw, path) -> {
                    Path fileInTarPath = Paths.get(localTarDir.toString(), path.toString());
                    if (fileInTarPath.toFile().exists()) {
                        fileInTarPath.toFile().delete();
                    }
                });
                // If there no files left, delete local directory
                try {
                    if (!Files.walk(localTarDir).filter(f -> f.toFile().isFile()).findFirst().isPresent()) {
                        // Delete directory
                        FileUtils.deleteDirectory(localTarDir.toFile());
                    }
                } catch (IOException e) {
                    LOG.error("[STAF]Error getting files from TAR current directory {}", localTarDir.toString(), e);
                }
                pTARFile.setStatus(PhysicalFileStatusEnum.DELETED);
            }
        } catch (STAFTarException e) {
            LOG.error("[STAF] Error deleting files form local tar. {}", e.getMessage(), e);
        } finally {
            releaseLock(lock);
        }
    }

    /**
     * Delete the files into the given {@link PhysicalTARFile}.<br/>
     * Files to delete are the filesInTar properties of the {@link PhysicalTARFile}.</br>
     * If there is no remaining files in the TAR, then this method return true. Else this method return false.
     * @param pTARFile {@link PhysicalTARFile}
     * @return Does the all TAR file is deleted ?
     * @throws STAFTarException
     */
    public boolean deleteFilesFromTAR(PhysicalTARFile pTARFile) throws STAFTarException {

        boolean tarFullDeletion = false;
        // 1. Init tmp decompression directory
        Path decompressDir = Paths
                .get(getWorkingTarPath(pTARFile.getStafArchiveName(), pTARFile.getStafNode()).toString(),
                     "." + pTARFile.getLocalFilePath().getFileName().toString());
        if (!decompressDir.toFile().exists()) {
            try {
                Files.createDirectories(decompressDir);
            } catch (IOException e) {
                throw new STAFTarException(e);
            }
        }

        // 2. Check file existance
        if (!pTARFile.getLocalFilePath().toFile().exists()) {
            throw new STAFTarException(
                    String.format("[STAF] Decompression - TAR file %s does not exists", pTARFile.getLocalFilePath()));
        }
        // 3. Decompress TAR
        CompressionFacade facade = new CompressionFacade();
        try {
            facade.decompress(CompressionTypeEnum.TAR, pTARFile.getLocalFilePath().toFile(), decompressDir.toFile());
        } catch (CompressionException e) {
            throw new STAFTarException(String.format("[STAF] Decompression - Error during TAR decompression %s",
                                                     pTARFile.getLocalFilePath()),
                    e);
        }

        // 4. delete files into tar decompress directory
        for (Entry<Path, Path> fileInTar : pTARFile.getFilesInTar().entrySet()) {
            try {
                Path fileToDelete = Paths.get(decompressDir.toString(), fileInTar.getKey().getFileName().toString());
                if (fileToDelete.toFile().exists()) {
                    Files.delete(Paths.get(decompressDir.toString(), fileInTar.getKey().getFileName().toString()));
                }
            } catch (IOException e) {
                throw new STAFTarException(e);
            }
        }

        // 5. Delete original TAR file
        try {
            Files.delete(pTARFile.getLocalFilePath());
        } catch (IOException e) {
            throw new STAFTarException(e);
        }

        // 6. If there is still files in the decompression directory, reconstruct it.
        try {
            Set<Path> remainingFiles = Files.walk(decompressDir).filter(f -> f.toFile().isFile()).map(Path::getFileName)
                    .collect(Collectors.toSet());
            if (remainingFiles.isEmpty()) {
                // No remaining files, delete all TAR from STAF.
                tarFullDeletion = true;
            } else {
                // Add remaining files into the replacement TAR to STAF.
                facade.compress(CompressionTypeEnum.TAR, decompressDir.toFile(), null,
                                pTARFile.getLocalFilePath().toFile(), null, true, false);
            }
        } catch (IOException | CompressionException e) {
            throw new STAFTarException(e);
        }
        return tarFullDeletion;
    }

    /**
     * Retrieve, if exists, the current pending tar as a {@link PhysicalTARFile}.
     * @param pWorkspaceTarPath {@link Path} of the TAR Workspace.
     * @param pSTAFArchiveName {@link String} Name of the STAF Archive.
     * @param pSTAFNode {@link Path} STAF Node.
     * @return {@link Optional}<{@link PhysicalTARFile}
     */
    private Optional<PhysicalTARFile> findPendingTarDirectory(Path pWorkspaceTarPath, String pSTAFArchiveName,
            Path pSTAFNode) {
        Optional<PhysicalTARFile> tarFile = Optional.empty();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(pWorkspaceTarPath,
                                                                     path -> path.toFile().isDirectory())) {
            for (Path dir : stream) {
                // Check if the directory match the pending current TAR directory regex.
                Matcher matcher = TAR_CURRENT_DIRECTORY_PATTERN.matcher(dir.getFileName().toString());
                if (matcher.matches()) {
                    // Pending current TAR directory found. Create the new associated PhysicalTARFile
                    // The creation date of the pending directory can be read in the directory name.
                    tarFile = Optional
                            .of(createPendingTarFromDirectory(dir, pSTAFArchiveName, pSTAFNode, matcher.group(1)));
                }
            }
        } catch (IOException e) {
            LOG.error("[STAF] Tar preparation - Error looking for current pending tar", e);
        }
        return tarFile;
    }

    /**
     * Create a {@link PhysicalTARFile} associated to the given current TAR pending directory.
     * @param pDirectory {@link Path} to current pending TAR directory.
     * @param pSTAFArchiveName {@link String} STAF Archive name.
     * @param pSTAFNode {@link Path} STAF Node
     * @param pDateStr {@link String} Creation date of the directory with TAR_FILE_NAME_DATA_FORMAT format.
     * @return {@link PhysicalTARFile} of the current pending TAR directory.
     */
    private PhysicalTARFile createPendingTarFromDirectory(Path pDirectory, String pSTAFArchiveName, Path pSTAFNode,
            String pDateStr) {
        try {
            Date date = DateUtils.parseDate(pDateStr, new String[] { TAR_FILE_NAME_DATE_FORMAT });
            LocalDateTime dateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(date.getTime()),
                                                             ZoneId.systemDefault());
            PhysicalTARFile pendingTarFile = new PhysicalTARFile(pSTAFArchiveName, pSTAFNode,
                    getLocalTarFileName(dateTime));
            // Set TAR directory path
            pendingTarFile.setLocalTarDirectory(pDirectory);
            // Set futur TAR file path
            pendingTarFile
                    .setLocalFilePath(Paths.get(pDirectory.getParent().toString(), getLocalTarFileName(dateTime)));
            pendingTarFile.setLocalTarDirectoryCreationDate(dateTime);
            return pendingTarFile;
        } catch (ParseException e) {
            LOG.error("[STAF] Error parsing date from TAR current directory {}", pDirectory);
            return null;
        }
    }

    /**
     * Get the PhysicalTARFile associated to the current working TAR.
     * The current working TAR, is a directory containing all files waiting to be send to STAF system as compressed into
     * a TAR file.
     * The TAR File is sent to the staf only if the minimum size of a TAR is raised.
     * If no current TAR is found, a new one is created.
     *
     * @param pTarFiles {@link PhysicalTARFile}s already prepared.
     * @param pSTAFArchiveName Name of the STAF Archive name where to store the TAR File.
     * @param pSTAFNode Path of the STAF Node where to store the TAR File.
     * @return The {@link PhysicalTARFile} associated to the current working TAR.
     * @throws IOException If an error occured during TAR working directory management.
     */
    private PhysicalTARFile getCurrentTarPhysicalFile(Set<PhysicalTARFile> pTarFiles, String pSTAFArchiveName,
            Path pSTAFNode) throws IOException {

        // If the current TAR file is already present in the given list do not calculate it.
        Optional<PhysicalTARFile> tar = pTarFiles.stream()
                .filter(t -> PhysicalFileStatusEnum.PENDING.equals(t.getStatus())).findFirst();

        if (!tar.isPresent()) {
            // The current TAR is not already calculated.
            // Get the current pending TAR from workspace if exists.
            PhysicalTARFile workingTar;
            Path tarPath = getWorkingTarPath(pSTAFArchiveName, pSTAFNode);
            // Check into the TAR directory if a pending one exists for the given STAFNode.
            Optional<PhysicalTARFile> existingPendingTar = findPendingTarDirectory(tarPath, pSTAFArchiveName,
                                                                                   pSTAFNode);
            if (existingPendingTar.isPresent()) {
                workingTar = existingPendingTar.get();
            } else {
                LocalDateTime date = LocalDateTime.now();
                String tarName = date.format(DateTimeFormatter.ofPattern(TAR_FILE_NAME_DATE_FORMAT));
                Path newTarDirectory = Paths.get(tarPath.toString(), tarName + "_current");
                workingTar = new PhysicalTARFile(pSTAFArchiveName, pSTAFNode, getLocalTarFileName(date));
                Files.createDirectories(newTarDirectory);
                workingTar.setLocalFilePath(Paths.get(tarPath.toString(), getLocalTarFileName(date)));
                workingTar.setLocalTarDirectory(newTarDirectory);
                workingTar.setLocalTarDirectoryCreationDate(date);
            }

            // Get already present files in the current tar folder
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(workingTar.getLocalTarDirectory(),
                                                                         path -> path.toFile().isFile())) {
                // For old files in TAR, we don't know the raw file path. We only have the current file in tar.
                stream.forEach(file -> workingTar.addFileInTar(file, null));
            }

            // Add working tar file to the tar list.
            pTarFiles.add(workingTar);
            return workingTar;
        } else {
            return tar.get();
        }
    }

    /**
     * Return a file name without his extension.
     * @param pFilePath {@link Path} Path to the file.
     * @return {@link File} without file extension.
     */
    private File getFileWithoutExtension(Path pFilePath) {
        return new File(pFilePath.toString().replaceFirst("[.][^.]+$", ""));
    }

    /**
     * Calculate a TAR file name for the current working TAR.
     *
     * @param pDate {@link LocalDateTime} The TAR working directory creation date.
     * @return {@link String} TAR file name.
     */
    private String getLocalTarFileName(LocalDateTime pDate) {
        return String.format("%s.tar", pDate.format(DateTimeFormatter.ofPattern(TAR_FILE_NAME_DATE_FORMAT)));
    }

    /**
     * Calculate a TAR file name for the current working TAR.
     *
     * @param pDate {@link LocalDateTime} The TAR working directory creation date.
     * @return {@link String} TAR file name.
     */
    private String getLocalTarCurrentDirName(LocalDateTime pDate) {
        return String.format("%s%s", pDate.format(DateTimeFormatter.ofPattern(TAR_FILE_NAME_DATE_FORMAT)),
                             TAR_CURRENT_PREFIX);
    }

    /**
     * Retrieve the workspace working directory to create new TAR for the given STAF Archive name and STAF Node.
     * @param pSTAFArchiveName {@link String} STAF Archive name
     * @param pStafNode {@link String} STAF Node
     * @return {@link Path} to the TAR working directory
     */
    private Path getWorkingTarPath(String pSTAFArchiveName, Path pStafNode) {
        return Paths.get(workspaceDirectory.toString(), pSTAFArchiveName, TAR_DIRECTORY, pStafNode.toString());
    }

}
