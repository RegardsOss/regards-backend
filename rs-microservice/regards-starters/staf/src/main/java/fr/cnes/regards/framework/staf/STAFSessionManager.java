/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.staf;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Logger;

import fr.cnes.regards.framework.staf.domain.ArchiveAccessModeEnum;
import fr.cnes.regards.framework.staf.domain.STAFArchive;
import fr.cnes.regards.framework.staf.domain.STAFConfiguration;
import fr.cnes.regards.framework.staf.exception.STAFException;

/**
 * Le gestionnaire STAF est une classe qui permet de centraliser les informations communes necessaires au traitement des
 * requetes. Cette classe assure principalement la gestion de la charge imposee au STAF en limitant le nombre de
 * sessions ouvertes en parallele. Cette limitation s'appuie sur un systeme de reservations de ressources.
 * @author sbinda CS
 */
public class STAFSessionManager {

    private static Logger logger = Logger.getLogger(STAFSessionManager.class);

    /**
     * Instance unique du gestionnaire STAF (design pattern singleton)
     */
    private static STAFSessionManager instance = null;

    /**
     * Configuration du service STAF
     */
    private STAFConfiguration configuration;

    /**
     * Dernier identifiant de session attribue
     */
    private final AtomicInteger lastIdentifier = new AtomicInteger(0);

    /**
     * Liste des reservations en cours
     */
    private final List<Integer> reservations = Collections.synchronizedList(new ArrayList<>());

    private Map<ArchiveAccessModeEnum, Semaphore> semaphoreMap = Collections
            .synchronizedMap(new EnumMap<ArchiveAccessModeEnum, Semaphore>(ArchiveAccessModeEnum.class));

    private STAFSessionManager(STAFConfiguration configuration) {
        this.configuration = configuration;
        this.semaphoreMap
                .put(ArchiveAccessModeEnum.ARCHIVE_MODE, new Semaphore(configuration.getMaxSessionsArchivingMode()));
        this.semaphoreMap.put(ArchiveAccessModeEnum.RESTITUTION_MODE,
                              new Semaphore(configuration.getMaxSessionsRestitutionMode()));
    }

    public STAFSession getNewSession() {
        return new STAFSession(configuration);
    }

    /**
     * Recupere l'instance unique du gestionnaire STAF
     * @return L'instance unique du gestionnaire STAF
     * @throws STAFException si le manager ne peut pas etre instancie.
     */
    public static STAFSessionManager getInstance(STAFConfiguration configuration) {
        if (instance == null) {
            instance = new STAFSessionManager(configuration);
        }
        return instance;
    }

    /**
     * Effectue la reservation d'un certain nombre de sessions pour une restitution ou une archive
     * @param reservationCount Nombre de reservations demandees
     * @param block Indique si l'octroi d'au moins une reservation est bloquant
     * @param mode mode de restitution ou d archivage (constante de STAFService)
     * @return Un tableau contenant les identifiants de reservation obtenus. En fonction de la disponibilite des
     * sessions, le nombre de reservations reellement obtenues peut etre inferieur au nombre de reservations
     * demandees.
     */
    public List<Integer> getReservations(int reservationCount, boolean block, ArchiveAccessModeEnum mode) {

        Semaphore semaphore = this.semaphoreMap.get(mode);

        // Reservations obtenues
        List<Integer> currentReservations = new ArrayList<>();
        // Reservation unique
        int uniqueReservation = -1;

        // La reservation a lieu dans un section critique pour eviter tout
        // conflit en cas de reservation simultanee de plusieurs commandes.
        //        synchronized (this) {
        if (reservationCount > 1) {
            //    if (hasFreeSessions(mode)) {
            // Il existe des sessions libres. On en attribue un maximum au
            // demandeur
            //                int reservationIndex = 0;
            //                while ((reservationIndex < reservationCount) && hasFreeSessions(mode)) {
            //                    currentReservations.add(getReservationIdentifier());
            //                    reservationIndex++;
            //                }
            int tokenCount = Math.min(reservationCount, semaphore.availablePermits());
            for (int i = 0; i < tokenCount; i++) {
                try {
                    semaphore.acquire();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e); // NOSONAR
                }
                currentReservations.add(getReservationIdentifier());
            }
        } else if (block) {
            // Une reservation est ajoutee a la liste des reservations en cours
            // mais la main n'est rendue a l'appelant que lorsque la reservation
            // ainsi ajoutee fait partie des N (config) premieres de la liste des
            // reservations en cours (l'ordre des reservations temoigne de leur
            // ordre d'arrivee).
            try {
                semaphore.acquire();
            } catch (InterruptedException e) {
                throw new RuntimeException(e); // NOSONAR
            }
            currentReservations.add(getReservationIdentifier());
        } else { // non bloquant
            if (semaphore.tryAcquire()) {
                currentReservations.add(getReservationIdentifier());
            }
        }
        //        }

