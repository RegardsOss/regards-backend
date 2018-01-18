/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.staf.standalone;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.collect.Sets;
import fr.cnes.regards.framework.staf.STAFController;
import fr.cnes.regards.framework.staf.STAFSessionManager;
import fr.cnes.regards.framework.staf.domain.AbstractPhysicalFile;
import fr.cnes.regards.framework.staf.domain.STAFArchive;

/**
 * Component to handle retrieve process for the standalone STAF Library.
 * @author Sébastien Binda
 */
@Component
public class STAFRetrieveUtil {

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(STAFRetrieveUtil.class);

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
     * Directory where to restore files from STAF System
     */
    private Path restoreDirectory;

    /**
     * List of STAF URL to restore.
     */
    private final Set<URL> stafURLsToRestore = Sets.newHashSet();

    /**
     * STAF Controller to handle restoration.
     */
    private STAFController controller;

    /**
     * Initialization method to read needed parameters.
     */
    private void readParameters(String[] args) {
        String archiveName = System.getProperty("archive.name", null);
        String archivePassword = System.getProperty("archive.password", null);
        String restoreDir = System.getProperty("outputdir", null);
        String workspace = System.getProperty("workspace");

        if ((archiveName == null) || (archivePassword == null)) {
            throw new STAFRunException("Invalid archive");
        }

        if ((restoreDir == null) || !Paths.get(restoreDir).toFile().exists()) {
            throw new STAFRunException("Invalid restoration directory");
        }

        if ((workspace == null) || !Paths.get(workspace).toFile().exists()) {
            throw new STAFRunException("Invalid workspace directory");
        }

        archive.setArchiveName(archiveName);
        archive.setPassword(archivePassword);
        restoreDirectory = Paths.get(restoreDir);

        for (String arg : args) {
            try {
                stafURLsToRestore.add(new URL(arg));
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

        LOG.info(" -- STAF Retrieve util --");
        LOG.info(" -> STAF Archive : {}", archiveName);
        LOG.info(" -> Restore directory : {}", restoreDirectory);
    }

    /**
     * Do retrieve files from STAF System
     * @param args
     */
    public void retrieve(String[] args) {
        readParameters(args);
        if (!stafURLsToRestore.isEmpty()) {
            Set<AbstractPhysicalFile> preparedFiles = controller.prepareFilesToRestore(stafURLsToRestore);
            controller.restoreFiles(preparedFiles, restoreDirectory, new StandaloneListener());
        }
    }

}
