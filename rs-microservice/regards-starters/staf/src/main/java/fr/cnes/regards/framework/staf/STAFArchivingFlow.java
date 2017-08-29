package fr.cnes.regards.framework.staf;

import java.util.HashMap;
import java.util.Map;

/**
 * Cette classe represente un flots de fichiers a archiver avec la classe de service correspondante.
 *
 * @author CS
 * @version 4.1
 * @since 4.1
 */

public class STAFArchivingFlow {

    /**
     * map des fichiers a archiver {String->String} source -> destination
     *
     * @since 4.1
     */
    private final Map<String, String> filesMap_;

    /**
     * classe de service a utiliser (cs1,cs3,cs5)
     *
     * @since 4.1
     */
    private String serviceClass_;

    /**
     * constructeur, initialise la map des fichiers a archiver
     *
     * @since 4.1
     *
     */
    public STAFArchivingFlow() {
        filesMap_ = new HashMap<>();
    }

    /**
     * permet d'ajouter un couple source, destination dans la map
     *
     * @param pSource
     * @param pDestination
     * @since 4.1
     */
    public void addFileToFlow(String pSource, String pDestination) {
        filesMap_.put(pSource, pDestination);
    }

    // GETTERS AND SETTERS

    public Map<String, String> getFilesMap() {
        return filesMap_;
    }

    public String getServiceClass() {
        return serviceClass_;
    }

    public void setServiceClass(String pServiceClass) {
        serviceClass_ = pServiceClass;
    }
}
