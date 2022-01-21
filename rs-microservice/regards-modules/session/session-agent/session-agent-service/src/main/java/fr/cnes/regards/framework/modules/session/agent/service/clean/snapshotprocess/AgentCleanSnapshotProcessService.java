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
import fr.cnes.regards.framework.modules.session.agent.dao.IStepPropertyUpdateRequestRepository;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

/**
 * Service to clean old unused {@link fr.cnes.regards.framework.modules.session.commons.domain.SnapshotProcess}
 *
 * @author Iliana Ghazali
 **/
@MultitenantTransactional
public class AgentCleanSnapshotProcessService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AgentCleanSnapshotProcessService.class);

    @Autowired
    private IStepPropertyUpdateRequestRepository stepPropertyRepo;

    @Value("${regards.session.agent.clean.snapshot.process.limit.store:30}")
    private int limitStoreSnapshotProcess;

    public int clean() {
        // Init startClean with the current date minus the limit of SnapshotProcess save configured
        OffsetDateTime startClean = OffsetDateTime.now(ZoneOffset.UTC).minusDays(this.limitStoreSnapshotProcess);
        LOGGER.debug("Check unused snapshot processes before {}", startClean);
        return this.stepPropertyRepo.deleteUnusedProcess(startClean);
    }
}