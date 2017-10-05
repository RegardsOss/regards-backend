/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.storage.dao;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import fr.cnes.regards.modules.storage.domain.AIPState;
import fr.cnes.regards.modules.storage.domain.database.AIPDataBase;

/**
 *
 * Repository handling JPA representation of AIP.
 *
 * @author Sylvain Vissiere-Guerinet
 *
 */
public interface IAIPDataBaseRepository extends JpaRepository<AIPDataBase, String> {

    /**
     * @param pState
     * @param pPageable
     * @return
     */
    @EntityGraph("graph.aip.tags")
    Page<AIPDataBase> findAllByStateIn(AIPState pState, Pageable pPageable);

    /**
     * @param pState
     * @param pFrom
     * @param pTo
     * @param pPageable
     * @return
     */
    @EntityGraph("graph.aip.tags")
    Page<AIPDataBase> findAllByStateAndSubmissionDateAfterAndLastEventDateBefore(AIPState pState, OffsetDateTime pFrom,
            OffsetDateTime pTo, Pageable pPageable);

    /**
     * @param pState
     * @param pFrom
     * @param pPageable
     * @return
     */
    @EntityGraph("graph.aip.tags")
    Page<AIPDataBase> findAllByStateAndSubmissionDateAfter(AIPState pState, OffsetDateTime pFrom, Pageable pPageable);

    /**
     * @param pState
     * @param pFrom
     * @param pPageable
     * @return
     */
    @EntityGraph("graph.aip.tags")
    Page<AIPDataBase> findAllByStateAndLastEventDateBefore(AIPState pState, OffsetDateTime pFrom, Pageable pPageable);

    /**
     * @param pFrom
     * @param pTo
     * @param pPageable
     * @return
     */
    @EntityGraph("graph.aip.tags")
    Page<AIPDataBase> findAllBySubmissionDateAfterAndLastEventDateBefore(OffsetDateTime pFrom, OffsetDateTime pTo,
            Pageable pPageable);

    /**
     * @param pFrom
     * @param pPageable
     * @return
     */
    @EntityGraph("graph.aip.tags")
    Page<AIPDataBase> findAllBySubmissionDateAfter(OffsetDateTime pFrom, Pageable pPageable);

    /**
     * @param pTo
     * @param pPageable
     * @return
     */
    @EntityGraph("graph.aip.tags")
    Page<AIPDataBase> findAllByLastEventDateBefore(OffsetDateTime pTo, Pageable pPageable);

    //    @Query("from AIPDataBase aip left join fetch aip.dataObjects where aip.ipId=:ipId")
    //    AIPDataBase findOneByIpIdWithDataObjects(@Param("ipId") String pIpId);

    @Query("from AIPDataBase aip where aip.ipId LIKE :urnWithoutVersion%")
    Set<AIPDataBase> findAllByIpIdStartingWith(@Param("urnWithoutVersion") String pUrnWithoutVersion);

    @EntityGraph("graph.aip.tags")
    Set<AIPDataBase> findAllByStateIn(AIPState... states);

    @EntityGraph("graph.aip.tags")
    Optional<AIPDataBase> findOneByIpId(String ipId);
}
