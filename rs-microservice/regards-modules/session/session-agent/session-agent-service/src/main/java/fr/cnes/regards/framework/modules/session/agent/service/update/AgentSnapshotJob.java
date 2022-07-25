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
package fr.cnes.regards.framework.modules.session.agent.service.update;

import com.google.gson.reflect.TypeToken;
import fr.cnes.regards.framework.modules.jobs.domain.AbstractJob;
import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterInvalidException;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterMissingException;
import fr.cnes.regards.framework.modules.jobs.service.IJobInfoService;
import fr.cnes.regards.framework.modules.session.commons.domain.SnapshotProcess;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.OffsetDateTime;
import java.util.Map;

/**
 * Job to create {@link fr.cnes.regards.framework.modules.session.commons.domain.SessionStep}s
 *
 * @author Iliana Ghazali
 */
public class AgentSnapshotJob extends AbstractJob<Void> {

    /**
     * Job parameters
     */

    public static final String FREEZE_DATE = "FREEZE_DATE";

    public static final String SNAPSHOT_PROCESS = "SNAPSHOT_PROCESS";

    private SnapshotProcess snapshotProcess;

    private OffsetDateTime freezeDate;

    /**
     * Services
     */

    @Autowired
    protected IJobInfoService jobInfoService;

    @Autowired
    private AgentSnapshotService agentSnapshotService;

    @Override
    public void setParameters(Map<String, JobParameter> parameters)
        throws JobParameterMissingException, JobParameterInvalidException {
        this.snapshotProcess = getValue(parameters, SNAPSHOT_PROCESS, new TypeToken<SnapshotProcess>() {

        }.getType());

        this.freezeDate = getValue(parameters, FREEZE_DATE, new TypeToken<OffsetDateTime>() {

        }.getType());
    }

    @Override
    public void run() {
        String source = snapshotProcess.getSource();
        logger.debug("[{}] AgentSnapshot job starts for source {}", jobInfoId, source);
        long start = System.currentTimeMillis();
        agentSnapshotService.generateSessionStep(snapshotProcess, freezeDate);
        logger.debug("[{}] AgentSnapshot job ends in {} ms for source {}",
                     jobInfoId,
                     System.currentTimeMillis() - start,
                     source);
    }
}
