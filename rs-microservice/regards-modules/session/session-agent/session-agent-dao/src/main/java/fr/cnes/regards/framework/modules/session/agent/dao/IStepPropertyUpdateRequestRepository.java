package fr.cnes.regards.framework.modules.session.agent.dao;

/**
 * @author Iliana Ghazali
 **/

import fr.cnes.regards.framework.modules.session.agent.domain.StepPropertyUpdateRequest;
import fr.cnes.regards.framework.modules.session.commons.domain.SessionStep;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * JPA Repository for {@link StepPropertyUpdateRequest}
 *
 * @author Iliana Ghazali
 **/

@Repository
public interface IStepPropertyUpdateRequestRepository extends JpaRepository<StepPropertyUpdateRequest, Long> {

    Set<StepPropertyUpdateRequest> findBySourceAndDateBetween(String source, OffsetDateTime lastUpdate,
            OffsetDateTime freezeDate);

    long countBySourceAndDateBetween(String source, OffsetDateTime lastUpdate, OffsetDateTime freezeDate);

    Set<StepPropertyUpdateRequest> findBySourceAndDateBefore(String source, OffsetDateTime freezeDate);

    long countBySourceAndDateBefore(String source, OffsetDateTime lastUpdate);

    List<StepPropertyUpdateRequest> findBySessionStepIn(List<SessionStep> content);
}
