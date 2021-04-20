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

    Page<SessionStep> findByLastUpdateBefore(OffsetDateTime startClean, Pageable page);

}
