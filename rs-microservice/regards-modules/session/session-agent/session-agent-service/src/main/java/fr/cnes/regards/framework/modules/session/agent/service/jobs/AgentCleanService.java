package fr.cnes.regards.framework.modules.session.agent.service.jobs;

import fr.cnes.regards.framework.jpa.utils.RegardsTransactional;
import fr.cnes.regards.framework.modules.session.sessioncommons.dao.ISessionStepRepository;
import fr.cnes.regards.framework.modules.session.sessioncommons.domain.SessionStep;
import java.time.OffsetDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

/**
 * @author Iliana Ghazali
 **/
@Service
@RegardsTransactional
public class AgentCleanService {

    @Autowired
    private ISessionStepRepository sessionStepRepo;

    @Value("${regards.session-agent.limit.store.session-steps:30}")
    private int limitStoreSessionSteps;

    private static final Logger LOGGER = LoggerFactory.getLogger(AgentCleanService.class);


    public int clean() {
        int nbStepsDeleted = 0;
        OffsetDateTime startClean = OffsetDateTime.now();
        LOGGER.debug("Check old session steps. Current date : {}", startClean);

        Pageable page = PageRequest.of(0, 100);
        Page<SessionStep> sessionStepsToDelete;
        do {
            sessionStepsToDelete = sessionStepRepo.findByLastUpdateBefore(startClean, page);
        } while (sessionStepsToDelete.hasNext());
        return nbStepsDeleted;
    }
}
