/*
 * Copyright 2017 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.framework.modules.jobs.domain;

import java.nio.file.Path;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;

import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterInvalidException;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterMissingException;

/**
 * Interface for all regards jobs
 * @author Léo Mieulet
 */
public interface IJob extends Runnable {
    /**
     * URI pointed by JobResult MUST NOT be into workspace (else they will be erased after job execution)
     * @return The Job's results
     */
    default Set<JobResult> getResults() {
        return Collections.emptySet();
    }

    /**
     * @return Does the job has a result ?
     */
    default boolean hasResults() {
        return ((getResults() != null) && !getResults().isEmpty());
    }

    /**
     * If the job needs a workspace, JobService create one for it before executing job and clean it after execution
     * @return does the job need a workspace ?
     */
    default boolean needWorkspace() {
        return false;
    }

    /**
     * If the job needs a workspace, JobService create one for it before executing job and clean it after execution
     * @param pPath set workspace path
     */
    void setWorkspace(Path pPath);

    Path getWorkspace();

    /**
     * @param pJobInfoId save the jobInfo id inside the job
     */
    void setId(final UUID pJobInfoId);

    /**
     * Set the parameters and should check if all needed parameters are specified
     * @param pParameters set job parameters
     */
    void setParameters(Set<JobParameter> pParameters) throws JobParameterMissingException, JobParameterInvalidException;
}
