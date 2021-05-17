package fr.cnes.regards.framework.modules.session.management.service.clean.snapshotprocess;

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.modules.session.commons.dao.ISnapshotProcessRepository;
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
public class ManagerCleanSnapshotProcessService {

    @Autowired
    private ISnapshotProcessRepository snapshotProcessRepo;

    @Value("${regards.session.manager.limit.store.snapshot.process:30}")
    private int limitStoreSnapshotProcess;

    private static final Logger LOGGER = LoggerFactory.getLogger(ManagerCleanSnapshotProcessService.class);

    public int clean() {
        // Init startClean with the current date minus the limit of SnapshotProcess save configured
        OffsetDateTime startClean = OffsetDateTime.now().minusDays(this.limitStoreSnapshotProcess);
        LOGGER.debug("Check unused snapshot processes before {}", startClean);
        return this.snapshotProcessRepo.deleteUnusedProcess(startClean);
    }
}