/*
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
 *
 * This file is part of REGARDS.
 *
 * REGARDS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * REGARDS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with REGARDS. If not, see <http://www.gnu.org/licenses/>.
 */


package fr.cnes.regards.modules.crawler.service.job;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.jobs.domain.AbstractJob;
import fr.cnes.regards.framework.utils.RsRuntimeException;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * This job is used to reset the catalog
 *
 * @author Iliana Ghazali
 */

public class CatalogResetJob extends AbstractJob<Void> {

    @Autowired
    private CatalogResetService datasourceDeletionService;

    @Override
    public void run() {
        logger.debug("[CATALOG RESET JOB] Running job to reset the catalog");
        long start = System.currentTimeMillis();
        try {
            datasourceDeletionService.resetCatalog();
        } catch (ModuleException e) {
            logger.error("[CATALOG RESET JOB] An error occured during the reset of the catalog", e);
            throw new RsRuntimeException(e);
        }
        logger.debug("[CATALOG RESET JOB] Job handled for the catalog reset in {}ms",
                     System.currentTimeMillis() - start);
    }

}
