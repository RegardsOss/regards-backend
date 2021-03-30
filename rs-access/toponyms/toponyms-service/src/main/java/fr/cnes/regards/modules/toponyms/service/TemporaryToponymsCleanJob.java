package fr.cnes.regards.modules.toponyms.service;

import fr.cnes.regards.framework.modules.jobs.domain.AbstractJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * This job is used to clean expired temporary toponyms
 * @author Iliana Ghazali
 */

public class TemporaryToponymsCleanJob extends AbstractJob<Void> {

    @Autowired
    private TemporaryToponymsCleanService cleanService;

    /**
     * Class logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(TemporaryToponymsCleanJob.class);

    @Override
    public void run() {
        long start = System.currentTimeMillis();
        LOGGER.info("[CLEAN TEMPORARY TOPONYMS JOB] - Scanning the number of temporary toponyms to delete");
        int nbDeleted = cleanService.clean();
        LOGGER.info("[CLEAN TEMPORARY TOPONYMS JOB] - Job handled in {}ms. {} temporary toponyms deleted.", System.currentTimeMillis() - start, nbDeleted);
    }
}
