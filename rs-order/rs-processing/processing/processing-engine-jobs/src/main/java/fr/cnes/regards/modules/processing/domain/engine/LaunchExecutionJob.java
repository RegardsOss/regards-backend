/* Copyright 2017-2021 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.processing.domain.engine;

import fr.cnes.regards.framework.modules.jobs.domain.AbstractJob;
import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterInvalidException;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterMissingException;
import fr.cnes.regards.modules.processing.domain.service.IExecutionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;
import java.util.UUID;

/**
 * This class provides the job launching the actual execution's process' executable.
 *
 * @author gandrieu
 */
public class LaunchExecutionJob extends AbstractJob<Void> {

    private static final Logger STATIC_LOGGER = LoggerFactory.getLogger(LaunchExecutionJob.class);

    public static final String EXEC_ID_PARAM = "execId";

    @Autowired
    private IExecutionService execService;

    private UUID execId;

    @Override
    public void setParameters(Map<String, JobParameter> parameters)
            throws JobParameterMissingException, JobParameterInvalidException {
        execId = getValue(parameters, EXEC_ID_PARAM);
    }

    @Override
    public void run() {
        STATIC_LOGGER.info("exec={} - LaunchExecutionJob start", execId);
        execService.runExecutable(execId)
                .subscribe(exec -> STATIC_LOGGER.info("exec={} - LaunchExecutionJob success", execId),
                           err -> STATIC_LOGGER.error("exec={} - LaunchExecutionJob failure: {}", execId, err.getMessage()));
    }
}
