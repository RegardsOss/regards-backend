/*
 * Copyright 2017-2020 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

/**
 * TODO: ExecutionMonitoringDTO description
 *
 * @author gandrieu
 */
package fr.cnes.regards.modules.processing.domain.dto;

import fr.cnes.regards.modules.processing.domain.PExecution;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
public class ExecutionMonitoringDTO extends PExecution {

    protected String processName;

    public ExecutionMonitoringDTO(PExecution exec, String processName) {
        super(exec.getId(), exec.getExecutionCorrelationId(),
            exec.getBatchId(), exec.getBatchCorrelationId(),
            exec.getExpectedDuration(),
            exec.getInputFiles(),
            exec.getSteps(),
            exec.getTenant(), exec.getUserName(),
            exec.getProcessBusinessId(),
            exec.getCreated(),
            exec.getLastUpdated(),
            exec.getVersion(), exec.isPersisted());
        this.processName = processName;
    }
}
