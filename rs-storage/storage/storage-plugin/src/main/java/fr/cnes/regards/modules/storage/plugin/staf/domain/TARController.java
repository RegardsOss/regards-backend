/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.storage.plugin.staf.domain;

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
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.cnes.regards.framework.file.utils.compression.CompressionException;
import fr.cnes.regards.framework.file.utils.compression.CompressionFacade;
import fr.cnes.regards.framework.file.utils.compression.CompressionTypeEnum;
import fr.cnes.regards.framework.staf.STAFConfiguration;
import fr.cnes.regards.modules.storage.domain.database.DataFile;
import fr.cnes.regards.modules.storage.plugin.staf.domain.exception.STAFTarException;

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
     * Pattern of the directory containg the current pending TAR.
     */
    private static final Pattern TAR_CURRENT_DIRECTORY_PATTERN = Pattern.compile("^([0-9]{17})_current$");

    /**
     * Name of the file used to create a lock on the pending TAR.
     */
    private static final String LOCK_FILE_NAME = ".staf_lock";

    /**
     * Date time format.
     */
    public static final String TAR_FILE_NAME_DATA_FORMAT = "yyyyMMddHHmmssSSS";

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
     * Add a given file to the existing tar current file or if not to new tar file.
     *
     * @param pPhysicalFileToArchive {@link File} to add into the TAR.
     * @param pFile {@link DataFile} associated to the {@link File} to add.
     * @param pStafNode Path into the staf archive where to store TAR.
     * @param pTarFiles {@link Set} of {@link PhysicalTARFile} to archive.
     * @throws IOException : Unable to create new TAR current directory
     * @throws STAFTarException : Unable to add file to current TAR.
     */
    public void addFileToTar(Path pPhysicalFileToArchive, Set<PhysicalTARFile> pTarFiles, String pSTAFArciveName,
            String pStafNode) throws STAFTarException {

        // 1. Initialize lock on the tar current directory of the specified node
        Path localTarDirectory = getWorkingTarPath(pSTAFArciveName, pStafNode);
        Path lockFile = Paths.get(localTarDirectory.toString(), LOCK_FILE_NAME);

        // If TAR directory for the given doesn't exists, create it before lock
        try {
            Files.createDirectories(localTarDirectory);
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
            throw new STAFTarException(
                    String.format("[STAF] Error creating temporary tar directoty %s", localTarDirectory.toString()));
        }

        // 2. Get lock
        FileLock lock = null;
        try (FileChannel fileChannel = FileChannel
                .open(lockFile,
                      EnumSet.of(StandardOpenOption.CREATE, StandardOpenOption.READ, StandardOpenOption.WRITE))) {
            LOG.debug("[STAF] Getting lock for directory {}", lockFile.toString());
            lock = fileChannel.lock();
            LOG.debug("[STAF] Directory {} locked", lockFile.toString());

            // 3. Get the current creating tar file
            PhysicalTARFile workingTarPhysicalFile = getCurrentTarPhysicalFile(pTarFiles, pSTAFArciveName, pStafNode);

            // 4. Move file in the tar directory
            Path sourceFile = pPhysicalFileToArchive;
            Path destinationFile = Paths.get(workingTarPhysicalFile.getLocalTarDirectory().toString(),
                                             pPhysicalFileToArchive.getFileName().toString());
            Files.copy(sourceFile, destinationFile, StandardCopyOption.REPLACE_EXISTING);
            // Add files into TAR
            workingTarPhysicalFile.addFileInTar(destinationFile, pPhysicalFileToArchive);
            // Add originale associated file
            workingTarPhysicalFile.addRawAssociatedFile(pPhysicalFileToArchive);
            LOG.debug("[STAF] File {} added to current TAR file (original file={})", destinationFile,
                      pPhysicalFileToArchive);

            // 5. Check TAR size
            long size = Files.walk(workingTarPhysicalFile.getLocalTarDirectory()).filter(f -> f.toFile().isFile())
                    .mapToLong(p -> p.toFile().length()).sum();
            if (size > stafConfiguration.getMaxTarSize()) {
                LOG.debug("[STAF] Current TAR Size exceed limit ({}octets > {}octets), tar creation ... ", size,
                          stafConfiguration.getMaxTarSize());
                // No other files can be added in tar.
                // Create TAR
                LOG.debug("[STAF] Creating TAR file {}", workingTarPhysicalFile.getLocalTarFile());
                CompressionFacade facade = new CompressionFacade();
                List<File> filesToTar = Files.walk(workingTarPhysicalFile.getLocalTarDirectory())
                        .filter(f -> f.toFile().isFile()).filter(f -> !LOCK_FILE_NAME.equals(f.toFile().getName()))
                        .map(f -> f.toFile()).collect(Collectors.toList());
                facade.compress(CompressionTypeEnum.TAR, workingTarPhysicalFile.getLocalTarDirectory().toFile(),
                                filesToTar, getFileWithoutExtension(workingTarPhysicalFile.getLocalTarFile()),
                                workingTarPhysicalFile.getLocalTarDirectory().toFile(), true, false);
                // Set the TAR local path
                workingTarPhysicalFile.setStatus(PhysicalFileStatusEnum.TO_STORE);
                LOG.info("[STAF] TAR FILE created and ready to send to STAF System : {}",
                         workingTarPhysicalFile.getLocalTarFile());
                // Delete curent directory and all associated files
                FileUtils.deleteDirectory(workingTarPhysicalFile.getLocalTarDirectory().toFile());
            } else {
                LOG.debug("[STAF] Current TAR {} not big enougth to be stored in staf ({}octets < {}octets)",
                          workingTarPhysicalFile.getLocalTarDirectory(), size, stafConfiguration.getMaxTarSize());
            }

            LOG.debug("[STAF] Releasing lock for directory {}", lockFile.toString());
            lock.release();
            LOG.debug("[STAF] Lock released for directory {}", lockFile.toString());
        } catch (IOException | CompressionException e) {
            // Error adding file to tar
            if ((lock != null) && lock.isValid()) {
                LOG.debug("[STAF] Releasing lock for directory {}", lockFile.toString());
                try {
                    lock.release();
                } catch (IOException e1) {
                    LOG.error("[STAF] Error releasing lock for directory {}", lockFile.toString(), e1);
                }
                LOG.debug("[STAF] Lock released for directory {}", lockFile.toString());
            }
            throw new STAFTarException(e);
        }
    }

    /**
     * Retrieve, if exists, the current pending tar as a {@link PhysicalTARFile}.
     * @param pWorkspaceTarPath {@link Path} of the TAR Workspace.
     * @param pSTAFArchiveName {@link String} Name of the STAF Archive.
     * @param pSTAFNode {@link String} STAF Node.
     * @return {@link Optional}<{@link PhysicalTARFile}
     */
    private Optional<PhysicalTARFile> findPendingTarDirectory(Path pWorkspaceTarPath, String pSTAFArchiveName,
            String pSTAFNode) {
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
     * @param pSTAFNode {@link String} STAF Node
     * @param pDateStr {@link String} Creation date of the directory with TAR_FILE_NAME_DATA_FORMAT format.
     * @return {@link PhysicalTARFile} of the current pending TAR directory.
     */
    private PhysicalTARFile createPendingTarFromDirectory(Path pDirectory, String pSTAFArchiveName, String pSTAFNode,
            String pDateStr) {
        try {
            PhysicalTARFile pendingTarFile = new PhysicalTARFile(pSTAFArchiveName, pSTAFNode);
            Date date = DateUtils.parseDate(pDateStr, new String[] { TAR_FILE_NAME_DATA_FORMAT });
            LocalDateTime dateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(date.getTime()),
                                                             ZoneId.systemDefault());
            // Set TAR directory path
            pendingTarFile.setLocalTarDirectory(pDirectory);
            // Set futur TAR file path
            pendingTarFile.setLocalTarFile(Paths.get(pDirectory.getParent().toString(), getLocalTarFileName(dateTime)));
            pendingTarFile.setLocalTarDirectoryCreationDate(dateTime);
            return pendingTarFile;
        } catch (ParseException e) {
            LOG.error("[STAF] Error parsing date from TAR current directory {}", pDirectory);
            return null;
        }
    }

    /**
     * Get the PhysicalTARFile associated to the current working TAR.
     * The current working TAR, is a directory containing all files waiting to be send to STAF system as compressed into a TAR file.
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
            String pSTAFNode) throws IOException {

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
                workingTar = new PhysicalTARFile(pSTAFArchiveName, pSTAFNode);
                LocalDateTime date = LocalDateTime.now();
                String tarName = date.format(DateTimeFormatter.ofPattern(TAR_FILE_NAME_DATA_FORMAT));
                Path newTarDirectory = Paths.get(tarPath.toString(), tarName + "_current");
                Files.createDirectories(newTarDirectory);
                workingTar.setLocalTarFile(Paths.get(tarPath.toString(), getLocalTarFileName(date)));
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
        return String.format("%s.tar", pDate.format(DateTimeFormatter.ofPattern(TAR_FILE_NAME_DATA_FORMAT)));

    }

    /**
     * Retrieve the workspace working directory to create new TAR for the given STAF Archive name and STAF Node.
     * @param pSTAFArciveName {@link String} STAF Archive name
     * @param pStafNode {@link String} STAF Node
     * @return {@link Path} to the TAR working directory
     */
    private Path getWorkingTarPath(String pSTAFArciveName, String pStafNode) {
        return Paths.get(workspaceDirectory.toString(), pSTAFArciveName, TAR_DIRECTORY, pStafNode);
    }

}
