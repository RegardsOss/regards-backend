/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.staf;

import java.util.Stack;

import org.apache.log4j.Logger;
import org.apache.log4j.NDC;

import fr.cnes.regards.framework.staf.domain.STAFConfiguration;
import fr.cnes.regards.framework.staf.exception.STAFException;

/**
 * <p>Cette classe abstraite represente une session de communication avec le STAF qui peut
 * etre lancee en arriere plan. Cette commande, necessairement non interactive
 * ne peut etre utilisee que pour restituer ou archiver des fichiers.</p>
 * <p>A l'issue de la session en arriere plan il est possible de verifier si
 * elle s'est terminee de maniere nominale ou en erreur.</p>
 *
 */
public abstract class AbstractSTAFBackgroundSession extends Thread {

    /**
     * La variable utilisee pour tracer des erreurs.
     */
    private static Logger logger = Logger.getLogger(AbstractSTAFBackgroundSession.class);

    /**
     * Nom du projet permettant l'ouverture de la session
     */
    protected String project;

    /**
     * Mot de passe permettant l'ouverture de la session
     */
    protected String password;

    /**
     * Session STAF manipulee en arriere plan
     */
    protected STAFSession session;

    /**
     * Session ID
     */
    protected Integer sessionId;

    /**
     * Repertoire du STAF dans lequel les fichiers seront archives
     */
    protected String directory;

    /**
     * pile heritee de la session principale du STAF pour logger un contexte de la commande
     */
    protected Stack parentStack;

    /**
     * Permet de memoriser une eventuelle erreur de fonctionnement de la session
     */
    protected STAFException error;

    protected STAFConfiguration configuration;

    /**
     * Constructeur
     * @param pSessionId l'identifiatn de session.
     * @param pProject Nom du projet STAF contenant les fichiers a restituer/archiver
     * @param pPassword Mot de passe STAF permettant de se connecter
     * @param pGFAccount Indique si le projet STAF est un GF (Gros Fichiers)
     * @param pDirectory Repertoire du STAF dans lequel les fichiers seront archives
     */
    public AbstractSTAFBackgroundSession(Integer pSessionId, String pProject, String pPassword, String pDirectory,
            STAFConfiguration pConfiguration) {
        session = new STAFSession(pConfiguration);
        sessionId = pSessionId;
        project = pProject;
        password = pPassword;
        directory = pDirectory;
        error = null;
    }

    /**
     * Lancement de la session et execution de la restitution ou l archivage
     */
    @Override
    public void run() {
        try {
            try {
                // Initialement aucune erreur
                NDC.inherit(parentStack);
                NDC.push("Session " + sessionId);
                error = null;
                // Lance la session
                session.stafconOpen(project, password);
                // Restitue ou archive les fichiers de maniere bufferisee (par flots)
                doProcess();
            } catch (STAFException e) {
                // Memorise l'erreur
                logger.error(e);
                error = e;
            } finally {
                // Essaie d'arreter la session dans tous les cas
                session.stafconClose();
            }
            // Que la restitution soit reussie ou non, on libere la ressource occupee
            // par la session.
            freeReservation();
        } catch (STAFException e) {
            // Memorise l'erreur
            logger.error(e);
            error = e;
        } finally {
            NDC.remove();
        }
    }

    /**
     * Cette methode permet de verifier si une erreur a ete rencontree lors du
     * deroulement de la session.
     * @return Indique si la session s'est terminee en erreur
     */
    public boolean isInError() {
        return error != null;
    }

    /**
     *
     * @param pParentStack
     */
    public void setParentStack(Stack pParentStack) {
        parentStack = pParentStack;
    }

    /**
     * Execution de la restitution ou l archivage
     *
     * @throws STAFException
     */
    public abstract void doProcess() throws STAFException;

    /**
     * Liberation de la ressource occupee par la session
     *
     * @throws STAFException
     */
    public abstract void freeReservation() throws STAFException;
}
