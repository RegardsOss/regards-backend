package fr.cnes.regards.framework.staf;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import fr.cnes.regards.framework.staf.event.CollectEvent;
import fr.cnes.regards.framework.staf.event.CollectEventOffLine;
import fr.cnes.regards.framework.staf.event.ICollectListener;

/**
 * <p>
 * Cette classe represente une session de communication avec le STAF qui peut etre lancee en arriere plan. Cette
 * commande, necessairement non interactive ne peut etre utilisee que pour restituer des fichiers.
 * </p>
 * <p>
 * A l'issue de la session en arriere plan il est possible de verifier si elle s'est terminee de maniere nominale ou en
 * erreur.
 * </p>
 *
 * Cette classe etend AbstractSTAFBackgroundSession.
 *
 * @author CS
 * @version $Revision: 1.7 $
 * @since 4.1
 * @DM SIPNG-DM-0044-CN : renomme STAFBackgroundSession en STAFBackgroundSessionRetrieve
 */
public class STAFBackgroundSessionRetrieve extends AbstractSTAFBackgroundSession {

    /**
     * Ensemble des fichiers a restituer ou a archiver
     *
     * @since 4.1
     */
    protected Map<String, String> files_;

    /**
     * indique le listener a notifier en fin de traitement de la session
     *
     * @since 4.4
     */
    protected ICollectListener listener_;

    /**
     * Constructeur
     *
     * @param pSessionId
     *            l'identifiatn de session.
     * @param pProject
     *            Nom du projet STAF contenant les fichiers a restituer
     * @param pPassword
     *            Mot de passe STAF permettant de se connecter
     * @param pGFAccount
     *            Indique si le projet STAF est un GF (Gros Fichiers)
     * @param pFiles
     *            Ensemble des fichiers a restituer
     * @since 4.1
     */
    public STAFBackgroundSessionRetrieve(Integer pSessionId, String pProject, String pPassword,
            Map<String, String> pFiles, STAFConfiguration pConfiguration, ICollectListener pListener) {
        // Call super
        super(pSessionId, pProject, pPassword, null, pConfiguration);
        listener_ = pListener;
        // Files to retrieve
        files_ = pFiles;
    }

    /**
     * Methode surchargee
     *
     * Restitue les fichiers de maniere bufferisee (par flots)
     *
     * @see sipad.externalSystems.archiving.staf.AbstractSTAFBackgroundSession#doProcess()
     * @since 4.1
     */
    @Override
    public void doProcess() throws STAFException {
        // Retrieve files by bufferisation (by flow)
        session.staffilRetrieveBuffered(files_);
        if (listener_ != null) {
            CollectEvent collectEnd = new CollectEventOffLine(this);
            List<String> fileNames = new ArrayList<>();
            for (String fileName : files_.keySet()) {
                fileNames.add(fileName);
            }
            collectEnd.setFiles(fileNames);
            listener_.collectEnded(collectEnd);
        }
    }

    /**
     * Methode surchargee
     *
     * Liberation de la ressource occupee par la session
     *
     * @see sipad.externalSystems.archiving.staf.AbstractSTAFBackgroundSession#freeReservation()
     * @since 4.1
     */
    @Override
    public void freeReservation() throws STAFException {
        // Frees ressource used by the session
        STAFManager.getInstance(configuration).freeReservation(sessionId, ArchiveAccessModeEnum.RESTITUTION_MODE);
    }

}
