package fr.cnes.regards.framework.modules.session.management.service.clean.snapshotprocess;

import fr.cnes.regards.framework.modules.jobs.domain.AbstractJob;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * See {@link ManagerCleanSnapshotProcessService}
 *
 * @author Iliana Ghazali
 **/
public class ManagerCleanSnapshotProcessJob extends AbstractJob<Void> {

    @Autowired
    private ManagerCleanSnapshotProcessService managerCleanSnapshotProcessService;

    @Override
    public void run() {
        logger.debug("[{}] ManagerCleanSnapshotProcessJob starts", jobInfoId);
        long start = System.currentTimeMillis();
        int nbSnapshotProcessDeleted = managerCleanSnapshotProcessService.clean();
        logger.debug("[{}] ManagerCleanSnapshotProcessJob ends in {} ms. {} snapshot process deleted", jobInfoId,
                     System.currentTimeMillis() - start, nbSnapshotProcessDeleted);
    }

}