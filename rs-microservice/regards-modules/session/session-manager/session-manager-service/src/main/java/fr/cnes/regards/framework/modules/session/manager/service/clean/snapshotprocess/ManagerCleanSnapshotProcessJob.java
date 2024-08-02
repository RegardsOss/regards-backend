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
package fr.cnes.regards.framework.modules.session.manager.service.clean.snapshotprocess;

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
        logger.debug("[{}] ManagerCleanSnapshotProcessJob ends in {} ms. {} snapshot process deleted",
                     jobInfoId,
                     System.currentTimeMillis() - start,
                     nbSnapshotProcessDeleted);
    }

}