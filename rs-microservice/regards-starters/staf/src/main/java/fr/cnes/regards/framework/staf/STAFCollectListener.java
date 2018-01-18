/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.staf;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;
import fr.cnes.regards.framework.staf.domain.AbstractPhysicalFile;
import fr.cnes.regards.framework.staf.domain.PhysicalCutFile;
import fr.cnes.regards.framework.staf.domain.PhysicalCutPartFile;
import fr.cnes.regards.framework.staf.domain.PhysicalFileStatusEnum;
import fr.cnes.regards.framework.staf.domain.PhysicalNormalFile;
import fr.cnes.regards.framework.staf.domain.PhysicalTARFile;
import fr.cnes.regards.framework.staf.domain.STAFArchiveModeEnum;
import fr.cnes.regards.framework.staf.event.CollectEvent;
import fr.cnes.regards.framework.staf.event.IClientCollectListener;
import fr.cnes.regards.framework.staf.protocol.STAFURLException;
import fr.cnes.regards.framework.staf.protocol.STAFURLFactory;
import fr.cnes.regards.framework.utils.file.CommonFileUtils;
import fr.cnes.regards.framework.utils.file.CutFileUtils;
import fr.cnes.regards.framework.utils.file.compression.CompressionException;
import fr.cnes.regards.framework.utils.file.compression.CompressionFacade;
import fr.cnes.regards.framework.utils.file.compression.CompressionTypeEnum;

/**
 * STAF Service listener for retreiving process.<br/>
 * This listener handle the link between files retrieved from STAF and files asked during the retrieve process.<br/>
 * <ul>
 * <li>For cut files : 1 file to retrieve = X files to retrieve from STAF.</li>
 * <li>For normal files : 1 file to retrieve = 1 file to retrieve from STAF.</li>
 * <li>For TAR files : X files to retrieve = 1 file to retrieve from STAF</li>
 * </ul>
 *
 * @author Sébastien Binda
 *
 */
public class STAFCollectListener {

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(STAFController.class);

    /**
     * {@link AbstractPhysicalFile}s to restore.
     */
    private final Set<AbstractPhysicalFile> allFilesToRestore;

    /**
     * {@link Path} of the restoration directory
     */
    private final Path restorationDirectoryPath;

    /**
     * {@link IClientCollectListener} to inform retrieve ended and retrieve error for each file of allFilesToRestore.
     */
    private final IClientCollectListener clientListener;

    /**
     * Constructor
     * @param pAllFilesToRestore {@link Set}<{@link AbstractPhysicalFile}> All files to restore from STAF.
     * @param pRestorationDirectoryPath {@link Path} of the restoration directory.
     * @param pClientListener {@link IClientCollectListener} to inform retrieve ended and retrieve error for each file
     *            of allFilesToRestore.
     */
    public STAFCollectListener(Set<AbstractPhysicalFile> pAllFilesToRestore, Path pRestorationDirectoryPath,
            IClientCollectListener pClientListener) {
        super();
        allFilesToRestore = pAllFilesToRestore;
        restorationDirectoryPath = pRestorationDirectoryPath;
        clientListener = pClientListener;
    }

    public void collectEnded(CollectEvent pEvent) {

        // For each file restored from STAF System
        for (Path stafFilePathRestored : pEvent.getRestoredFilePaths()) {
            Path restoredFilePath = Paths.get(restorationDirectoryPath.toString(),
                                              stafFilePathRestored.getFileName().toString());
            allFilesToRestore.stream()
                    .filter(fileToRestore -> stafFilePathRestored.equals(fileToRestore.getSTAFFilePath()))
                    .forEach(physicalFile -> handleFileRestored(physicalFile, restoredFilePath));
        }

        for (Path stafFilePathNotRestored : pEvent.getNotRestoredFilePaths()) {
            allFilesToRestore.stream()
                    .filter(fileToRestore -> stafFilePathNotRestored.equals(fileToRestore.getSTAFFilePath()))
                    .forEach(this::handleRestorError);
        }
    }

    /**
     * Handle restoration success for files in a TAR locally stored. (waiting to be store in STAF System).
     * @param pRemoteSTAFTARPath {@link Path} of the STAF remote TAR file asked for restoration.
     */
    public void localTARCollectSucceed(Path pRemoteSTAFTARPath) {
        // Find the PhysicalTarFile to restore associated to the given remote STAF Path
        allFilesToRestore.stream()
                .filter(fileToRestore -> STAFArchiveModeEnum.TAR.equals(fileToRestore.getArchiveMode()))
                .filter(fileToRestore -> pRemoteSTAFTARPath.equals(fileToRestore.getSTAFFilePath()))
                .forEach(physicalFile -> handleTarInFileLocallyRestored((PhysicalTARFile) physicalFile));
    }

