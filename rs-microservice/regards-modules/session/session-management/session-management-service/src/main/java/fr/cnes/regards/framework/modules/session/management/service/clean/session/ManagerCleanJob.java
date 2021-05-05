package fr.cnes.regards.framework.modules.session.management.service.clean.session;

import fr.cnes.regards.framework.modules.jobs.domain.AbstractJob;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * See {@link ManagerCleanService}
 *
 * @author Iliana Ghazali
 **/
public class ManagerCleanJob extends AbstractJob<Void> {

    @Autowired
    private ManagerCleanService managerCleanService;

    @Override
    public void run() {
        logger.debug("[{}] ManagerCleanJob starts ...", jobInfoId);
        long start = System.currentTimeMillis();
        int nbSession = managerCleanService.clean();
        logger.debug("[{}] AgentCleanJob ends in {} ms. {} sessions deleted", jobInfoId,
                     System.currentTimeMillis() - start, nbSession);
    }

}