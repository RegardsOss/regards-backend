package fr.cnes.regards.modules.storage.dao;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import fr.cnes.regards.modules.storage.domain.AIP;
import fr.cnes.regards.modules.storage.domain.AIPState;
import fr.cnes.regards.modules.storage.domain.database.AIPEntity;

/**
 * DAO to access {@link AIP} entities by requesting {@link AIPEntity}.
 * The {@link AIP} are built from the {@link AIPEntity} with json deserialization.
 *
 * @author Sylvain VISSIERE-GUERINET
 * @author Sébastien Binda
 */
public interface IAIPDao {

    /**
     * Create or update an {@link AIP}
     * @param toSave {@link AIP}
     * @return saved {@link AIP}
     */
    AIP save(AIP toSave);

    /**
     * Retrieve all existing {@link AIP}s.
     * @param pageable {@link Pageable} pagination parameters.
     * @return {@link AIP}s
     */
    Page<AIP> findAll(Pageable pageable);

    /**
     * Retrieve all existing {@link AIP}s with given {@link AIPState} state.
     * @param state {@link AIPState} state requested.
     * @param pageable {@link Pageable} pagination parameters.
     * @return {@link AIP}s
     */
    Page<AIP> findAllByState(AIPState state, Pageable pageable);

    /**
     * Retrieve all existing {@link AIP}s with submission date after the given {@link OffsetDateTime}
     * @param submissionAfter {@link OffsetDateTime} submission date.
     * @param pageable {@link Pageable} pagination parameters.
     * @return {@link AIP}s
     */
    Page<AIP> findAllBySubmissionDateAfter(OffsetDateTime submissionAfter, Pageable pageable);

    /**
     * Retrieve all existing {@link AIP}s with last event date before the given {@link OffsetDateTime}
     * @param lastEventBefore {@link OffsetDateTime} last event date.
     * @param pageable {@link Pageable} pagination parameters.
     * @return {@link AIP}s
     */
    Page<AIP> findAllByLastEventDateBefore(OffsetDateTime lastEventBefore, Pageable pageable);

    /**
     * Retrieve all existing {@link AIP}s with last event date before the given {@link OffsetDateTime} and
     * with given {@link AIPState} state.
     * @param state {@link AIPState} state requested.
     * @param lastEventBefore {@link OffsetDateTime} last event date.
     * @param pageable {@link Pageable} pagination parameters.
     * @return {@link AIP}s
     */
    Page<AIP> findAllByStateAndLastEventDateBefore(AIPState state, OffsetDateTime lastEventBefore, Pageable pageable);

    /**
     * Retrieve all existing {@link AIP}s  with submission date after the given  {@link OffsetDateTime},
     * last event date before the given {@link OffsetDateTime} and with given {@link AIPState} state.
     * @param state {@link AIPState} state requested.
     * @param submissionAfter {@link OffsetDateTime} submission date.
     * @param lastEventBefore {@link OffsetDateTime} last event date.
     * @param pageable {@link Pageable} pagination parameters.
     * @return {@link AIP}s
     */
    Page<AIP> findAllByStateAndSubmissionDateAfterAndLastEventDateBefore(AIPState state, OffsetDateTime submissionAfter,
            OffsetDateTime lastEventBefore, Pageable pageable);

    /**
     * Retrieve all existing {@link AIP}s with submission date after the given {@link OffsetDateTime} and
     * with given {@link AIPState} state.
     * @param state {@link AIPState} state requested.
     * @param submissionAfter {@link OffsetDateTime} submission date.
     * @param pageable {@link Pageable} pagination parameters.
     * @return {@link AIP}s
     */
    Page<AIP> findAllByStateAndSubmissionDateAfter(AIPState state, OffsetDateTime submissionAfter, Pageable pageable);

    /**
     * Retrieve all existing {@link AIP}s  with submission date after the given  {@link OffsetDateTime}
     * and last event date before the given {@link OffsetDateTime}.
     * @param submissionAfter {@link OffsetDateTime} submission date.
     * @param lastEventBefore {@link OffsetDateTime} last event date.
     * @param pageable {@link Pageable} pagination parameters.
     * @return {@link AIP}s
     */
    Page<AIP> findAllBySubmissionDateAfterAndLastEventDateBefore(OffsetDateTime submissionAfter,
            OffsetDateTime lastEventBefore, Pageable pageable);

    /**
     * Retrieve all existing {@link AIP}s with given starting ipId {@link String}
     * @param ipIdWithoutVersion starting ipId {@link String}
     * @return {@link AIP}s
     */
    Set<AIP> findAllByIpIdStartingWith(String ipIdWithoutVersion);

    Set<AIP> findAllByStateService(AIPState state);

    Optional<AIP> findOneByIpId(String ipId);

    void deleteAll();

    Set<AIP> findAllByStateInService(AIPState... states);

    void remove(AIP associatedAIP);

    Set<AIP> findAllByIpIdIn(Collection<String> ipIds);

    Set<AIP> findAllByTags(String tag);

    Set<AIP> findAllBySipId(String sipIpId);
}
