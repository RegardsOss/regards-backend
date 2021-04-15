package fr.cnes.regards.framework.modules.session.sessioncommons.dao;

import fr.cnes.regards.framework.modules.session.sessioncommons.domain.SessionStep;
import java.time.OffsetDateTime;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * @author Iliana Ghazali
 **/

@Repository
public interface ISessionStepRepository extends JpaRepository<SessionStep, Long> {


    Set<SessionStep> findSessionStepBySourceAndSession(String source, String session);

    Page<SessionStep> findByLastUpdateBefore(OffsetDateTime startClean, Pageable page);
}
