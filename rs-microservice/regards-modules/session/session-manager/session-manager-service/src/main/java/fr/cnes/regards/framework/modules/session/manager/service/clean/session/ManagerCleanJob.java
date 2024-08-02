/*
 * Copyright 2017-2024 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.framework.modules.session.manager.service.clean.session;

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
        logger.debug("[{}] AgentCleanJob ends in {} ms. {} sessions deleted",
                     jobInfoId,
                     System.currentTimeMillis() - start,
                     nbSession);
    }
}