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

public class TARController {

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(TARController.class);

    private final STAFConfiguration stafConfiguration;

    private final Path workspaceDirectory;

    public static final String TAR_DIRECTORY = "tar";

    private static final Pattern TAR_CURRENT_DIRECTORY_PATTERN = Pattern.compile("^([0-9]{17})_current$");

    private static final String LOCK_FILE_NAME = ".staf_lock";

    public static final String TAR_FILE_NAME_DATA_FORMAT = "yyyyMMddHHmmssSSS";

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
            String pStafNode) throws STAFTarException, IOException {

        // 1. Initialize lock on the tar current directory of the specified node
        Path localTarDirectory = Paths.get(workspaceDirectory.toString(), TAR_DIRECTORY, pStafNode);
        Path lockFile = Paths.get(localTarDirectory.toString(), LOCK_FILE_NAME);

        // If TAR directory for the given doesn't exists, create it before lock
        Files.createDirectories(localTarDirectory);

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
                LOG.info("[STAF] Current TAR {} not big enougth to be stored in staf ({}octets < {}octets)",
                         workingTarPhysicalFile.getLocalTarDirectory(), size, stafConfiguration.getMaxTarSize());
            }

            LOG.debug("[STAF] Releasing lock for directory {}", lockFile.toString());
            lock.release();
            LOG.debug("[STAF] Lock released for directory {}", lockFile.toString());
        } catch (IOException | CompressionException e) {
            // Error adding file to tar
            if ((lock != null) && lock.isValid()) {
                LOG.debug("[STAF] Releasing lock for directory {}", lockFile.toString());
                lock.release();
                LOG.debug("[STAF] Lock released for directory {}", lockFile.toString());
            }
            throw new STAFTarException(e);
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
            // Create the new TarPhysicalFile

            PhysicalTARFile newWorkingTar = new PhysicalTARFile(pSTAFArchiveName, pSTAFNode);
            Path tarPath = Paths.get(workspaceDirectory.toString(), TAR_DIRECTORY, pSTAFNode);
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(tarPath,
                                                                         path -> path.toFile().isDirectory())) {
                stream.forEach(dir -> {
                    Matcher matcher = TAR_CURRENT_DIRECTORY_PATTERN.matcher(dir.getFileName().toString());
                    if (matcher.matches()) {
                        String dateStr = matcher.group(1);
                        Date date;
                        try {
                            date = DateUtils.parseDate(dateStr, new String[] { TAR_FILE_NAME_DATA_FORMAT });
                            LocalDateTime dateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(date.getTime()),
                                                                             ZoneId.systemDefault());
                            // Set TAR directory path
                            newWorkingTar.setLocalTarDirectory(dir);
                            // Set futur TAR file path
                            newWorkingTar.setLocalTarFile(Paths.get(tarPath.toString(), getLocalTarFileName(dateTime)));
                            newWorkingTar.setLocalTarDirectoryCreationDate(dateTime);
                        } catch (ParseException e) {
                            LOG.error("[STAF] Error parsing date from TAR current directory {}", dir);
                        }
                    }
                });
            }

            // No working tar exists, so create a new one
            if (newWorkingTar.getLocalTarDirectory() == null) {
                LocalDateTime date = LocalDateTime.now();
                String tarName = date.format(DateTimeFormatter.ofPattern(TAR_FILE_NAME_DATA_FORMAT));
                Path newTarDirectory = Paths.get(tarPath.toString(), tarName + "_current");
                Files.createDirectories(newTarDirectory);
                newWorkingTar.setLocalTarFile(Paths.get(tarPath.toString(), getLocalTarFileName(date)));
                newWorkingTar.setLocalTarDirectory(newTarDirectory);
                newWorkingTar.setLocalTarDirectoryCreationDate(date);
            }

            // Get already present files in the current tar folder
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(newWorkingTar.getLocalTarDirectory(),
                                                                         path -> path.toFile().isFile())) {
                // For old files in TAR, we don't know the raw file path. We only have the current file in tar.
                stream.forEach(file -> newWorkingTar.addFileInTar(file, null));
            }

            // Add working tar file to the tar list.
            pTarFiles.add(newWorkingTar);
            return newWorkingTar;
        } else {
            return tar.get();
        }
    }

    /**
     * Calculate a TAR file name for the current working TAR.
     *
     * @param pDate {@link LocalDateTime} The TAR working directory creation date.
     * @return
     */
    private String getLocalTarFileName(LocalDateTime pDate) {
        return String.format("%s.tar", pDate.format(DateTimeFormatter.ofPattern(TAR_FILE_NAME_DATA_FORMAT)));

    }

    private File getFileWithoutExtension(Path pFilePath) {
        return new File(pFilePath.toString().replaceFirst("[.][^.]+$", ""));
    }

}
