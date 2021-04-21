package fr.cnes.regards.framework.modules.session.agent.service.clean;

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.modules.session.agent.dao.IStepPropertyUpdateRequestRepository;
import fr.cnes.regards.framework.modules.session.commons.dao.ISessionStepRepository;
import fr.cnes.regards.framework.modules.session.commons.dao.ISnapshotProcessRepository;
import fr.cnes.regards.framework.modules.session.commons.domain.SessionStep;
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
 * Service to clean old {@link fr.cnes.regards.framework.modules.session.commons.domain.SessionStep}
 * and {@link fr.cnes.regards.framework.modules.session.agent.domain.StepPropertyUpdateRequest }
 *
 * @author Iliana Ghazali
 **/
@Service
@MultitenantTransactional
public class AgentCleanSessionStepService {

    @Autowired
    private ISessionStepRepository sessionStepRepo;

    @Autowired
    private ISnapshotProcessRepository snapshotRepo;

    @Autowired
    private IStepPropertyUpdateRequestRepository stepPropertyRepo;

    @Value("${regards.session.agent.limit.store.session.steps:30}")
    private int limitStoreSessionSteps;

    @Value("${regards.session.agent.clean.session.steps.page:1000}")
    private int pageSize;

    private static final Logger LOGGER = LoggerFactory.getLogger(AgentCleanSessionStepService.class);

    public int clean() {
        // Init startClean with the current date minus the limit of SessionStep save configured
        OffsetDateTime startClean = OffsetDateTime.now().minusDays(this.limitStoreSessionSteps);
        LOGGER.debug("Check old session steps before {}", startClean);

        int nbSessionStepsDeleted = 0;
        Pageable page = PageRequest.of(0, pageSize);
        Page<SessionStep> sessionStepsToDelete;
        do {
            // Get all session steps to delete older than startClean
            sessionStepsToDelete = sessionStepRepo.findByLastUpdateBefore(startClean, page);
            // Delete all related StepPropertyUpdateRequests
            this.stepPropertyRepo
                    .deleteInBatch(stepPropertyRepo.findBySessionStepIn(sessionStepsToDelete.getContent()));
            // Delete SesionSteps
            this.sessionStepRepo.deleteInBatch(sessionStepsToDelete);
            nbSessionStepsDeleted += sessionStepsToDelete.getNumberOfElements();
        } while (sessionStepsToDelete.hasNext());
        return nbSessionStepsDeleted;
    }
}