    /**
     * Handle an error during file retrieve to notify listener.
     * @param pFileNotRestored {@link AbstractPhysicalFile} not successfully restored from STAF System.
     */
    private void handleRestorError(AbstractPhysicalFile pFileNotRestored) {
        // Set error status to the given file.
        pFileNotRestored.setStatus(PhysicalFileStatusEnum.ERROR);
        try {
            // Special case for CUT PART files.
            // the client waits for an error event on the orginial asked file so the including full file.
            if (STAFArchiveModeEnum.CUT_PART.equals(pFileNotRestored.getArchiveMode())) {
                PhysicalCutPartFile partNotRestored = (PhysicalCutPartFile) pFileNotRestored;
                // If the including cut file is already in error status, so the event has already be sent.
                if (!PhysicalFileStatusEnum.ERROR.equals(partNotRestored.getIncludingCutFile().getStatus())) {
                    partNotRestored.getIncludingCutFile().setStatus(PhysicalFileStatusEnum.ERROR);
                    sendFileRetrieveErrorNotification(STAFURLFactory
                            .getCutFileSTAFUrl(partNotRestored.getIncludingCutFile()),
                                                      "[STAF] File is in ERROR status.");
                }
            } else {
                // For other case TAR and NORMAL notify listener for the files in error.
                STAFURLFactory.getSTAFURLs(pFileNotRestored).forEach(f -> sendFileRetrieveErrorNotification(f, String
                        .format("[STAF] File %s not restored.", f.toString())));
            }
        } catch (STAFURLException e) {
            LOG.error("[STAF] Invalid file to handle restore error", e);
        }
    }

    /**
     * Handle events sent by the STAF Service to inform that a file is restored from STAF.
     * @param pFileRestored {@link AbstractPhysicalFile} File restored.
     * @param restoredFilePath {@link Path} of the restored file.
     */
    private void handleFileRestored(AbstractPhysicalFile pFileRestored, Path restoredFilePath) {
        // Update the local physical file path of the restored file.
        pFileRestored.setLocalFilePath(restoredFilePath);
        pFileRestored.setStatus(PhysicalFileStatusEnum.RETRIEVED);
        switch (pFileRestored.getArchiveMode()) {
            case CUT_PART:
                handleCutPartFileRestored((PhysicalCutPartFile) pFileRestored);
                break;
            case NORMAL:
                handleNormaleFileRestored((PhysicalNormalFile) pFileRestored);
                break;
            case TAR:
                handleTARFileRestored((PhysicalTARFile) pFileRestored);
                break;
            case CUT:
            default:
                // Nothing to do.
                break;
        }
    }

    /**
     * Handle a NORMAL Mode ({@link STAFArchiveModeEnum}) file restoration.<br/>
     * DNotify the {@link IClientCollectListener} that the asked file is available.
     * @param pNormalFileRestore {@link PhysicalNormalFile} file restored.
     */
    private void handleNormaleFileRestored(PhysicalNormalFile pNormalFileRestore) {
        try {
            // Notify client that the asked file is available
            URL url = STAFURLFactory.getNormalFileSTAFUrl(pNormalFileRestore);
            sendFileRetrievedNotification(url, pNormalFileRestore.getLocalFilePath());
        } catch (STAFURLException e) {
            LOG.error("[STAF] Invalid file restored", e);
        }
    }

