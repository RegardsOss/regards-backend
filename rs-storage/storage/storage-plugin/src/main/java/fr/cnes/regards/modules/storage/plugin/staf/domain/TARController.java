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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.Vector;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.assertj.core.util.Sets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.cnes.regards.framework.file.utils.compression.CompressManager;
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

    private static final String TAR_DIRECTORY = "tar";

    private static final String TAR_CURRENT_DIRECTORY = "current";

    private static final String LOCK_FILE_NAME = ".staf_lock";

    private static final String TAR_FILE_NAME_DATA_FORMAT = "yyyyMMddHHmm";

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
    public void addFileToTar(Path pPhysicalFileToArchive, String pStafNode, Set<PhysicalTARFile> pTarFiles)
            throws STAFTarException, IOException {

        Path localTarDirectory = Paths.get(workspaceDirectory.toString(), TAR_DIRECTORY, pStafNode);
        Path localCurrentTarDirectory = Paths.get(workspaceDirectory.toString(), TAR_DIRECTORY, pStafNode,
                                                  TAR_CURRENT_DIRECTORY);
        Path lockFile = Paths.get(localCurrentTarDirectory.toString(), LOCK_FILE_NAME);

        // 1. Create new current TAR directory if doesnt exists
        if (!localCurrentTarDirectory.toFile().exists()) {
            // No TAR waiting to be archive so create a new one
            Files.createDirectories(localCurrentTarDirectory);
        }

        // 2. Get lock on directory to avoid an other process to add file into.
        FileLock lock = null;
        try (FileChannel fileChannel = FileChannel
                .open(lockFile,
                      EnumSet.of(StandardOpenOption.CREATE, StandardOpenOption.READ, StandardOpenOption.WRITE))) {
            LOG.debug("[STAF] Getting lock for directory {}", lockFile.toString());
            lock = fileChannel.lock();
            LOG.debug("[STAF] Directory {} locked", lockFile.toString());

            // 3. Get the current creating tar file
            PhysicalTARFile workingTarPhysicalFile = getCurrentTarPhysicalFile(pTarFiles, pStafNode);

            // 4. Move file in the tar directory
            Path sourceFile = pPhysicalFileToArchive;
            Path destinationFile = Paths.get(localCurrentTarDirectory.toString(),
                                             pPhysicalFileToArchive.getFileName().toString());
            Files.copy(sourceFile, destinationFile, StandardCopyOption.REPLACE_EXISTING);
            // Add files into TAR
            workingTarPhysicalFile.getLocalFiles().add(destinationFile);
            // Add originale associated file
            workingTarPhysicalFile.addRawAssociatedFile(pPhysicalFileToArchive);
            LOG.info("[STAF] File {} added to current TAR file (original file={})", destinationFile,
                     pPhysicalFileToArchive);

            // 5. Check TAR size
            long size = Files.walk(localCurrentTarDirectory).filter(f -> f.toFile().isFile())
                    .mapToLong(p -> p.toFile().length()).sum();
            if (size > stafConfiguration.getMaxTarSize()) {
                LOG.info("[STAF] Current TAR Size exceed limit ({}octets > {}octets), tar creation ... ", size,
                         stafConfiguration.getMaxTarSize());
                // No other files can be added in tar.
                // Create TAR
                String tarName = LocalDateTime.now().format(DateTimeFormatter.ofPattern(TAR_FILE_NAME_DATA_FORMAT));
                File tarFileWithoutExt = Paths.get(localTarDirectory.toString(), tarName).toFile();
                File tarFileWithExt = Paths.get(localTarDirectory.toString(), tarName + ".tar").toFile();
                int cpt = 1;
                while (tarFileWithExt.exists()) {
                    tarFileWithoutExt = Paths.get(localTarDirectory.toString(), tarName + "_" + cpt).toFile();
                    tarFileWithExt = Paths.get(localTarDirectory.toString(), tarName + "_" + cpt + ".tar").toFile();
                }
                LOG.info("[STAF] Creating TAR file {}", tarFileWithExt.getPath());
                CompressionFacade facade = new CompressionFacade();
                List<File> filesToTar = Files.walk(localCurrentTarDirectory).filter(f -> f.toFile().isFile())
                        .filter(f -> !LOCK_FILE_NAME.equals(f.toFile().getName())).map(f -> f.toFile())
                        .collect(Collectors.toList());
                Vector<CompressManager> manager = facade
                        .compress(CompressionTypeEnum.TAR, localCurrentTarDirectory.toFile(), filesToTar,
                                  tarFileWithoutExt, localCurrentTarDirectory.toFile(), true, false);
                // Set the TAR local path
                tarFileWithExt = manager.firstElement().getCompressedFile();
                workingTarPhysicalFile.setLocalTarFile(Paths.get(tarFileWithExt.getPath()));
                workingTarPhysicalFile.setStatus(PhysicalFileStatusEnum.TO_STORE);
                LOG.info("[STAF] TAR FILE created and ready to store : {}", tarFileWithExt.getPath());
                // Delete curent directory and all associated files
                FileUtils.deleteDirectory(localCurrentTarDirectory.toFile());
            } else {
                LOG.info("[STAF] Current TAR not big enougth to be stored in staf ({}octets < {}octets)", size,
                         stafConfiguration.getMaxTarSize());
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

    private PhysicalTARFile getCurrentTarPhysicalFile(Set<PhysicalTARFile> pTarFiles, String pStafNode)
            throws IOException {
        // If the current TAR file is already present in the given list do not calculate it.
        Optional<PhysicalTARFile> tar = pTarFiles.stream()
                .filter(t -> PhysicalFileStatusEnum.PENDING.equals(t.getStatus())).findFirst();

        if (!tar.isPresent()) {
            // The current TAR is not already calculated.
            // Create the new TarPhysicalFile
            // Get already present files in the current tar folder
            Set<Path> filesInTar = Sets.newHashSet();
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(getCurrentTarPath(pStafNode),
                                                                         path -> path.toFile().isFile())) {
                stream.forEach(filesInTar::add);
            }
            PhysicalTARFile newTar = new PhysicalTARFile(pStafNode, filesInTar);
            // Add working tar file to the tar list.
            pTarFiles.add(newTar);
            return newTar;
        } else {
            return tar.get();
        }
    }

    private Path getCurrentTarPath(String pStafNode) {
        return Paths.get(workspaceDirectory.toString(), TAR_DIRECTORY, pStafNode, TAR_CURRENT_DIRECTORY);
    }

}
