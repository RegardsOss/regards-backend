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
package fr.cnes.regards.framework.modules.session.agent.service.clean.snapshotprocess;

import fr.cnes.regards.framework.modules.jobs.domain.AbstractJob;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * See {@link AgentCleanSnapshotProcessService}
 *
 * @author Iliana Ghazali
 **/
public class AgentCleanSnapshotProcessJob extends AbstractJob<Void> {

    @Autowired
    private AgentCleanSnapshotProcessService agentCleanSnapshotProcessService;

    @Override
    public void run() {
        logger.debug("[{}] AgentCleanSnapshotProcessJob starts...", jobInfoId);
        long start = System.currentTimeMillis();
        int nbSnapshotProcessDeleted = agentCleanSnapshotProcessService.clean();
        logger.debug("[{}] AgentCleanSnapshotProcessJob ends in {} ms. {} snapshot process deleted",
                     jobInfoId,
                     System.currentTimeMillis() - start,
                     nbSnapshotProcessDeleted);
    }

}