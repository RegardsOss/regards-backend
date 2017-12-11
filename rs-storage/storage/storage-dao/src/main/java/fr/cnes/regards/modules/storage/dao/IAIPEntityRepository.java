/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.storage.dao;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import fr.cnes.regards.modules.storage.domain.AIP;
import fr.cnes.regards.modules.storage.domain.AIPState;
import fr.cnes.regards.modules.storage.domain.database.AIPEntity;

/**
 *
 * Repository handling JPA representation of AIP.
 *
 * @author Sylvain Vissiere-Guerinet
 *
 */
public interface IAIPEntityRepository extends JpaRepository<AIPEntity, Long> {

    /**
     * Find a page of aips which state is the provided one
     * @param pState
     * @param pPageable
     * @return a page of aips which state is the provided one
     */
    @EntityGraph("graph.aip.tags")
    Page<AIPEntity> findAllByStateIn(AIPState pState, Pageable pPageable);

    /**
     * Find all aips which state is one of the provided one
     * @param states
     * @return aips which state is one of the provided one
     */
    @EntityGraph("graph.aip.tags")
    Set<AIPEntity> findAllByStateIn(AIPState... states);

    /**
     * Find a page of aips which state is the provided one and which has been submitted after the given date and which last event occurred before the given date
     * @param pState
     * @param pFrom
     * @param pTo
     * @param pPageable
     * @return a page of aips which state is the provided one and which has been submitted after the given date and which last event occurred before the given date
     */
    @EntityGraph("graph.aip.tags")
    Page<AIPEntity> findAllByStateAndSubmissionDateAfterAndLastEventDateBefore(AIPState pState, OffsetDateTime pFrom,
            OffsetDateTime pTo, Pageable pPageable);

    /**
     * Find a page of aips which state is the provided one and which has been submitted after the given date
     * @param pState
     * @param pFrom
     * @param pPageable
     * @return a page of aips which state is the provided one and which has been submitted after the given date
     */
    @EntityGraph("graph.aip.tags")
    Page<AIPEntity> findAllByStateAndSubmissionDateAfter(AIPState pState, OffsetDateTime pFrom, Pageable pPageable);

    /**
     * Find a page of aips which state is the provided one and which last event occurred before the given date
     * @param pState
     * @param pFrom
     * @param pPageable
     * @return a page of aips which state is the provided one and which last event occurred before the given date
     */
    @EntityGraph("graph.aip.tags")
    Page<AIPEntity> findAllByStateAndLastEventDateBefore(AIPState pState, OffsetDateTime pFrom, Pageable pPageable);

    /**
     * Find a page of aips which has been submitted after the given date and which last event occurred before the given date
     * @param pFrom
     * @param pTo
     * @param pPageable
     * @return a page of aips which has been submitted after the given date and which last event occurred before the given date
     */
    @EntityGraph("graph.aip.tags")
    Page<AIPEntity> findAllBySubmissionDateAfterAndLastEventDateBefore(OffsetDateTime pFrom, OffsetDateTime pTo,
            Pageable pPageable);

    /**
     * Find a page of aips which has been submitted after the given date
     * @param pFrom
     * @param pPageable
     * @return a page of aips which has been submitted after the given date
     */
    @EntityGraph("graph.aip.tags")
    Page<AIPEntity> findAllBySubmissionDateAfter(OffsetDateTime pFrom, Pageable pPageable);

    /**
     * Retrieve a page of aips which last event occurred before the given date
     * @param pTo
     * @param pPageable
     * @return a page of aips which last event occurred before the given date
     */
    @EntityGraph("graph.aip.tags")
    Page<AIPEntity> findAllByLastEventDateBefore(OffsetDateTime pTo, Pageable pPageable);

    /**
     * Retrieve a page of aip which state is the one provided and contains the provided tags and which last event occurred after the given date
     * @param state
     * @param tags
     * @param fromLastUpdateDate
     * @param pageable
     * @return a page of aip which state is the one provided and contains the provided tags and which last event occurred after the given date
     */
    @EntityGraph("graph.aip.tags")
    Page<AIPEntity> findAllByStateAndTagsInAndLastEventDateAfter(AIPState state, Set<String> tags,
            OffsetDateTime fromLastUpdateDate, Pageable pageable);

    /**
     * Retrieve a page of aip which state is the one provided and contains the provided tags
     * @param state
     * @param tags
     * @param pageable
     * @return a page of aip which state is the one provided and contains the provided tags
     */
    @EntityGraph("graph.aip.tags")
    Page<AIPEntity> findAllByStateAndTagsIn(AIPState state, Set<String> tags, Pageable pageable);

    /**
     * Retrieve all aips which ip id starts with the provided string
     * @param pUrnWithoutVersion
     * @return aips respecting the constraints
     */
    @Query("from AIPEntity aip where aip.ipId LIKE :urnWithoutVersion%")
    Set<AIPEntity> findAllByIpIdStartingWith(@Param("urnWithoutVersion") String pUrnWithoutVersion);

    /**
     * Retrieve an aip by its ip id
     * @param ipId
     * @return requested aip
     */
    @EntityGraph("graph.aip.tags")
    Optional<AIPEntity> findOneByIpId(String ipId);

    /**
     * Retrieve all aips which ip id is one of the provided ones
     * @param ipIds
     * @return all aips which respects the constraints
     */
    @EntityGraph("graph.aip.tags")
    Set<AIPEntity> findAllByIpIdIn(Collection<String> ipIds);

    /**
     * Retrieve all aips which are tagged by the provided tag
     * @param tag
     * @return aips which respects the constraints
     */
    @EntityGraph("graph.aip.tags")
    Set<AIPEntity> findAllByTags(String tag);

    /**
     * Retrieve all aips which sip id is the provided one
     * @param sipIpId
     * @return aips which respects the constraints
     */
    @EntityGraph("graph.aip.tags")
    Set<AIPEntity> findAllBySipId(String sipIpId);

    /**
     * Retrieve page of aips which sip id is the provided one
     * @param sipId
     * @param pageable
     * @return a page of aip respecting the constraints
     */
    @EntityGraph("graph.aip.tags")
    Page<AIPEntity> findAllBySipId(String sipId, Pageable pageable);
}
