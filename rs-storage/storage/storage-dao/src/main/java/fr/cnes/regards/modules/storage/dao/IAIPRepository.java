/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.storage.dao;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import fr.cnes.regards.modules.storage.domain.AIP;
import fr.cnes.regards.modules.storage.domain.AIPState;
import fr.cnes.regards.modules.storage.urn.UniformResourceName;

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
    Page<AIP> findAllByStateAndSubmissionDateAfterAndLastEventDateBefore(AIPState pState, LocalDateTime pFrom,
            LocalDateTime pTo, Pageable pPageable);

    /**
     * @param pState
     * @param pFrom
     * @param pPageable
     * @return
     */
    Page<AIP> findAllByStateAndSubmissionDateAfter(AIPState pState, LocalDateTime pFrom, Pageable pPageable);

    /**
     * @param pState
     * @param pFrom
     * @param pPageable
     * @return
     */
    Page<AIP> findAllByStateAndLastEventDateBefore(AIPState pState, LocalDateTime pFrom, Pageable pPageable);

    /**
     * @param pFrom
     * @param pTo
     * @param pPageable
     * @return
     */
    Page<AIP> findAllBySubmissionDateAfterAndLastEventDateBefore(LocalDateTime pFrom, LocalDateTime pTo,
            Pageable pPageable);

    /**
     * @param pFrom
     * @param pPageable
     * @return
     */
    Page<AIP> findAllBySubmissionDateAfter(LocalDateTime pFrom, Pageable pPageable);

    /**
     * @param pTo
     * @param pPageable
     * @return
     */
    Page<AIP> findAllByLastEventDateBefore(LocalDateTime pTo, Pageable pPageable);

    @Query("from AIP aip join fetch aip.dataObjects where aip.ipId=:ipId")
    AIP findOneByIpIdWithDataObjects(@Param("ipId") UniformResourceName pIpId);

    @Query("from AIP aip where aip.ipId LIKE :urnWithoutVersion%")
    List<AIP> findAllByIpIdStartingWith(@Param("urnWithoutVersion") String pUrnWithoutVersion);

}
