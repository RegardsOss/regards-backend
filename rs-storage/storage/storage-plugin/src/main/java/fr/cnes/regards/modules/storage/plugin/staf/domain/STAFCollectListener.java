/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.storage.plugin.staf.domain;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.SortedSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;

import fr.cnes.regards.framework.file.utils.CutFileUtils;
import fr.cnes.regards.framework.file.utils.compression.CompressionFacade;
import fr.cnes.regards.framework.staf.STAFArchiveModeEnum;
import fr.cnes.regards.framework.staf.STAFException;
import fr.cnes.regards.framework.staf.event.CollectEvent;
import fr.cnes.regards.framework.staf.event.ICollectListener;
import fr.cnes.regards.modules.storage.plugin.staf.domain.protocol.STAFUrlException;
import fr.cnes.regards.modules.storage.plugin.staf.domain.protocol.STAFUrlFactory;

/**
 * STAF Service listener for retreiving process.<br/>
 * This listener handle the link between files retrieved from STAF and files asked during the retrieve process.<br/>
 * <ul>
 * <li> For cut files : 1 file to retrieve = X files to retrieve from STAF.</li>
 * <li> For normal files : 1 file to retrieve = 1 file to retrieve from STAF.</li>
 * <li> For TAR files : X files to retrieve = 1 file to retrieve from STAF</li>
 * </ul>
 *
 * @author Sébastien Binda
 *
 */
public class STAFCollectListener implements ICollectListener {

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
     * @param pClientListener {@link IClientCollectListener} to inform retrieve ended and retrieve error for each file of allFilesToRestore.
     */
    public STAFCollectListener(Set<AbstractPhysicalFile> pAllFilesToRestore, Path pRestorationDirectoryPath,
            IClientCollectListener pClientListener) {
        super();
        allFilesToRestore = pAllFilesToRestore;
        restorationDirectoryPath = pRestorationDirectoryPath;
        clientListener = pClientListener;
    }

    @Override
    public void collectEnded(CollectEvent pEvent) {

        // For each file restored from STAF System
        for (Path stafFilePathRestored : pEvent.getRestoredFilePaths()) {
            Path restoredFilePath = Paths.get(restorationDirectoryPath.toString(),
                                              stafFilePathRestored.getFileName().toString());
            allFilesToRestore.stream().filter(fileToRestore -> {
                try {
                    return stafFilePathRestored.equals(fileToRestore.getSTAFFilePath());
                } catch (STAFException e) {
                    LOG.error("[STAF] Error getting STAF File path", e);
                    return false;
                }
            }).forEach(physicalFile -> handleFileRestored(physicalFile, restoredFilePath));
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
                handTARFileRestored((PhysicalTARFile) pFileRestored);
                break;
            case CUT:
            default:
                // Nothing to do.
                break;
        }
    }

    /**
     * Handle a NORMAL Mode ({@link STAFArchiveModeEnum}) file restoration.<br/>
     * Directly inform the {@link IClientCollectListener} thaht the asked file is available.
     * @param pNormalFileRestore {@link PhysicalNormalFile} file restored.
     */
    private void handleNormaleFileRestored(PhysicalNormalFile pNormalFileRestore) {
        LOG.info("[SEB] File retrieived {}", pNormalFileRestore.getLocalFilePath());
    }

    /**
     * Handle a CUT Mode ({@link STAFArchiveModeEnum}) file restoration.<br/>
     * Inform the {@link IClientCollectListener} that the asked file is available only when all parts are restored.
     * @param pCutPartFileRestored {@link PhysicalCutPartFile} file restored.
     */
    private void handleCutPartFileRestored(PhysicalCutPartFile pCutPartFileRestored) {
        // CUT PART -> 1 file retrieve form STAF -> 1/X file retrieved
        // Check if all part are restored
        PhysicalCutFile includingCutFile = pCutPartFileRestored.getIncludingCutFile();
        SortedSet<PhysicalCutPartFile> allCutParts = pCutPartFileRestored.getIncludingCutFile().getCutedFileParts();
        // If all parts are retrieved, reconstruct gobal file
        if (!allCutParts.stream().anyMatch(part -> !PhysicalFileStatusEnum.RETRIEVED.equals(part.getStatus()))) {
            SortedSet<Path> partFiles = Sets.newTreeSet();
            allCutParts.forEach(f -> partFiles.add(f.getLocalFilePath()));
            try {
                CutFileUtils.rebuildCutedfile(includingCutFile.getLocalFilePath(), partFiles);
                includingCutFile.setStatus(PhysicalFileStatusEnum.RETRIEVED);
                // Inform client that the asked file is available
                URL url = STAFUrlFactory.getCutFileSTAFUrl(includingCutFile);
                Path path = includingCutFile.getLocalFilePath();
                clientListener.fileRetreived(url, path);
            } catch (IOException | STAFUrlException e) {
                LOG.error(e.getMessage(), e);
                includingCutFile.setStatus(PhysicalFileStatusEnum.ERROR);
            }
        }
    }

    /**
     * Handle a TAR Mode ({@link STAFArchiveModeEnum}) file restoration.<br/>
     * Directly inform the {@link IClientCollectListener} all asked files in the restored TAR are availables.
     * @param pTARFileRestored {@link PhysicalTARFile} file restored.
     */
    private void handTARFileRestored(PhysicalTARFile pTARFileRestored) {
        LOG.info("[SEB] File retrieived {}", pTARFileRestored.getLocalFilePath());
        pTARFileRestored.getFilesInTar()
                .forEach((tf, raw) -> LOG.info("[SEB] File retrieived in tar {}", tf.toString()));
        // TODO Extract files
        CompressionFacade facade = new CompressionFacade();
        // facade.decompress(CompressionTypeEnum.TAR, pTARFileRestored.getLocalFilePath().toFile(), pOutputDirectory);
    }

}
