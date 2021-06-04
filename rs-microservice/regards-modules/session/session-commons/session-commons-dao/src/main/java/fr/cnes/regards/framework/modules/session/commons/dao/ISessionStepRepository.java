package fr.cnes.regards.framework.modules.session.commons.dao;

import fr.cnes.regards.framework.modules.session.commons.domain.SessionStep;
import java.time.OffsetDateTime;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * JPA Repository for {@link SessionStep}
 *
 * @author Iliana Ghazali
 **/

@Repository
public interface ISessionStepRepository extends JpaRepository<SessionStep, Long> {

    Optional<SessionStep> findBySourceAndSessionAndStepId(String source, String session, String stepId);

    /**
     * Methods to calculate snapshots of SessionSteps
     */
    int countBySourceAndLastUpdateDateBefore(String source, OffsetDateTime schedulerStartDate);

    Page<SessionStep> findBySourceAndLastUpdateDateBefore(String source, OffsetDateTime freezeDate,
            Pageable pageToRequest);

    int countBySourceAndLastUpdateDateGreaterThanAndLastUpdateDateLessThanEqual(String source,
            OffsetDateTime lastUpdateDate, OffsetDateTime schedulerStartDate);

    Page<SessionStep> findBySourceAndLastUpdateDateGreaterThanAndLastUpdateDateLessThanEqual(String source,
            OffsetDateTime lastUpdateDate, OffsetDateTime freezeDate, Pageable pageToRequest);

    /**
     * Clean SessionSteps
     */
    void deleteByLastUpdateDateBefore(OffsetDateTime startClean);

    Page<SessionStep> findByLastUpdateDateBefore(OffsetDateTime startClean, Pageable page);

    /**
     * Return all names of sources and sessions associated to SessionSteps
     */
    Page<ISessionStepLight> findBy(Pageable pageToRequest);
}
