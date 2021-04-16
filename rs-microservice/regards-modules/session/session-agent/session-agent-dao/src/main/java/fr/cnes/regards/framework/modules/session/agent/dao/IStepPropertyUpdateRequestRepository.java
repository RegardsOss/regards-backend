package fr.cnes.regards.framework.modules.session.agent.dao;

/**
 * @author Iliana Ghazali
 **/

import fr.cnes.regards.framework.modules.session.agent.domain.StepPropertyUpdateRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * @author Iliana Ghazali
 **/

@Repository
public interface IStepPropertyUpdateRequestRepository extends JpaRepository<StepPropertyUpdateRequest, Long>{

}
