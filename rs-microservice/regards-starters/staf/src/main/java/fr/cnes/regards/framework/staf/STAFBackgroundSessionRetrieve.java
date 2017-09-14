/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.staf;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Sets;

import fr.cnes.regards.framework.staf.domain.ArchiveAccessModeEnum;
import fr.cnes.regards.framework.staf.domain.STAFConfiguration;
import fr.cnes.regards.framework.staf.event.CollectEvent;
import fr.cnes.regards.framework.staf.event.ICollectListener;
import fr.cnes.regards.framework.staf.exception.STAFException;

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
 */
public class STAFBackgroundSessionRetrieve extends AbstractSTAFBackgroundSession {

    /**
     * Ensemble des fichiers a restituer ou a archiver
     */
    protected Map<String, String> files;

    /**
     * indique le listener a notifier en fin de traitement de la session
     */
    protected ICollectListener listener;

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
     */
    public STAFBackgroundSessionRetrieve(Integer pSessionId, String pProject, String pPassword,
            Map<String, String> pFiles, STAFConfiguration pConfiguration, ICollectListener pListener) {
        // Call super
        super(pSessionId, pProject, pPassword, null, pConfiguration);
        listener = pListener;
        // Files to retrieve
        files = pFiles;
    }

    /**
     * Methode surchargee
     *
     * Restitue les fichiers de maniere bufferisee (par flots)
     */
    @Override
    public void doProcess() throws STAFException {
        // Retrieve files by bufferisation (by flow)
        try {
            session.staffilRetrieveBuffered(files);
        } finally {
            // Send notification for all files error or succeed.
            if (listener != null) {
                CollectEvent collectEnd = new CollectEvent(this);
                Set<Path> filePaths = Sets.newHashSet();
                final Set<Path> errorFilePaths = Sets.newHashSet();
                for (final String fileName : files.keySet()) {
                    Path restoredFile = Paths.get(files.get(fileName));
                    if (restoredFile.toFile().exists()) {
                        filePaths.add(Paths.get(fileName));
                    } else {
                        errorFilePaths.add(Paths.get(fileName));
                    }
                }
                collectEnd.setRestoredFilePaths(filePaths);
                collectEnd.setNotRestoredFilePaths(errorFilePaths);
                collectEnd.setRestoredFilePaths(filePaths);
                listener.collectEnded(collectEnd);
            }
        }
    }

    /**
     * Methode surchargee
     *
     * Liberation de la ressource occupee par la session
     */
    @Override
    public void freeReservation() throws STAFException {
        // Frees ressource used by the session
        STAFSessionManager.getInstance(configuration).freeReservation(sessionId, ArchiveAccessModeEnum.RESTITUTION_MODE);
    }

}
