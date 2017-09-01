package fr.cnes.regards.modules.storage.plugin.staf.domain;

import java.io.File;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.Set;
import java.util.Vector;

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

        // 1. Create new current TAR directory if doesnt exists
        if (!localCurrentTarDirectory.toFile().exists()) {
            // No TAR waiting to be archive so create a new one
            Files.createDirectories(localCurrentTarDirectory);
        }

        // 2. Get lock on directory to avoid an other process to add file into.
        try (FileChannel fileChannel = FileChannel.open(localCurrentTarDirectory)) {
            LOG.debug(" Getting lock for directory {}", localCurrentTarDirectory.toString());
            FileLock lock = fileChannel.lock();
            LOG.debug(" Directory {} locked", localCurrentTarDirectory.toString());

            // 3. Get the current creating tar file
            PhysicalTARFile workingTarPhysicalFile = getCurrentTarPhysicalFile(pTarFiles, pStafNode);

            // 4. Move file in the tar directory
            Path sourceFile = pPhysicalFileToArchive;
            Path destinationFile = Paths.get(localCurrentTarDirectory.toString(),
                                             pPhysicalFileToArchive.getFileName().toString());
            Files.move(sourceFile, destinationFile);
            workingTarPhysicalFile.getLocalFiles().add(destinationFile);

            // 5. Check TAR size
            long size = Files.walk(localTarDirectory).mapToLong(p -> p.toFile().length()).sum();
            if (size > stafConfiguration.getMaxTarSize()) {
                // No other files can be added in tar.
                // Create TAR
                String tarName = LocalDateTime.now().format(DateTimeFormatter.ofPattern(TAR_FILE_NAME_DATA_FORMAT));
                CompressionFacade facade = new CompressionFacade();
                Vector<CompressManager> manager = facade
                        .compress(CompressionTypeEnum.TAR, localCurrentTarDirectory.toFile(), null,
                                  Paths.get(localTarDirectory.toString(), tarName).toFile(),
                                  localCurrentTarDirectory.toFile(), true, false);
                // Set the TAR local path
                File tarFile = manager.firstElement().getCompressedFile();
                workingTarPhysicalFile.setLocalTarFile(Paths.get(tarFile.getPath()));
                // TODO : When to delete TAR File ?
            }

            LOG.debug(" Releasing lock for directory {}", localCurrentTarDirectory.toString());
            lock.release();
            LOG.debug(" Lock released for directory {}", localCurrentTarDirectory.toString());
        } catch (IOException | CompressionException e) {
            // Error adding file to tar
            throw new STAFTarException(e);
        }

    }

    private PhysicalTARFile getCurrentTarPhysicalFile(Set<PhysicalTARFile> pTarFiles, String pStafNode)
            throws IOException {
        // If the current TAR file is already present in the given list do not calculate it.
        Optional<PhysicalTARFile> tar = pTarFiles.stream().filter(t -> !t.isToStore()).findFirst();

        if (!tar.isPresent()) {
            // The current TAR is not already calculated.
            // Create the new TarPhysicalFile
            // Get already present files in the current tar folder
            Set<Path> filesInTar = Sets.newHashSet();
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(getCurrentTarPath(pStafNode),
                                                                         path -> path.toFile().isFile())) {
                stream.forEach(filesInTar::add);
            }
            PhysicalTARFile newTar = new PhysicalTARFile(pStafNode, filesInTar, null, null);
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
