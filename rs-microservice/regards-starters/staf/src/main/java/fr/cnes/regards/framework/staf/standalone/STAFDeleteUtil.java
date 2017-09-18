/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.staf.standalone;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.Set;

import org.assertj.core.util.Sets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import fr.cnes.regards.framework.staf.STAFController;
import fr.cnes.regards.framework.staf.STAFSessionManager;
import fr.cnes.regards.framework.staf.domain.AbstractPhysicalFile;
import fr.cnes.regards.framework.staf.domain.STAFArchive;

/**
 * Component to handle delete process for the standalone STAF Library.
 * @author Sébastien Binda
 */
@Component
public class STAFDeleteUtil {

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(STAFDeleteUtil.class);

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
     * List of STAF URL to delete.
     */
    private final Set<URL> stafURLsToDelete = Sets.newHashSet();

    /**
     * STAF Controller to handle archiving.
     */
    private STAFController controller;

    /**
     * Initialization method to read needed parameters.
     */
    private void readParameters(String[] args) {
        String archiveName = System.getProperty("archive.name", null);
        String archivePassword = System.getProperty("archive.password", null);
        String workspace = System.getProperty("workspace");

        if ((archiveName == null) || (archivePassword == null)) {
            throw new STAFRunException("Invalid archive");
        }

        if ((workspace == null) || !Paths.get(workspace).toFile().exists()) {
            throw new STAFRunException("Invalid workspace directory");
        }

        archive.setArchiveName(archiveName);
        archive.setPassword(archivePassword);

        for (String arg : args) {
            try {
                stafURLsToDelete.add(new URL(arg));
            } catch (MalformedURLException e) {
                throw new STAFRunException(String.format("Invalid STAF URL %s", arg), e);
            }
        }

        try {
            controller = new STAFController(manager.getConfiguration(), Paths.get(workspace),
                    manager.getNewArchiveAccessService(archive));
        } catch (IOException e) {
            throw new STAFRunException(e.getMessage(), e);
        }

        LOG.info(" -- STAF Delete util --");
        LOG.info(" -> STAF Archive : {}", archiveName);
    }

    /**
     * Do delete files from the scanDirectory.
     */
    public void delete(String[] args) {
        readParameters(args);
        Set<AbstractPhysicalFile> preparedFiles = controller.prepareFilesToDelete(stafURLsToDelete);
        Set<URL> deleteFiles = controller.deleteFiles(preparedFiles);
        deleteFiles.forEach(url -> LOG.info(String.format(" --> File %s deleted", url)));
    }

}
