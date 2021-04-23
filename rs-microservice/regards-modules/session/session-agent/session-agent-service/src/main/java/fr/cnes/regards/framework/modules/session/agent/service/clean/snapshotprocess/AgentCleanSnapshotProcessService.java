package fr.cnes.regards.framework.modules.session.agent.service.clean.snapshotprocess;

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.modules.session.agent.dao.IStepPropertyUpdateRequestRepository;
import java.time.OffsetDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Service to clean old unused {@link fr.cnes.regards.framework.modules.session.commons.domain.SnapshotProcess}
 *
 * @author Iliana Ghazali
 **/
@Service
@MultitenantTransactional
public class AgentCleanSnapshotProcessService {

    @Autowired
    private IStepPropertyUpdateRequestRepository stepPropertyRepo;

    @Value("${regards.session.agent.limit.store.snapshot.process:30}")
    private int limitStoreSnapshotProcess;

    private static final Logger LOGGER = LoggerFactory.getLogger(AgentCleanSnapshotProcessService.class);

    public int clean() {
        // Init startClean with the current date minus the limit of SnapshotProcess save configured
        OffsetDateTime startClean = OffsetDateTime.now().minusDays(this.limitStoreSnapshotProcess);
        LOGGER.debug("Check unused snapshot processes before {}", startClean);
        return this.stepPropertyRepo.deleteUnusedProcess(startClean);
    }
}