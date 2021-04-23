package fr.cnes.regards.framework.modules.session.agent.dao;

/**
 * @author Iliana Ghazali
 **/

import fr.cnes.regards.framework.modules.session.agent.domain.StepPropertyUpdateRequest;
import fr.cnes.regards.framework.modules.session.commons.domain.SessionStep;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

/**
 * JPA Repository for {@link StepPropertyUpdateRequest}
 *
 * @author Iliana Ghazali
 **/

@Repository
public interface IStepPropertyUpdateRequestRepository extends JpaRepository<StepPropertyUpdateRequest, Long> {

    Page<StepPropertyUpdateRequest> findBySourceAndDateBetween(String source, OffsetDateTime lastUpdate,
            OffsetDateTime freezeDate, Pageable page);

    long countBySourceAndDateBetween(String source, OffsetDateTime lastUpdate, OffsetDateTime freezeDate);

    Page<StepPropertyUpdateRequest> findBySourceAndDateBefore(String source, OffsetDateTime freezeDate, Pageable page);

    long countBySourceAndDateBefore(String source, OffsetDateTime lastUpdate);

    List<StepPropertyUpdateRequest> findBySessionStepIn(List<SessionStep> content);

    @Modifying
    @Query("DELETE FROM SnapshotProcess p where p.source NOT IN (SELECT s.source FROM StepPropertyUpdateRequest s) "
            + "AND (p.lastUpdate IS NULL OR p.lastUpdate <= ?1)")
    int deleteUnusedProcess(OffsetDateTime limitDate);
}
