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
     * @param pState
     * @param pPageable
     * @return
     */
    @EntityGraph("graph.aip.tags")
    Page<AIPEntity> findAllByStateIn(AIPState pState, Pageable pPageable);

    @EntityGraph("graph.aip.tags")
    Set<AIPEntity> findAllByStateIn(AIPState... states);

    /**
     * @param pState
     * @param pFrom
     * @param pTo
     * @param pPageable
     * @return
     */
    @EntityGraph("graph.aip.tags")
    Page<AIPEntity> findAllByStateAndSubmissionDateAfterAndLastEventDateBefore(AIPState pState, OffsetDateTime pFrom,
            OffsetDateTime pTo, Pageable pPageable);

    /**
     * @param pState
     * @param pFrom
     * @param pPageable
     * @return
     */
    @EntityGraph("graph.aip.tags")
    Page<AIPEntity> findAllByStateAndSubmissionDateAfter(AIPState pState, OffsetDateTime pFrom, Pageable pPageable);

    /**
     * @param pState
     * @param pFrom
     * @param pPageable
     * @return
     */
    @EntityGraph("graph.aip.tags")
    Page<AIPEntity> findAllByStateAndLastEventDateBefore(AIPState pState, OffsetDateTime pFrom, Pageable pPageable);

    /**
     * @param pFrom
     * @param pTo
     * @param pPageable
     * @return
     */
    @EntityGraph("graph.aip.tags")
    Page<AIPEntity> findAllBySubmissionDateAfterAndLastEventDateBefore(OffsetDateTime pFrom, OffsetDateTime pTo,
            Pageable pPageable);

    /**
     * @param pFrom
     * @param pPageable
     * @return
     */
    @EntityGraph("graph.aip.tags")
    Page<AIPEntity> findAllBySubmissionDateAfter(OffsetDateTime pFrom, Pageable pPageable);

    /**
     * @param pTo
     * @param pPageable
     * @return
     */
    @EntityGraph("graph.aip.tags")
    Page<AIPEntity> findAllByLastEventDateBefore(OffsetDateTime pTo, Pageable pPageable);

    //    @Query("from AIPEntity aip left join fetch aip.dataObjects where aip.ipId=:ipId")
    //    AIPEntity findOneByIpIdWithDataObjects(@Param("ipId") String pIpId);

    @Query("from AIPEntity aip where aip.ipId LIKE :urnWithoutVersion%")
    Set<AIPEntity> findAllByIpIdStartingWith(@Param("urnWithoutVersion") String pUrnWithoutVersion);

    @EntityGraph("graph.aip.tags")
    Optional<AIPEntity> findOneByIpId(String ipId);

    @EntityGraph("graph.aip.tags")
    Set<AIPEntity> findAllByIpIdIn(Collection<String> ipIds);

    @EntityGraph("graph.aip.tags")
    Set<AIPEntity> findAllByTags(String tag);

    @EntityGraph("graph.aip.tags")
    Set<AIPEntity> findAllBySipId(String sipIpId);

    @EntityGraph("graph.aip.tags")
    Page<AIPEntity> findAllBySipId(String sipId, Pageable pageable);
}
