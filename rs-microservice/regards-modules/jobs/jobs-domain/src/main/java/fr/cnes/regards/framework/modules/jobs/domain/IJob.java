/*
 * Copyright 2017-2018 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterInvalidException;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterMissingException;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobWorkspaceException;
import fr.cnes.regards.framework.modules.jobs.domain.function.CheckedSupplier;

/**
 * Interface for all regards jobs
 * @param <R> result type
 * @author Léo Mieulet
 * @author oroussel
 */
public interface IJob<R> extends Runnable {

    /**
     * Manage job result.
     * <br/>
     * <b>Override this method for the result to be stored in its {@link JobInfo}</b>
     * @return The Job result
     */
    default R getResult() {
        return null;
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
     * @param workspaceSupplier workspace supplier that is also called by the method to set workspace path
     */
    void setWorkspace(CheckedSupplier<Path, IOException> workspaceSupplier) throws JobWorkspaceException;

    Path getWorkspace();

    /**
     * Manage job parameters (if there are any). Job implementation has <b>to check
     * if all needed parameters are specified</b> and <b>store them</b> for use in job execution.
     * <br/>
     * <b>Beware : do nothing by default, this method must be overridden.</b>
     * @param parameters non null parameter map
     */
    default void setParameters(Map<String, JobParameter> parameters) // NOSONAR
            throws JobParameterMissingException, JobParameterInvalidException {
    }

    /**
     * To manage completion estimated date and percentComplete property, a job should provide the number of times it
     * will call {@link #advanceCompletion()} during its execution.
     * @return 100 by default
     */
    default int getCompletionCount() {
        return 100; // NOSONAR
    }

    /**
     * Advance completion count. This method should not be called more than {@link #getCompletionCount()} times
     */
    void advanceCompletion();
}
