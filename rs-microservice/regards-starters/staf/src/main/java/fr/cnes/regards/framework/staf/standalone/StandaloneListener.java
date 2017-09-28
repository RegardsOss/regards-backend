/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.staf.standalone;

import java.net.URL;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.cnes.regards.framework.staf.event.IClientCollectListener;

/**
 * Listener to log each file successfully restored and each file in error.
 * @author Sébastien Binda
 */
public class StandaloneListener implements IClientCollectListener {

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(StandaloneListener.class);

    @Override
    public void fileRetreived(URL pSTAFFileUrl, Path pLocalFilePathRetrieved) {
        LOG.info(" --> File {} retrieved into {}", pSTAFFileUrl, pLocalFilePathRetrieved);

    }

    @Override
    public void fileRetrieveError(URL pSTAFFileUrl, String pErrorMessage) {
        LOG.info(" --> Error retrieving file: {}. Cause : ", pSTAFFileUrl, pErrorMessage);
    }

}
