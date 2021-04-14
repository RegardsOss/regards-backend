package fr.cnes.regards.framework.modules.session.commons.dao;

import fr.cnes.regards.framework.modules.session.commons.domain.SessionStep;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Set;

/**
 * @author Iliana Ghazali
 **/

@Repository
public interface ISessionStepRepository extends JpaRepository<SessionStep, Long> {


    Set<SessionStep> findSessionStepBySource(String source);
}
