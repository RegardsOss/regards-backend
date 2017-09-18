/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.staf.event;

import java.net.URL;
import java.nio.file.Path;

/**
 * Interface for STAF Controller listener during a restoration process.
 * @author Sébastien Binda
 */
public interface IClientCollectListener {

    /**
     * Inform the instanciated client thaht the given {@link URL} file <br/>
     * as been retreived from the STAF System and put into the {@link Path}.
     * @param pSTAFFileUrl {@link URL} of the STAF File retrieved
     * @param pLocalFilePathRetrieved {@link Path} of the accessible file.
     */
    void fileRetreived(URL pSTAFFileUrl, Path pLocalFilePathRetrieved);

    /**
     * Inform the the instanciated client that there was an error during <br/>
     * the given {@link URL} file restoration<br/>
     * @param pSTAFFileUrl {@link URL} of the STAF File in error.
     */
    void fileRetrieveError(URL pSTAFFileUrl);
}
