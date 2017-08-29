package fr.cnes.regards.framework.staf;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

/**
 * Le gestionnaire STAF est une classe qui permet de centraliser les informations communes necessaires au traitement des
 * requetes. Cette classe assure principalement la gestion de la charge imposee au STAF en limitant le nombre de
 * sessions ouvertes en parallele. Cette limitation s'appuie sur un systeme de reservations de ressources.
 */
public class STAFManager {

    private static Logger logger = Logger.getLogger(STAFManager.class);

    /**
     * Instance unique du gestionnaire STAF (design pattern singleton)
     */
    private static STAFManager instance = null;

    /**
     * Configuration du service STAF
     */
    private STAFConfiguration configuration;

    /**
     * Dernier identifiant de session attribue
     */
    private int lastIdentifier;

    /**
     * Liste des reservations en cours
     */
    private List<Integer> reservations;

    private STAFManager(STAFConfiguration pConfiguration) throws STAFException {
        lastIdentifier = 0;
        reservations = new ArrayList<>();
        configuration = pConfiguration;
    }

    private STAFManager() {

    }

    /**
     * Recupere l'instance unique du gestionnaire STAF
     *
     * @return L'instance unique du gestionnaire STAF
     * @throws STAFException
     *             si le manager ne peut pas etre instancie.
     */
    public static synchronized STAFManager getInstance(STAFConfiguration pConfiguration) throws STAFException {
        if (instance == null) {
            instance = new STAFManager(pConfiguration);
        }
        return instance;
    }

    /**
     * Effectue la reservation d'un certain nombre de sessions pour une restitution ou une archive
     *
     * @param pReservationCount
     *            Nombre de reservations demandees
     * @param pBlock
     *            Indique si l'octroi d'au moins une reservation est bloquant
     * @param pMode
     *            mode de restitution ou d archivage (constante de STAFService)
     * @return Un tableau contenant les identifiants de reservation obtenus. En fonction de la disponibilite des
     *         sessions, le nombre de reservations reellement obtenues peut etre inferieur au nombre de reservations
     *         demandees.
     */
    public List<Integer> getReservations(int pReservationCount, boolean pBlock, ArchiveAccessModeEnum pMode) {

        // Reservations obtenues
        List<Integer> currentReservations = new ArrayList<>();
        // Reservation unique
        Integer uniqueReservation = null;

        // La reservation a lieu dans un section critique pour eviter tout
        // conflit en cas de reservation simultanee de plusieurs commandes.
        synchronized (this) {
            if (hasFreeSessions(pMode)) {
                // Il existe des sessions libres. On en attribue un maximum au
                // demandeur
                int reservationIndex = 0;
                while ((reservationIndex < pReservationCount) && hasFreeSessions(pMode)) {
                    currentReservations.add(getReservationIdentifier());
                    reservationIndex++;
                }
            } else if (pBlock) {
                // Une reservation est ajoutee a la liste des reservations en cours
                // mais la main n'est rendue a l'appelant que lorsque la reservation
                // ainsi ajoutee fait partie des N (config) premieres de la liste des
                // reservations en cours (l'ordre des reservations temoigne de leur
                // ordre d'arrivee).
                uniqueReservation = getReservationIdentifier();
            }
        }

        // L'attente eventuelle de l'obtention d'une reservation unique se fait
        // en dehors de la section critique car elle peut durer longtemps. En
        // revanche la verification de l'ordre de la reservation est synchronisee.
        if (uniqueReservation != null) {
            while (!isReservationAuthorized(uniqueReservation, pMode)) {
                try {
                    synchronized (uniqueReservation) {
                        uniqueReservation.wait();
                    }
                } catch (InterruptedException e) {
                    // Une interruption de l'attente n'est pas une erreur
                }
            }
            // La reservation a ete obtenue
            currentReservations.add(uniqueReservation);
        }
        if (logger.isDebugEnabled()) {
            logger.debug(currentReservations.size() + " session reserved");
        }
        return currentReservations;
    }

    /**
     * Effectue la reservation pour une seule session (appel bloquant) pour une restitution ou une archive
     *
     * @param pMode
     *            mode de restitution ou d archivage (constante de STAFService)
     * @return L'identifiant de la session demandee.
     */
    public Integer getReservation(ArchiveAccessModeEnum pMode) {
        List<Integer> sessions = getReservations(1, true, pMode);
        return sessions.get(0);
    }

