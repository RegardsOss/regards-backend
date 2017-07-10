/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.storage.dao;

import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import fr.cnes.regards.modules.storage.domain.AIP;
import fr.cnes.regards.modules.storage.domain.AIPState;

/**
 *
 * Repository handling JPA representation of AIP.
 *
 * @author Sylvain Vissiere-Guerinet
 *
 */
public interface IAIPRepository extends JpaRepository<AIP, Long> {

    /**
     * @param pState
     * @param pPageable
     * @return
     */
    Page<AIP> findAllByState(AIPState pState, Pageable pPageable);

    /**
     * @param pState
     * @param pFrom
     * @param pTo
     * @param pPageable
     * @return
     */
    Page<AIP> findAllByStateAndSubmissionDateAfterAndLastEventDateBefore(AIPState pState, OffsetDateTime pFrom,
            OffsetDateTime pTo, Pageable pPageable);

    /**
     * @param pState
     * @param pFrom
     * @param pPageable
     * @return
     */
    Page<AIP> findAllByStateAndSubmissionDateAfter(AIPState pState, OffsetDateTime pFrom, Pageable pPageable);

    /**
     * @param pState
     * @param pFrom
     * @param pPageable
     * @return
     */
    Page<AIP> findAllByStateAndLastEventDateBefore(AIPState pState, OffsetDateTime pFrom, Pageable pPageable);

    /**
     * @param pFrom
     * @param pTo
     * @param pPageable
     * @return
     */
    Page<AIP> findAllBySubmissionDateAfterAndLastEventDateBefore(OffsetDateTime pFrom, OffsetDateTime pTo,
            Pageable pPageable);

    /**
     * @param pFrom
     * @param pPageable
     * @return
     */
    Page<AIP> findAllBySubmissionDateAfter(OffsetDateTime pFrom, Pageable pPageable);

    /**
     * @param pTo
     * @param pPageable
     * @return
     */
    Page<AIP> findAllByLastEventDateBefore(OffsetDateTime pTo, Pageable pPageable);

    @Query("from AIP aip left join fetch aip.dataObjects where aip.ipId=:ipId")
    AIP findOneByIpIdWithDataObjects(@Param("ipId") String pIpId);

    @Query("from AIP aip where aip.ipId LIKE :urnWithoutVersion%")
    List<AIP> findAllByIpIdStartingWith(@Param("urnWithoutVersion") String pUrnWithoutVersion);

}