    /**
     * Handle a CUT Mode ({@link STAFArchiveModeEnum}) file restoration.<br/>
     * Notify the {@link IClientCollectListener} that the asked file is available only when all parts are restored.
     * @param pCutPartFileRestored {@link PhysicalCutPartFile} file restored.
     */
    private void handleCutPartFileRestored(PhysicalCutPartFile pCutPartFileRestored) {
        // CUT PART -> 1 file retrieve form STAF -> 1/X file retrieved
        // Check if all part are restored
        PhysicalCutFile includingCutFile = pCutPartFileRestored.getIncludingCutFile();
        SortedSet<PhysicalCutPartFile> allCutParts = pCutPartFileRestored.getIncludingCutFile().getCutedFileParts();

        // If the including cuted full file is in ERROR status, so do not handle other retrieved files.
        if (PhysicalFileStatusEnum.ERROR.equals(includingCutFile.getStatus())) {
            deleteAllCutPartFilesRetrieved(includingCutFile);
        }

        // If all parts are retrieved, reconstruct gobal file
        if (!allCutParts.stream().anyMatch(part -> !PhysicalFileStatusEnum.RETRIEVED.equals(part.getStatus()))) {
            SortedSet<Path> partFiles = Sets.newTreeSet();
            allCutParts.forEach(f -> partFiles.add(f.getLocalFilePath()));
            try {
                URL url = STAFURLFactory.getCutFileSTAFUrl(includingCutFile);
                try {
                    Path fullFile = Paths.get(pCutPartFileRestored.getLocalFilePath().getParent().toString(),
                                              includingCutFile.getStafFileName());
                    CutFileUtils.rebuildCutedfile(fullFile, partFiles);
                    includingCutFile.setStatus(PhysicalFileStatusEnum.RETRIEVED);
                    // Notify client that the asked file is available
                    sendFileRetrievedNotification(url, fullFile);
                } catch (IOException e) {
                    LOG.error(e.getMessage(), e);
                    includingCutFile.setStatus(PhysicalFileStatusEnum.ERROR);
                    sendFileRetrieveErrorNotification(url, e.getMessage());
                }
            } catch (STAFURLException e1) {
                LOG.error("[STAF] Invalid file restored", e1);
            } finally {
                // Delete all part files
                deleteAllCutPartFilesRetrieved(includingCutFile);
            }
        }
    }

    /**
     * Handle a TAR Mode ({@link STAFArchiveModeEnum}) file restoration.<br/>
     * Directly Notify the {@link IClientCollectListener} for all asked files in the restored TAR are availables.
     * @param pTARFileRestored {@link PhysicalTARFile} file restored.
     */
    private void handleTARFileRestored(PhysicalTARFile pTARFileRestored) {
        Path tarExtractionPath = Paths.get(restorationDirectoryPath.toString(),
                                           "." + pTARFileRestored.getLocalFilePath().getFileName().toString());
        try {
            Map<Path, URL> urls = STAFURLFactory.getTARFilesSTAFUrl(pTARFileRestored);
            if (!tarExtractionPath.toFile().exists()) {
                try {
                    // Extract files from TAR
                    Files.createDirectories(tarExtractionPath);
                    CompressionFacade facade = new CompressionFacade();
                    facade.decompress(CompressionTypeEnum.TAR, pTARFileRestored.getLocalFilePath().toFile(),
                                      tarExtractionPath.toFile());
                } catch (CompressionException | IOException e) {
                    String msg = String.format("[STAF] Error during TAR decompression %s.",
                                               pTARFileRestored.getLocalFilePath());
                    LOG.error(msg, e);
                    urls.forEach((localPath, url) -> sendFileRetrieveErrorNotification(url, msg));
                }
            }
            // Check files existance and notify client if files are well restored.
            pTARFileRestored.getFilesInTar().forEach((fileInTarPath,
                    rawPath) -> handleTARInFileRestored(tarExtractionPath, fileInTarPath, urls.get(rawPath), false));
        } catch (STAFURLException e1) {
            LOG.error("[STAF] Invalid file restored", e1);
        } finally {
            // Always delete TAR after all files are restored from it.
            if (!pTARFileRestored.getLocalFilePath().toFile().delete()) {
                LOG.error("[STAF] Error deleting tar file {} restored from STAF",
                          pTARFileRestored.getLocalFilePath().toString());
            }
            // Always delete extraction directory after all files retrieved.
            try {
                if (tarExtractionPath.toFile().exists()) {
                    FileUtils.deleteDirectory(tarExtractionPath.toFile());
                }
            } catch (IOException e) {
                LOG.error("[STAF] Error deleting temporary TAR Extraction directory", e.getMessage(), e);
            }
        }
    }

    /**
     * Handle restoration of each files in TAR for the given locally stored {@link pTarLocallyRestored}
     * @param pTarLocallyRestored {@link PhysicalTARFile} locally stored TAR file from which restore files.
     */
    private void handleTarInFileLocallyRestored(PhysicalTARFile pTarLocallyRestored) {
        if ((pTarLocallyRestored.getLocalTarDirectory() != null)
                && !pTarLocallyRestored.getLocalTarDirectory().toFile().exists()) {
            LOG.error("[STAF] Error trying to restore files from local tar {}. Local tar directory {} does not exists.",
                      pTarLocallyRestored.getLocalFilePath(), pTarLocallyRestored.getLocalTarDirectory());
        }
        pTarLocallyRestored.getFilesInTar().forEach((fileInTarPath, rawPath) -> {
            try {
                Optional<URL> url = STAFURLFactory.getTARFileSTAFUrl(pTarLocallyRestored, fileInTarPath);
                if (url.isPresent()) {
                    handleTARInFileRestored(pTarLocallyRestored.getLocalTarDirectory(), fileInTarPath, url.get(), true);
                }
            } catch (STAFURLException e) {
                LOG.error("[STAF] Invalid file to handle restore error", e);
            }
        });
    }