    /**
     * Libere une session. Les threads en attente d'une session sont avertis du retrait de la reservation pour la
     * restitution ou pour l archivage
     *
     * @param pReservation
     *            Reservation liberee
     * @param pMode
     *            mode de restitution ou d archivage (constante de STAFService)
     */
    public synchronized void freeReservation(Integer pReservation, ArchiveAccessModeEnum pMode) {
        // Free a reservation
        reservations.remove(pReservation);
        // Restitution mode
        if (pMode == ArchiveAccessModeEnum.RESTITUTION_MODE) {
            // Inform the first thread waiting for a free ressource
            if (reservations.size() >= configuration.getMaxSessionsRestitutionMode().intValue()) {
                int indexNextSession = configuration.getMaxSessionsRestitutionMode().intValue() - 1;
                Integer nextReservation = reservations.get(indexNextSession);
                synchronized (nextReservation) {
                    nextReservation.notify();
                }
            }
        }
        // Archive mode
        else if (pMode == ArchiveAccessModeEnum.ARCHIVE_MODE) {
            // Inform the first thread waiting for a free ressource
            if (reservations.size() >= configuration.getMaxSessionsArchivingMode().intValue()) {
                int indexNextSession = configuration.getMaxSessionsArchivingMode().intValue() - 1;
                Integer nextReservation = reservations.get(indexNextSession);
                synchronized (nextReservation) {
                    nextReservation.notify();
                }
            }
        }
        if (logger.isDebugEnabled()) {
            logger.debug("session number " + pReservation + " has been released");
        }
    }

    /**
     * Permet de savoir s'il existe des sessions disponibles pour une restitution ou un archivage
     *
     * @param pMode
     *            mode de restitution ou d archivage (constante de STAFService)
     * @return Indique s'il existe des sessions disponibles selon le mode
     * @since 4.1
     * @DM SIPNG-DM-0044-CN : changement de signature de methode (parametre supplementaire pour determiner si c est pour
     *     une restitution ou un archivage)
     */
    private boolean hasFreeSessions(ArchiveAccessModeEnum pMode) {
        boolean freeSession = false;
        // Restitution mode
        if (pMode == ArchiveAccessModeEnum.RESTITUTION_MODE) {
            freeSession = configuration.getMaxSessionsRestitutionMode().intValue() > reservations.size();
        }
        // Archivage mode
        else if (pMode == ArchiveAccessModeEnum.ARCHIVE_MODE) {
            freeSession = configuration.getMaxSessionsArchivingMode().intValue() > reservations.size();
        }
        return freeSession;
    }

    /**
     * Obtient un nouvel identificateur de reservation. Cet identificateur est stocke dans la table des reservations en
     * cours.
     *
     * @return Le nouvel identificateur
     * @since 2.0
     */
    private Integer getReservationIdentifier() {

        // Calcule le nouvel identifiant
        Integer identifier = null;
        lastIdentifier++;
        identifier = new Integer(lastIdentifier);

        // Stocke le nouvel identifiant
        reservations.add(identifier);

        return identifier;
    }

    /**
     * Verifie si une reservation autorise l'ouverture d'une session pour une restitution ou un archivage
     *
     * @param pReservation
     *            Reservation a verifier
     * @param pMode
     *            mode de restitution ou d archivage (constante de STAFService)
     * @return vrai si la reservation est autorisee, faux sinon
     * @since 4.1
     * @DM SIPNG-DM-0044-CN : changement de signature de methode (parametre supplementaire pour determiner si c est pour
     *     une restitution ou un archivage)
     */
    private synchronized boolean isReservationAuthorized(Integer pReservation, ArchiveAccessModeEnum pMode) {
        int reservationIndex = reservations.indexOf(pReservation);
        boolean reservationAuthorized = false;
        // Restitution mode
        if (pMode == ArchiveAccessModeEnum.RESTITUTION_MODE) {
            reservationAuthorized = reservationIndex < configuration.getMaxSessionsRestitutionMode().intValue();
        }
        // Archive mode
        if (pMode == ArchiveAccessModeEnum.ARCHIVE_MODE) {
            reservationAuthorized = reservationIndex < configuration.getMaxSessionsArchivingMode().intValue();
        }
        if (logger.isDebugEnabled()) {
            logger.debug(" session reservation is possible : " + reservationAuthorized);
        }
        return reservationAuthorized;
    }

    /**
     * Get method.
     *
     * @return the reservations
     * @since 5.3
     */
    public List<Integer> getReservations() {
        return reservations;
    }

    /**
     * Set method.
     *
     * @param pReservations
     *            the reservations to set
     * @since 5.3
     */
    public void setReservations(List<Integer> pReservations) {
        reservations = pReservations;
    }

    /**
     * Get method.
     *
     * @return the configuration
     * @since 5.3
     */
    public STAFConfiguration getConfiguration() {
        return configuration;
    }

    /**
     * Set method.
     *
     * @param pConfiguration
     *            the configuration to set
     * @since 5.3
     */
    public void setConfiguration(STAFConfiguration pConfiguration) {
        configuration = pConfiguration;
    }

    /**
     * Create a new service to retreive a STAF Access service for the given {@link STAFArchive}
     * @param pStafArchive
     * @return
     */
    public STAFService getNewArchiveAccessService(STAFArchive pStafArchive) {
        return new STAFService(this, pStafArchive);
    }

}
