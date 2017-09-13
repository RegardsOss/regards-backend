/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.staf;

import java.util.HashMap;
import java.util.Map;

/**
 * Cette classe represente un flots de fichiers a archiver avec la classe de service correspondante.
 * @author CS
 */

public class STAFArchivingFlow {

    /**
     * map des fichiers a archiver {String->String} source -> destination
     */
    private final Map<String, String> filesMap;

    /**
     * classe de service a utiliser (cs1,cs3,cs5)
     */
    private String serviceClass;

    /**
     * constructeur, initialise la map des fichiers a archiver
     */
    public STAFArchivingFlow() {
        filesMap = new HashMap<>();
    }

    /**
     * permet d'ajouter un couple source, destination dans la map
     * @param pSource
     * @param pDestination
     */
    public void addFileToFlow(String pSource, String pDestination) {
        filesMap.put(pSource, pDestination);
    }

    public Map<String, String> getFilesMap() {
        return filesMap;
    }

    public String getServiceClass() {
        return serviceClass;
    }

    public void setServiceClass(String pServiceClass) {
        serviceClass = pServiceClass;
    }
}
