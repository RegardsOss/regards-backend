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
 * @author oroussel
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
     * Dernier identifiant de session attribué
     */
    private final AtomicInteger lastIdentifier = new AtomicInteger(0);

    /**
     * Liste des reservations en cours
     */
    private final List<Integer> reservations = Collections.synchronizedList(new ArrayList<>());

    /**
     * One semaphore per archive mode
     */
    private final Map<ArchiveAccessModeEnum, Semaphore> semaphoreMap = Collections
            .synchronizedMap(new EnumMap<ArchiveAccessModeEnum, Semaphore>(ArchiveAccessModeEnum.class));

    private STAFSessionManager(STAFConfiguration configuration) {
        this.configuration = configuration;
        // Creating fair semaphores (to respect FIFO)
        this.semaphoreMap.put(ArchiveAccessModeEnum.ARCHIVE_MODE,
                              new Semaphore(configuration.getMaxSessionsArchivingMode(), true));
        this.semaphoreMap.put(ArchiveAccessModeEnum.RESTITUTION_MODE,
                              new Semaphore(configuration.getMaxSessionsRestitutionMode(), true));
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
            synchronized (STAFSessionManager.class) {
                instance = new STAFSessionManager(configuration);
            }
        }
        return instance;
    }

    /**
     * Effectue la reservation d'un certain nombre de sessions pour une restitution ou une archive
     * @param reservationCount Nombre de reservations demandees
     * @param block Indique si l'octroi d'une reservation est bloquant (non pris en compte si plusieurs reservations demandées
     * @param mode mode de restitution ou d archivage (constante de STAFService)
     * @return Un tableau contenant les identifiants de reservation obtenus. En fonction de la disponibilite des
     * sessions, le nombre de reservations reellement obtenues peut etre inferieur au nombre de reservations
     * demandees.
     */
    public List<Integer> getReservations(int reservationCount, boolean block, ArchiveAccessModeEnum mode) {
        Semaphore semaphore = this.semaphoreMap.get(mode);
        // Reservations obtenues
        List<Integer> currentReservations = new ArrayList<>();

        // La reservation a lieu dans un section critique pour eviter tout
        // conflit en cas de reservation simultanee de plusieurs commandes.
        if (reservationCount > 1) {
            int tokenCount = Math.min(reservationCount, semaphore.availablePermits());
            for (int i = 0; i < tokenCount; i++) {
                try {
                    semaphore.acquire();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e); // NOSONAR
                }
                currentReservations.add(getReservationIdentifier());
            }
        } else if (block) { // Blocking unique reservation
            try {
                semaphore.acquire();
            } catch (InterruptedException e) {
                throw new RuntimeException(e); // NOSONAR
            }
            currentReservations.add(getReservationIdentifier());
        } else { // non blocking unique reservation
            if (semaphore.tryAcquire()) {
                currentReservations.add(getReservationIdentifier());
            }
        }
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
        if (logger.isDebugEnabled()) {
            logger.debug("session number " + reservation + " has been released");
        }
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
        if (semaphore.availablePermits() < configuration.getMaxSessionsArchivingMode()) {
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