    /**
     * Handle restoration for one file from a restored TAR File.
     * Send notification to the {@link IClientCollectListener} for the given restored file.
     * @param pTARExtractionPath {@link Path} to the TAR restored extraction directory.
     * @param pFileRestoredFromTAR {@link Path} of the file in TAR restored.
     * @param pFileRestoredSTAFURL origine {@link URL} of the STAF file asked for restoration
     * @param move or copy file from extraction directory to restoration path ?
     */
    private void handleTARInFileRestored(Path pTARExtractionPath, Path pFileRestoredFromTAR, URL pFileRestoredSTAFURL,
            boolean keepOriginalFile) {
        Path extractedfilePath = Paths.get(pTARExtractionPath.toString(),
                                           pFileRestoredFromTAR.getFileName().toString());
        if (extractedfilePath.toFile().exists()) {
            // File found into the tar files.
            try {
                String uniqueFileName = CommonFileUtils
                        .getAvailableFileName(restorationDirectoryPath, extractedfilePath.getFileName().toString());
                Path finalDestinationPath = Paths.get(restorationDirectoryPath.toString(), uniqueFileName);
                if (!finalDestinationPath.toFile().exists()) {
                    // Move or cpy file to final destination
                    if (keepOriginalFile) {
                        Files.copy(extractedfilePath, finalDestinationPath);
                    } else {
                        Files.move(extractedfilePath, finalDestinationPath);
                    }
                    sendFileRetrievedNotification(pFileRestoredSTAFURL, finalDestinationPath);
                } else {
                    String msg = String.format("[STAF]Error restoring file. File %s already exists.",
                                               pFileRestoredSTAFURL);
                    sendFileRetrieveErrorNotification(pFileRestoredSTAFURL, msg);
                }

            } catch (IOException e) {
                String msg = String
                        .format("Error moving file from temporary tar extraction directory %s to restoration directory %s. %s",
                                pTARExtractionPath.toString(), restorationDirectoryPath.toString(), e.getMessage());
                LOG.error(msg, e);
                sendFileRetrieveErrorNotification(pFileRestoredSTAFURL, msg);
            }
        } else {
            String msg = String.format("[STAF] File %s is not found in the %s TAR file restored from STAF System.",
                                       extractedfilePath.getFileName().toString(), pFileRestoredSTAFURL.toString());
            LOG.error(msg);
            sendFileRetrieveErrorNotification(pFileRestoredSTAFURL, msg);
        }
    }

    /**
     * Delete all restored part files from the given {@link PhysicalCutFile}
     * @param pCutFile {@link PhysicalCutFile}
     */
    private void deleteAllCutPartFilesRetrieved(PhysicalCutFile pCutFile) {
        if ((pCutFile != null) && (pCutFile.getCutedFileParts() != null)) {
            pCutFile.getCutedFileParts().stream()
                    .filter(part -> PhysicalFileStatusEnum.RETRIEVED.equals(part.getStatus())).forEach(restoredPart -> {
                        if (restoredPart.getLocalFilePath().toFile().exists()
                                && !restoredPart.getLocalFilePath().toFile().delete()) {
                            LOG.error("[STAF] Error deleting cut file part {}", restoredPart.toString());
                        }
                    });
        }
    }

    private void sendFileRetrievedNotification(URL pSTAFURL, Path pLocalFilePathRestored) {
        LOG.info("[STAF collect-listener] Sending notification for STAF File {} retrieved in {}", pSTAFURL.toString(),
                 pLocalFilePathRestored.toString());
        clientListener.fileRetreived(pSTAFURL, pLocalFilePathRestored);
    }

    private void sendFileRetrieveErrorNotification(URL pSTAFURL, String pErrorMessage) {
        LOG.error("[STAF collect-listener] Sending notification for STAF File {} retrieve error", pSTAFURL.toString());
        clientListener.fileRetrieveError(pSTAFURL, pErrorMessage);
    }

}
