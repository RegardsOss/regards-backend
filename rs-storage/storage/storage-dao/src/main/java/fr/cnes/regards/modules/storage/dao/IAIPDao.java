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
     * Retrieve a page of aip which state is the one provided and contains at least one of the provided tags and which last event occurred after the given date
     * @param state
     * @param tags
     * @param fromLastUpdateDate
     * @param pageable
     * @return a page of aip which state is the one provided and contains at least one of the provided tags and which last event occurred after the given date
     */
    Page<AIP> findAllByStateAndTagsInAndLastEventDateAfter(AIPState state, Set<String> tags,
            OffsetDateTime fromLastUpdateDate, Pageable pageable);

    /**
     * Retrieve a page of aip which state is the one provided and contains at least one of the provided tags
     * @param state
     * @param tags
     * @param pageable
     * @return a page of aip which state is the one provided and contains at least one of the provided tags
     */
    Page<AIP> findAllByStateAndTagsIn(AIPState state, Set<String> tags, Pageable pageable);

    /**
     * Retrieve all existing {@link AIP}s with given starting ipId {@link String}
     * @param ipIdWithoutVersion starting ipId {@link String}
     * @return {@link AIP}s
     */
    Set<AIP> findAllByIpIdStartingWith(String ipIdWithoutVersion);

    /**
     * Retrieve all aips which state is the one given
     * @param state
     * @return aips which state is the requested one
     */
    Set<AIP> findAllByStateService(AIPState state);

    /**
     * Retrieve a single aip according to its ip id
     * @param ipId
     * @return an optional wrapping the aip to avoid nulls
     */
    Optional<AIP> findOneByIpId(String ipId);

    /**
     * Delete all aips from the database
     */
    void deleteAll();

    /**
     * Retrieve all aips which state is one of the provided ones
     * @param states
     * @return aips which state is one of the requested
     */
    Set<AIP> findAllByStateInService(AIPState... states);

    /**
     * Remove the given aip from the database
     * @param aip
     */
    void remove(AIP aip);

    /**
     * Retrieve all aip which ip id is one of the provided ones
     * @param ipIds
     * @return aips which ip id is one of the requested
     */
    Set<AIP> findAllByIpIdIn(Collection<String> ipIds);

    /**
     * Retrieve all aips which are tagged with the given tag
     * @param tag
     * @return aip tagged by tag
     */
    Set<AIP> findAllByTags(String tag);

    /**
     * Retrieve all aips which sip ip id is the given one
     * @param sipIpId
     * @return aips which sip ip id matches
     */
    Set<AIP> findAllBySipId(String sipIpId);

    Page<AIP> findAllByStateAndLastEventDateAfter(AIPState state, OffsetDateTime fromLastUpdateDate, Pageable pageable);
}