        // L'attente eventuelle de l'obtention d'une reservation unique se fait
        // en dehors de la section critique car elle peut durer longtemps. En
        // revanche la verification de l'ordre de la reservation est synchronisee.
/*        if (uniqueReservation != -1) {
            while (!isReservationAuthorized(uniqueReservation, mode)) {
//                try {
//                    // Ici il y avait un synchronized avec un wait sur le uniqueReservation (de type Integer)
//                    // TODO semaphore
//                    Thread.sleep(1000l);
//                } catch (InterruptedException e) {
//                    // Une interruption de l'attente n'est pas une erreur
//                }
                try {
                    switch (mode) {
                        case ARCHIVE_MODE:
                            archivageSemaphore.acquire();
                            break;
                        case RESTITUTION_MODE:
                            restitutionSemaphore.acquire();
                            break;
                    }
                } catch (InterruptedException e) {
                    // Une interruption de l'attente n'est pas une erreur
                }
            }
            // La reservation a ete obtenue
            currentReservations.add(uniqueReservation);
        }*/
        if (logger.isDebugEnabled()) {
            logger.debug(currentReservations.size() + " session reserved");
        }
        return currentReservations;
    }

    /**
     * Effectue la reservation pour une seule session (appel bloquant) pour une restitution ou une archive
     * @param mode mode de restitution ou d archivage (constante de STAFService)
     * @return L'identifiant de la session demandee.
     */
    public Integer getReservation(ArchiveAccessModeEnum mode) {
        List<Integer> sessions = getReservations(1, true, mode);
        return sessions.get(0);
    }

    /**
     * Libere une session. Les threads en attente d'une session sont avertis du retrait de la reservation pour la
     * restitution ou pour l archivage
     * @param reservation Reservation liberee
     * @param mode mode de restitution ou d archivage (constante de STAFService)
     */
    public void freeReservation(Integer reservation, ArchiveAccessModeEnum mode) {
        Semaphore semaphore = this.semaphoreMap.get(mode);
        semaphore.release();
        // Free a reservation
        reservations.remove(reservation);
        // Restitution mode
/*        if (mode == ArchiveAccessModeEnum.RESTITUTION_MODE) {
            // Inform the first thread waiting for a free ressource
            if (reservations.size() >= configuration.getMaxSessionsRestitutionMode().intValue()) {
                int indexNextSession = configuration.getMaxSessionsRestitutionMode().intValue() - 1;
                Integer nextReservation = reservations.get(indexNextSession);
//                // TODO REMPLACER PAR UN SEmaphore !!!
//                synchronized (nextReservation) {
//                    nextReservation.notify();
//                }
                restitutionSemaphore.release();
            }
        }
        // Archive mode
        else if (mode == ArchiveAccessModeEnum.ARCHIVE_MODE) {
            // Inform the first thread waiting for a free ressource
            if (reservations.size() >= configuration.getMaxSessionsArchivingMode().intValue()) {
                int indexNextSession = configuration.getMaxSessionsArchivingMode().intValue() - 1;
                Integer nextReservation = reservations.get(indexNextSession);
//                // TODO REMPLACER PAR UN SEmaphore !!!
//                synchronized (nextReservation) {
//                    nextReservation.notify();
//                }
                archivageSemaphore.release();
            }
        }*/
        if (logger.isDebugEnabled()) {
            logger.debug("session number " + reservation + " has been released");
        }
    }

    /**
     * Permet de savoir s'il existe des sessions disponibles pour une restitution ou un archivage
     * @param mode mode de restitution ou d archivage (constante de STAFService)
     * @return Indique s'il existe des sessions disponibles selon le mode
     */
    private boolean hasFreeSessions(ArchiveAccessModeEnum mode) {
        boolean freeSession = false;
        // Restitution mode
        if (mode == ArchiveAccessModeEnum.RESTITUTION_MODE) {
            freeSession = configuration.getMaxSessionsRestitutionMode().intValue() > reservations.size();
        } else if (mode == ArchiveAccessModeEnum.ARCHIVE_MODE) { // Archivage mode
            freeSession = configuration.getMaxSessionsArchivingMode().intValue() > reservations.size();
        }
        return freeSession;
    }

    /**
     * Obtient un nouvel identificateur de reservation. Cet identificateur est stocke dans la table des reservations en
     * cours.
     * @return Le nouvel identificateur
     */
    private int getReservationIdentifier() {
        // Calcule le nouvel identifiant
        reservations.add(lastIdentifier.incrementAndGet());
        return lastIdentifier.get();
    }

    /**
     * Verifie si une reservation autorise l'ouverture d'une session pour une restitution ou un archivage
     * @param reservation Reservation a verifier
     * @param mode mode de restitution ou d archivage (constante de STAFService)
     * @return vrai si la reservation est autorisee, faux sinon
     */
    private boolean isReservationAuthorized(Integer reservation, ArchiveAccessModeEnum mode) {
        int reservationIndex = reservations.indexOf(reservation);
        boolean reservationAuthorized = false;
        // Restitution mode
        if (mode == ArchiveAccessModeEnum.RESTITUTION_MODE) {
            reservationAuthorized = reservationIndex < configuration.getMaxSessionsRestitutionMode().intValue();
        }
        // Archive mode
        if (mode == ArchiveAccessModeEnum.ARCHIVE_MODE) {
            reservationAuthorized = reservationIndex < configuration.getMaxSessionsArchivingMode().intValue();
        }
        if (logger.isDebugEnabled()) {
            logger.debug(" session reservation is possible : " + reservationAuthorized);
        }
        return reservationAuthorized;
    }

    /**
     * Get method.
     * @return the reservations
     */
    public List<Integer> getReservations() {
        return reservations;
    }

    /**
     * Get method.
     * @return the configuration
     */
    public STAFConfiguration getConfiguration() {
        return configuration;
    }

    /**
     * Set method.
     * @param configuration the configuration to set
     */
    public void setConfiguration(STAFConfiguration configuration) {
        this.configuration = configuration;
    }

    /**
     * Create a new service to retreive a STAF Access service for the given {@link STAFArchive}
     */
    public STAFService getNewArchiveAccessService(STAFArchive stafArchive) {
        return new STAFService(this, stafArchive);
    }

    /**
     * Method only useable by tests (=> protected)
     * Release all currently blocking tokens
     */
    protected void releaseAllCurrentlyBlockingReservations() throws InterruptedException {
        Semaphore semaphore = semaphoreMap.get(ArchiveAccessModeEnum.ARCHIVE_MODE);
        if (semaphore.availablePermits()< configuration.getMaxSessionsArchivingMode()) {
            semaphore.release(configuration.getMaxSessionsArchivingMode() - semaphore.availablePermits());
        }
        semaphore = semaphoreMap.get(ArchiveAccessModeEnum.RESTITUTION_MODE);
        if (semaphore.availablePermits() < configuration.getMaxSessionsRestitutionMode()) {
            // Don't use relase(int permist) !!!! Each time a token is released it may unblock
            // a waiting task and so acquire a new token and so decrease available permits
            semaphore.release(configuration.getMaxSessionsRestitutionMode() - semaphore.availablePermits());
        }
    }
}
