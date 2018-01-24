/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.staf.standalone;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Set;

import org.assertj.core.util.Sets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.collect.Maps;
import fr.cnes.regards.framework.staf.STAFController;
import fr.cnes.regards.framework.staf.STAFSessionManager;
import fr.cnes.regards.framework.staf.domain.AbstractPhysicalFile;
import fr.cnes.regards.framework.staf.domain.STAFArchive;

/**
 * Component to handle archive process for the standalone STAF Library.
 * @author Sébastien Binda
 */
@Component
public class STAFArchiveUtil {

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(STAFArchiveUtil.class);

    /**
     * STAF Manager
     */
    @Autowired
    public STAFSessionManager manager;

    /**
     * STAF Archive configuration
     */
    private final STAFArchive archive = new STAFArchive();

    /**
     * Directory to scan to archive files.
     */
    private Path scanDirPath;

    /**
     * STAF Node where to archive files
     */
    private Path node;

    /**
     * STAF Controller to handle archiving.
     */
    private STAFController controller;

    /**
     * Initialization method to read needed parameters.
     */
    private void readParameters() {
        String archiveName = System.getProperty("archive.name", null);
        String archivePassword = System.getProperty("archive.password", null);
        String scanDir = System.getProperty("dir", null);
        String workspace = System.getProperty("workspace");
        String stafNode = System.getProperty("node");

        if ((archiveName == null) || (archivePassword == null)) {
            throw new STAFRunException("Invalid archive");
        }

        if (stafNode == null) {
            throw new STAFRunException("Invalid STAF Node");
        }

        if ((scanDir == null) || !Paths.get(scanDir).toFile().exists()) {
            throw new STAFRunException("Invalid scan directory");
        }

        if ((workspace == null) || !Paths.get(workspace).toFile().exists()) {
            throw new STAFRunException("Invalid workspace directory");
        }

        archive.setArchiveName(archiveName);
        archive.setPassword(archivePassword);
        scanDirPath = Paths.get(scanDir);
        node = Paths.get(stafNode);

        try {
            controller = new STAFController(manager.getConfiguration(), Paths.get(workspace),
                    manager.getNewArchiveAccessService(archive));
        } catch (IOException e) {
            throw new STAFRunException(e.getMessage(), e);
        }

        LOG.info(" -- STAF Archive util --");
        LOG.info(" -> STAF Archive : {}", archiveName);
        LOG.info(" -> STAF Node : {}", node);
        LOG.info(" -> STAF local workspace : {}", workspace);
        LOG.info(" -> Scan directory : {}", scanDirPath);
    }

    /**
     * Do archive files from the scanDirectory.
     */
    public void archive() {
        readParameters();
        Map<Path, Set<Path>> filesToARchivePerNode = Maps.newHashMap();
        filesToARchivePerNode.put(node, Sets.newHashSet());
        // 1. Scan directories to get files to archive
        try {
            Files.walk(scanDirPath).filter(f -> f.toFile().isFile()).forEach(file -> {
                Set<Path> files = filesToARchivePerNode.get(node);
                files.add(file);
                filesToARchivePerNode.put(node, files);
            });
        } catch (IOException e) {
            throw new STAFRunException(e.getMessage(), e);
        }

        Set<AbstractPhysicalFile> preparedFiles = controller.prepareFilesToArchive(filesToARchivePerNode);
        controller.archiveFiles(preparedFiles, false);
        controller.getRawFilesArchived(preparedFiles).forEach((storedFile, url) -> LOG
                .info(String.format(" --> File %s stored into STAF at %s", storedFile, url)));
    }

}
