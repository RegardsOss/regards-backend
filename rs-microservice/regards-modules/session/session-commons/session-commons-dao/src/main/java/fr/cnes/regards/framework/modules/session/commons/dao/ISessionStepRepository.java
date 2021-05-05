package fr.cnes.regards.framework.modules.session.commons.dao;

import fr.cnes.regards.framework.modules.session.commons.domain.SessionStep;
import java.time.OffsetDateTime;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

/**
 * JPA Repository for {@link SessionStep}
 *
 * @author Iliana Ghazali
 **/

@Repository
public interface ISessionStepRepository extends JpaRepository<SessionStep, Long> {

    Optional<SessionStep> findBySourceAndSessionAndStepId(String source, String session, String stepId);

    Page<SessionStep> findByLastUpdateDateBefore(OffsetDateTime startClean, Pageable page);

    int countBySourceAndLastUpdateDateBefore(String source, OffsetDateTime schedulerStartDate);

    int countBySourceAndLastUpdateDateBetween(String source, OffsetDateTime lastUpdateDate,
            OffsetDateTime schedulerStartDate);

    Page<SessionStep> findBySourceAndLastUpdateDateBetween(String source, OffsetDateTime lastUpdateDate,
            OffsetDateTime freezeDate, Pageable pageToRequest);

    Page<SessionStep> findBySourceAndLastUpdateDateBefore(String source, OffsetDateTime freezeDate,
            Pageable pageToRequest);

    void deleteByLastUpdateDateBefore(OffsetDateTime startClean);

    @Modifying
    @Query("DELETE FROM SnapshotProcess p where p.source NOT IN (SELECT s.source FROM SessionStep s) "
            + "AND (p.lastUpdateDate IS NULL OR p.lastUpdateDate <= ?1)")
    int deleteUnusedProcess(OffsetDateTime limitDate);
}
