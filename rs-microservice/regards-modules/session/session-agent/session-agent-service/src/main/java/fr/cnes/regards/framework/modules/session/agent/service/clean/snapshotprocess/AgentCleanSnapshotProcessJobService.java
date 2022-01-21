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
package fr.cnes.regards.framework.modules.session.agent.service.clean.snapshotprocess;

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.service.JobInfoService;
import fr.cnes.regards.framework.modules.session.commons.dao.ISnapshotProcessRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Service to launch {@link AgentCleanSnapshotProcessJob}
 *
 * @author Iliana Ghazali
 **/
@MultitenantTransactional
public class AgentCleanSnapshotProcessJobService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AgentCleanSnapshotProcessJobService.class);

    @Autowired
    private JobInfoService jobInfoService;

    @Autowired
    private ISnapshotProcessRepository snapshotProcessRepo;

    public void scheduleJob() {
        LOGGER.trace("[CLEAN SNAPSHOT PROCESS SCHEDULER] Scheduling job ...");
        long start = System.currentTimeMillis();
        JobInfo jobInfo = new JobInfo(false, 0, null, null, AgentCleanSnapshotProcessJob.class.getName());
        // create job
        jobInfoService.createAsQueued(jobInfo);
        LOGGER.trace("[CLEAN SNAPSHOT PROCESS ] AgentCleanSnapshotProcessJob scheduled in {}",
                     System.currentTimeMillis() - start);
    }
}